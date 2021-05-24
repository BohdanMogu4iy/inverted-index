import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ClientTCP {

    Socket socket;
    InputStream inputStream;
    OutputStream outputStream;

    int clientId;
    String serverName;
    int port;

    ClientTCP(int clientId, String serverName, int port) {
        this.clientId = clientId;
        this.serverName = serverName;
        this.port = port;
    }

    public void connect() throws IOException {
        //  Создаем сокет для подальшего использования
        socket = new Socket(serverName, port);
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
        System.out.println("Client [" + clientId + "] connected to " + socket.getRemoteSocketAddress());
    }

    public void closeConnection() throws IOException {
        //  Закрываем сокет
        socket.close();
        System.out.println("Client [" + clientId + "] closed connection to " + socket.getRemoteSocketAddress());
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
    interface jsonInput {
        CompletableFuture<JSONObject> call();
    }

    @FunctionalInterface
    interface jsonOutput {
        CompletableFuture<Boolean> call(JSONObject Data);
    }

    jsonInput input = () -> CompletableFuture
            .supplyAsync(
                    () -> {
                        JSONObject result = null;
                        float count = -1;
                        while (count == -1) {
                            try {
                                byte[] data = new byte[1000];
                                // Прием данных из InputStream
                                count = inputStream.read(data);
                                if (count == -1) {
                                    // Если данные пустые мы пробуем еще раз
                                    continue;
                                }
                                // Записываем данные в JSONObject
                                result = new JSONObject(new String(data, StandardCharsets.UTF_8));
                                System.out.println("Client [" + clientId + "] got response from " + socket.getRemoteSocketAddress() + "\n" + "Response :\n" + result.toString(2));
                            } catch (IOException e) {
                                System.err.println("Client [" + clientId + "] Error : " + e);
                                result = null;
                            }
                        }
                        // Возвращаем response
                        return result;
                    }
            );

    jsonOutput output = (Data) -> CompletableFuture
            .supplyAsync(
                    () -> {
                        try {
                            // Считываем данные из inputStream
                            System.out.println("Client [" + clientId + "] sent request to " + socket.getRemoteSocketAddress() + "\n" + "Request :\n" + Data.toString(2));
                            outputStream.write(Data.toString().getBytes(StandardCharsets.UTF_8));
                            return true;
                        } catch (IOException e) {
                            System.err.println("Client [" + clientId + "] Error : " + e);
                            return false;
                        }
                    }
            );
}
