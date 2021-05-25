import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ClientTCP {

    Socket socket;
    DataInputStream inputStream;
    DataOutputStream outputStream;

    private int clientId;
    private final String serverHost;
    private final int port;

    ClientTCP(int clientId, String serverHost, int port) {
        this.clientId = clientId;
        this.serverHost = serverHost;
        this.port = port;
    }

    public void connect() throws IOException {
        //  Создаем сокет для подальшего использования
        socket = new Socket(serverHost, port);
        inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        outputStream =  new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

        System.out.println("Client [" + clientId + "] connected to " + socket.getRemoteSocketAddress());
    }

    public void closeConnection() throws IOException {
        System.out.println("Client [" + clientId + "] closed connection to " + socket.getRemoteSocketAddress());
        //  Закрываем сокет
        socket.close();
    }

    public JSONObject request(JSONObject Data) throws IOException, InterruptedException, ExecutionException {
        CompletableFuture<JSONObject> result =
                output.call(Data)
                        //  Отрпавка данных через outputStream
                        .thenApply(
                                out -> {
                                    if (out) {
                                        try {
                                            //  Получаем данные из inputStream
                                            return input.call().get();
                                        } catch (InterruptedException | ExecutionException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    return null;
                                });
        return result.get();
    }

    @FunctionalInterface
    interface iJsonInput {
        CompletableFuture<JSONObject> call();
    }

    @FunctionalInterface
    interface iJsonOutput {
        CompletableFuture<Boolean> call(JSONObject Data);
    }

    iJsonInput input = () -> CompletableFuture
            .supplyAsync(
                    () -> {
                        JSONObject responseJson = null;
                        try {
                            int length = inputStream.readInt();
                            byte[] responseData = new byte[length];
                            inputStream.readFully(responseData);
                            responseJson = new JSONObject(new String(responseData, StandardCharsets.UTF_8));

                            System.out.println("Client [" + clientId + "] got response from " + socket.getRemoteSocketAddress() + "\n" + "Response :\n" + responseJson.toString(2));
                        } catch (IOException e) {
                            System.err.println("Client [" + clientId + "] Error : " + e);
                            responseJson = null;
                            e.printStackTrace();
                        }
                        return responseJson;
                    }
            );

    iJsonOutput output = (Data) -> CompletableFuture
            .supplyAsync(
                    () -> {
                        try {
                            byte[] requestData = Data.toString().getBytes(StandardCharsets.UTF_8);
                            System.out.println("Client [" + clientId + "] sent request to " + socket.getRemoteSocketAddress() + "\n" + "Request :\n" + Data.toString(2));

                            outputStream.writeInt(requestData.length);
                            outputStream.write(requestData);
                            outputStream.flush();
                            return true;
                        } catch (IOException e) {
                            System.err.println("Client [" + clientId + "] Error : " + e);
                            e.printStackTrace();
                            return false;
                        }
                    }
            );
}
