package izzulhaq.webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class WebServer {
    public static void main(String[] args) {
        int queue_length = 6;
        int port = 2540;
        Socket socket;

        try {
            ServerSocket serverSocket = new ServerSocket(port, queue_length);

            while (true) {
                socket = serverSocket.accept();
                System.out.println("Memulai web server, mendengarkan pada port " + port + ".");
                System.out.println("htp://localhost:" + port + " dapat diakses sekarang");
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String request = "";
                String clientRequest = "";

                while ((clientRequest = br.readLine()) != null) {
                    if (request.equals("")) {
                        request = clientRequest;
                    }
                    if (clientRequest.equals("")) {
                        break;
                    }
                }

                if (request != null && !request.equals("")) {
                    new HttpWorker(request, socket).start();
                }
            }
        }
        catch (IOException e) {
            System.out.println(e);
        }
        finally {
            System.out.println("Server ditutup.");
        }
    }
}
