import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

class Server implements Runnable {

    private final int port;
    private final InvertedIndex invIndex;
    private final FileListReader filesReader;

    Server(int port, String rootDataDir) {
        this.port = port;
        this.invIndex = new InvertedIndex();
        this.filesReader = new FileListReader(rootDataDir);
        try {
            invIndex.createIndex(6, filesReader);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void listen(){
        new Thread( this ).start();
        System.out.println("Server is listening on port : " + port);
    }

    public void run () {
        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
            server.setReuseAddress(true);

            while (true) {
                Socket client = server.accept();
                System.out.println("New client connection from : "
                        + client.getRemoteSocketAddress());
                ClientHandler clientSock = new ClientHandler(client, invIndex, filesReader);
                new Thread(clientSock).start();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (server != null) {
                try {
                    server.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final InvertedIndex invIndex;
        private final FileListReader filesReader;

        ClientHandler(Socket socket, InvertedIndex invIndex, FileListReader filesReader) {
            this.clientSocket = socket;
            this.invIndex = invIndex;
            this.filesReader = filesReader;
        }

        public void run() {
            while (true){
                try {
                    processInput();

                } catch (IOException e) {
                    System.out.println("Connection from : " + clientSocket.getRemoteSocketAddress() + " closed");
                    break;
                }
            }
        }

        private void processInput() throws IOException{
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
            DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));

            int length = in.readInt();

            if (length == 0){
                return;
            }

            byte[] requestData = new byte[length];

            in.readFully(requestData);

            JSONObject requestJson = new JSONObject(new String(requestData, StandardCharsets.UTF_8));
            JSONObject requestHeaders = (JSONObject) requestJson.get("headers");
            JSONObject requestBody = (JSONObject) requestJson.get("body");
            String endpoint = (String) requestHeaders.get("endpoint");

            System.out.println("Server got request on endpoint : " + endpoint + "\nfrom : " + clientSocket.getRemoteSocketAddress());

            JSONObject responseJson = new JSONObject();
            JSONObject responseBody = new JSONObject();
            JSONObject responseHeaders = new JSONObject();

            switch (endpoint) {
                case "search" -> {
                    try{
                        String search = (String) requestBody.get("searchQuery");
                        responseHeaders.put("status", 200);
                        responseBody.put("searchQuery", search);
                        List<String> searchList = Arrays.asList(search.split("\\W+"));
                        responseBody.put("documents", invIndex.search(searchList));
                        responseBody.put("info", "You have got all documents related to your search-query");

                    }catch (JSONException e){
                        responseHeaders.put("status", 400);
                        responseBody.put("info", "Bad request! Request body doesn't have needed fields.");
                    }
                }
                case "createIndex" -> {
                    try{
                        Integer threads = (Integer) requestBody.get("threads");

                        long workTime = -1;
                        try {
                            workTime = invIndex.createIndex(threads, filesReader);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (workTime == -1){
                            responseHeaders.put("status", 500);
                            responseBody.put("info", "Some server error.");
                            break;
                        }
                        responseHeaders.put("status", 200);
                        responseBody.put("threads", threads);
                        responseBody.put("time", workTime);
                        responseBody.put("type", "nanoseconds");
                        responseBody.put("info", "Inverted index is created.");

                    }catch (JSONException e){
                        responseHeaders.put("status", 400);
                        responseBody.put("info", "Bad request! Request body doesn't have needed fields.");
                    }
                }
                default -> {
                    responseHeaders.put("status", 404);
                    responseBody.put("info", "No such endpoint :(");
                }
            }

            responseJson.put("headers", responseHeaders);
            responseJson.put("body", responseBody);

            byte[] responseData = responseJson.toString().getBytes(StandardCharsets.UTF_8);

            out.writeInt(responseData.length);
            out.write(responseData);
            out.flush();

            System.out.println( "Server processed request from : " + clientSocket.getRemoteSocketAddress());
        }

    }

    static public void main(String[] args) throws Exception {
        int port = Integer.parseInt( args[0] );
        // Запускаем сервер на порту port
        Server server = new Server( port , "src/data");
        server.listen();
    }
}