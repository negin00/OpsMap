package com.opsmap.network;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private Socket socket;
    private MapServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String role;
    private boolean running = true;

    public ClientHandler(Socket socket, MapServer server) {
        this.socket = socket;
        this.server = server;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                System.out.println("📨 دریافت از [" + (username != null ? username : "ناشناس") + "]: " + message);
                processMessage(message);
            }
        } catch (IOException e) {
            System.out.println("🔴 قطع اتصال: " + (username != null ? username : "ناشناس"));
        } finally {
            disconnect();
        }
    }

    private void processMessage(String message) {
        // ===== ورود کاربر =====
        if (message.startsWith("JOIN:")) {
            String[] parts = message.substring(5).split(":");
            this.username = parts[0];
            this.role = parts.length > 1 ? parts[1] : "OPERATOR";

            // 1. خوش‌آمدگویی
            sendMessage("WELCOME:" + username);

            // 2. ارسال تاریخچه شکل‌ها (با پیام شروع و پایان)
            sendMessage("HISTORY_START");
            server.sendAllShapesToClient(this);
            sendMessage("HISTORY_END");

            // 3. اعلام ورود به همه (شامل خود کاربر)
            server.broadcast("USER_JOINED:" + username + ":" + role, null);

            // 4. ارسال لیست کاربران به **همه** کلاینت‌ها
            server.broadcast(server.getOnlineUsersList(), null);

            System.out.println("✅ کاربر وارد شد: " + username + " (" + role + ")");


            // ===== چت =====
        } else if (message.startsWith("CHAT:")) {
            String chatMsg = message.substring(5);
            server.broadcast("CHAT:" + username + ":" + chatMsg, this);
        }

        // ===== موقعیت موس =====
        else if (message.startsWith("CURSOR:")) {
            server.broadcast(message, this);
        }

        // ===== حذف یک شکل (مهم!) =====
        else if (message.startsWith("DELETE_SHAPE:")) {
            System.out.println("🗑️ درخواست حذف شکل: " + message);
            server.broadcast(message, this);  // به همه بفرست (به جز فرستنده)
            System.out.println("✅ پیام حذف broadcast شد به همه کلاینت‌ها");
        }

        // ===== حذف شکل‌های یک کاربر =====
        else if (message.startsWith("DELETE_USER_SHAPES:") || message.equals("DELETE_MY_SHAPES")) {
            server.broadcast("DELETE_USER_SHAPES:" + username, this);
        }

        // ===== پاک کردن همه (فقط فرمانده) =====
        else if (message.equals("CLEAR_ALL")) {
            if ("COMMANDER".equals(role)) {
                server.clearAllShapes();
                server.broadcast("CLEAR_ALL", null);
                System.out.println("🗑️ نقشه پاک شد توسط: " + username);
            } else {
                sendMessage("ERROR:شما دسترسی پاک کردن نقشه را ندارید");
            }
        }

        // ===== شکل جدید =====
        else if (message.startsWith("LINE:") || message.startsWith("RECT:") ||
                message.startsWith("CIRCLE:") || message.startsWith("TEXT:") ||
                message.startsWith("MARKER:") || message.startsWith("PATH:")) {
            System.out.println("🎨 شکل جدید دریافت شد: " + message.substring(0, Math.min(50, message.length())) + "...");
            server.addShape(message);
            server.broadcast(message, this);
        }

        // ===== PING/PONG =====
        else if (message.equals("PING")) {
            sendMessage("PONG");
        }

        // ===== قطع ارتباط =====
        else if (message.equals("DISCONNECT")) {
            running = false;
        }

        // ===== پیام ناشناخته =====
        else {
            System.out.println("⚠️ پیام ناشناخته: " + message);
        }
    }

    public void sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
        }
    }

    public void disconnect() {
        running = false;
        try {
            if (username != null) {
                server.broadcast("USER_LEFT:" + username, null);
                System.out.println("👋 کاربر خارج شد: " + username);
            }
            server.removeClient(this);
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}
