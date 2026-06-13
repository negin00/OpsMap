package com.opsmap.model;

import java.io.*;
import java.util.*;

public class UserManager {

    private static final String USERS_FILE = "users.dat";
    private Map<String, User> users = new HashMap<>();
    private static UserManager instance;

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    private UserManager() {
        loadUsers();
        createDefaultUsers();
    }

    private void createDefaultUsers() {
        if (!users.containsKey("admin")) {
            users.put("admin", new User("admin", "admin123", User.Role.COMMANDER));
        }
        if (!users.containsKey("operator1")) {
            users.put("operator1", new User("operator1", "1234", User.Role.OPERATOR));
        }
        if (!users.containsKey("observer1")) {
            users.put("observer1", new User("observer1", "1234", User.Role.OBSERVER));
        }
        saveUsers();
    }

    public boolean register(String username, String password, User.Role role) {
        if (username == null || username.trim().length() < 3) {
            return false;
        }
        if (password == null || password.length() < 4) {
            return false;
        }
        if (users.containsKey(username.toLowerCase().trim())) {
            return false;
        }

        User newUser = new User(username.trim(), password, role);
        users.put(username.toLowerCase().trim(), newUser);
        saveUsers();
        return true;
    }

    public User login(String username, String password) {
        if (username == null || password == null) return null;

        User user = users.get(username.toLowerCase().trim());
        if (user != null && user.getPassword().equals(password)) {
            user.setOnline(true);
            return user;
        }
        return null;
    }

    public void logout(String username) {
        User user = users.get(username.toLowerCase().trim());
        if (user != null) {
            user.setOnline(false);
        }
    }

    public boolean userExists(String username) {
        return users.containsKey(username.toLowerCase().trim());
    }

    public User getUser(String username) {
        return users.get(username.toLowerCase().trim());
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public List<User> getOnlineUsers() {
        List<User> online = new ArrayList<>();
        for (User user : users.values()) {
            if (user.isOnline()) {
                online.add(user);
            }
        }
        return online;
    }

    @SuppressWarnings("unchecked")
    private void loadUsers() {
        File file = new File(USERS_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                users = (Map<String, User>) ois.readObject();
            } catch (Exception e) {
                System.out.println("خطا در بارگذاری کاربران: " + e.getMessage());
                users = new HashMap<>();
            }
        }
    }

    private void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            oos.writeObject(users);
        } catch (Exception e) {
            System.out.println("خطا در ذخیره کاربران: " + e.getMessage());
        }
    }

    public boolean changePassword(String username, String oldPass, String newPass) {
        User user = users.get(username.toLowerCase().trim());
        if (user != null && user.getPassword().equals(oldPass)) {
            user.setPassword(newPass);
            saveUsers();
            return true;
        }
        return false;
    }
}
