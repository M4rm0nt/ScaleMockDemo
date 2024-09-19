package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MockScaleServer {

    public static void main(String[] args) {
        int port = 5000;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Mock Waage-Server gestartet. Warte auf Verbindungen...\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Neuer Client verbunden: " + clientSocket.getInetAddress() + "\n");
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Starten des Servers: " + e.getMessage() + "\n");
        }
    }
}

class ClientHandler implements Runnable {

    private Socket clientSocket;

    ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
                Socket socket = this.clientSocket;
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        ) {
            String command;
            while ((command = in.readLine()) != null) {
                System.out.println("Empfangener Befehl von " + socket.getInetAddress() + ": " + command + "\n");

                if (command.trim().equals("wei.groa")) {
                    String response = "wei.groaA:GRO:+50.0;<CR><LF>"; // Beispiel Bruttogewicht von 50 kg
                    out.println(response);
                    System.out.println("Gesendete Antwort an " + socket.getInetAddress() + ": " + response + "\n");
                } else if (command.trim().equals("wei.net")) {
                    String response = "wei.netA:NET:+45.0;<CR><LF>"; // Beispiel Nettogewicht von 45 kg
                    out.println(response);
                    System.out.println("Gesendete Antwort an " + socket.getInetAddress() + ": " + response + "\n");
                } else {
                    out.println("Unbekannter Befehl<CR><LF>");
                    System.out.println("Unbekannter Befehl von " + socket.getInetAddress() + " empfangen." + "\n");
                }
            }
            System.out.println("Verbindung zu " + socket.getInetAddress() + " geschlossen." + "\n");
        } catch (IOException e) {
            System.err.println("Fehler bei der Client-Kommunikation mit " + clientSocket.getInetAddress() + ": " + e.getMessage() + "\n");
        }
    }
}
