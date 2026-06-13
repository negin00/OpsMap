package com.opsmap.network;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MapServer {

    private static final int DEFAULT_PORT = 5555;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private List<String> shapeHistory = new CopyOnWriteArrayList<>();
    private boolean running = true;
    private int port;

    public void start() {
        port = DEFAULT_PORT;

        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                serverSocket = new ServerSocket(port);
                printServerBanner();

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        clientSocket.setTcpNoDelay(true);

                        System.out.println("🔵 اتصال جدید: " + clientSocket.getInetAddress());

                        ClientHandler handler = new ClientHandler(clientSocket, this);
                        clients.add(handler);
                        new Thread(handler).start();

                    } catch (IOException e) {
                        if (running) {
                            System.out.println("⚠️ خطا: " + e.getMessage());
                        }
                    }
                }
                return;

            } catch (IOException e) {
                System.out.println("⚠️ پورت " + port + " اشغال است، تلاش با پورت " + (port + 1));
                port++;
            }
        }
        System.out.println("❌ پورت آزاد پیدا نشد!");
    }

    private void printServerBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     🗺️  OpsMap Server v2.0              ║");
        System.out.println("║     ✅ سرور با موفقیت راه‌اندازی شد     ║");
        System.out.println("║     📍 پورت: " + port + "                         ║");
        System.out.println("║     🕐 " + new Date() + "     ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();
    }

    public synchronized void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public void addShape(String shapeData) {
        shapeHistory.add(shapeData);
    }

    public void clearAllShapes() {
        shapeHistory.clear();
    }

    public void sendAllShapesToClient(ClientHandler client) {
        for (String shape : shapeHistory) {
            client.sendMessage(shape);
        }
    }

    public String getOnlineUsersList() {
        StringBuilder sb = new StringBuilder("USERLIST:");
        for (ClientHandler client : clients) {
            if (client.getUsername() != null) {
                sb.append(client.getUsername())
                        .append(":")
                        .append(client.getRole())
                        .append(",");
            }
        }
        return sb.toString();
    }

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("📊 تعداد کاربران آنلاین: " + clients.size());
        broadcast(getOnlineUsersList(), null);
    }

    public int getClientCount() {
        return clients.size();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            for (ClientHandler client : clients) {
                client.disconnect();
            }
            clients.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new MapServer().start();
    }
}
