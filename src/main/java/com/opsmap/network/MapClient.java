package com.opsmap.network;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class MapClient {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Consumer<String> messageHandler;
    private boolean connected = false;
    private Thread listenerThread;
    private String username;
    private String role;

    public boolean connect(String host, int port) {
        // تلاش برای اتصال به چند پورت
        for (int p = port; p < port + 10; p++) {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, p), 2000);
                socket.setTcpNoDelay(true);

                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                connected = true;

                startListening();
                System.out.println("✅ متصل شد به " + host + ":" + p);
                return true;

            } catch (IOException e) {
                System.out.println("⚠️ پورت " + p + " در دسترس نیست");
            }
        }
        System.out.println("❌ اتصال ناموفق");
        return false;
    }

    public void setOnMessageReceived(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                String message;
                while (connected && (message = in.readLine()) != null) {
                    System.out.println("📩 دریافت: " + message);
                    if (messageHandler != null) {
                        final String msg = message;
                        messageHandler.accept(msg);
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    System.out.println("🔴 ارتباط قطع شد");
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void sendMessage(String message) {
        if (connected && out != null) {
            out.println(message);
            System.out.println("📤 ارسال: " + message);
        }
    }

    public void joinWithUser(String username, String role) {
        this.username = username;
        this.role = role;
        sendMessage("JOIN:" + username + ":" + role);
    }

    public void sendChat(String message) {
        sendMessage("CHAT:" + message);
    }

    public void deleteShape(String shapeId) {
        sendMessage("DELETE_SHAPE:" + shapeId);
    }

    public void clearAll() {
        sendMessage("CLEAR_ALL");
    }

    public void deleteMyShapes() {
        sendMessage("DELETE_MY_SHAPES");
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }
}
