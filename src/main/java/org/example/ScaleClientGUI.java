package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;

public class ScaleClientGUI extends JFrame {

    private JTextField ipField;
    private JTextField portField;
    private JTextArea outputArea;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private JButton connectButton;
    private JButton sendGrossWeightButton;
    private JButton sendNetWeightButton;

    // Enum zur Definition der Gewichtstypen und zugehöriger Informationen
    private enum WeightType {
        GROSS("wei.groa", "GRO:+", "Bruttogewicht"),
        NET("wei.net", "NET:+", "Nettogewicht");

        private final String command;
        private final String prefix;
        private final String displayName;

        WeightType(String command, String prefix, String displayName) {
            this.command = command;
            this.prefix = prefix;
            this.displayName = displayName;
        }

        public String getCommand() {
            return command;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public ScaleClientGUI() {
        setTitle("Waage Client");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Fenster zentrieren
        initializeGUI();
    }

    private void initializeGUI() {
        setLayout(new BorderLayout());

        // Oberes Panel für IP und Port
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // Mittleres Panel für Ausgabe
        JScrollPane scrollPane = createOutputArea();
        add(scrollPane, BorderLayout.CENTER);

        // Unteres Panel für Buttons
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        ipField = new JTextField("127.0.0.1");
        portField = new JTextField("5000");

        topPanel.add(new JLabel("IP-Adresse:"));
        topPanel.add(ipField);
        topPanel.add(new JLabel("Port:"));
        topPanel.add(portField);

        return topPanel;
    }

    private JScrollPane createOutputArea() {
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Ausgabe"));
        return scrollPane;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        connectButton = new JButton("Verbinden");
        sendGrossWeightButton = new JButton("Bruttogewicht abfragen");
        sendNetWeightButton = new JButton("Nettogewicht abfragen");

        // Buttons initial deaktivieren
        sendGrossWeightButton.setEnabled(false);
        sendNetWeightButton.setEnabled(false);

        connectButton.addActionListener(this::handleConnectButton);
        sendGrossWeightButton.addActionListener(e -> sendCommand(WeightType.GROSS));
        sendNetWeightButton.addActionListener(e -> sendCommand(WeightType.NET));

        buttonPanel.add(connectButton);
        buttonPanel.add(sendGrossWeightButton);
        buttonPanel.add(sendNetWeightButton);

        return buttonPanel;
    }

    private void handleConnectButton(ActionEvent e) {
        connectToScale();
    }

    private void connectToScale() {
        String ip = ipField.getText();
        int port;
        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException ex) {
            outputArea.append("Ungültiger Port.\n");
            return;
        }

        connectButton.setEnabled(false);
        outputArea.append("Verbinde zu " + ip + ":" + port + "...\n");

        new Thread(() -> {
            try {
                socket = new Socket(ip, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                SwingUtilities.invokeLater(() -> {
                    outputArea.append("Verbindung hergestellt mit: " + ip + ":" + port + "\n");
                    updateConnectionStatus(true);
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    outputArea.append("Fehler bei der Verbindung: " + e.getMessage() + "\n");
                    connectButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void sendCommand(WeightType weightType) {
        if (socket != null && socket.isConnected() && !socket.isClosed()) {
            new Thread(() -> {
                try {
                    String command = weightType.getCommand();
                    out.println(command);
                    SwingUtilities.invokeLater(() -> outputArea.append("Befehl gesendet: " + command + "\n"));

                    String response = in.readLine();
                    SwingUtilities.invokeLater(() -> {
                        outputArea.append("Empfangene Antwort: " + response + "\n");
                        String weight = extractWeight(response, weightType);
                        outputArea.append(weightType.getDisplayName() + ": " + weight + " kg\n");
                    });
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> outputArea.append("Fehler beim Senden des Befehls: " + e.getMessage() + "\n"));
                    closeConnection();
                }
            }).start();
        } else {
            outputArea.append("Nicht verbunden. Bitte zuerst verbinden.\n");
        }
    }

    private String extractWeight(String response, WeightType weightType) {
        String prefix = weightType.getPrefix();
        int startIndex = response.indexOf(prefix);
        if (startIndex != -1) {
            startIndex += prefix.length();
            int endIndex = response.indexOf(";", startIndex);
            if (endIndex != -1) {
                return response.substring(startIndex, endIndex).trim();
            }
        }
        return "Unbekannt";
    }

    private void closeConnection() {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            outputArea.append("Fehler beim Schließen der Verbindung: " + e.getMessage() + "\n");
        } finally {
            SwingUtilities.invokeLater(() -> updateConnectionStatus(false));
        }
    }

    private void updateConnectionStatus(boolean connected) {
        sendGrossWeightButton.setEnabled(connected);
        sendNetWeightButton.setEnabled(connected);
        ipField.setEditable(!connected);
        portField.setEditable(!connected);
        connectButton.setEnabled(!connected);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ScaleClientGUI gui = new ScaleClientGUI();
            gui.setVisible(true);
        });
    }
}
