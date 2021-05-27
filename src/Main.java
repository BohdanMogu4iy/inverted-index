import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class Main {
    static final String ROOT_DATA_DIR = "src/data";

    static public void main(String[] args) throws InterruptedException, IOException {
        String serverName = args[0];
        int port = Integer.parseInt(args[1]);

        HashMap<ClientTCP, JSONObject> clientMap = new HashMap<>();
        clientMap.put(new ClientTCP(0, serverName, port), createRequest("{endpoint: search}", "{searchQuery : personalities reasonably}"));
        clientMap.put(new ClientTCP(1, serverName, port), createRequest("{endpoint: search}", "{searchQuery : apple}"));
        clientMap.put(new ClientTCP(2, serverName, port), createRequest("{endpoint: search}", "{searchQuery : little human}"));
        Server server = new Server(port, ROOT_DATA_DIR);
        server.listen();

        Thread.sleep(10000);

        for (ClientTCP client : clientMap.keySet()){
            try {
                // Подключаем наших клиентов к серверу
                client.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        clientMap.keySet().stream().parallel().forEach(client -> {
            try {
                client.request(clientMap.get(client));
            } catch (IOException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        for (ClientTCP client : clientMap.keySet()){
            try {
                // Подключаем наших клиентов к серверу
                client.closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static JSONObject createRequest(String headers, String data){
        JSONObject requestHeaders = new JSONObject(headers);
        JSONObject requestBody = new JSONObject(data);
        JSONObject request = new JSONObject();
        request.put("headers", requestHeaders);
        request.put("body", requestBody);
        return request;
    }
}
