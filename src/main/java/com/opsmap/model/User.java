package com.opsmap.model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Role {
        COMMANDER("فرمانده", "#FF4444", true, true, true),
        OPERATOR("اپراتور", "#44FF44", true, true, false),
        OBSERVER("ناظر", "#4444FF", false, false, false);

        private final String persianName;
        private final String defaultColor;
        private final boolean canDraw;
        private final boolean canDeleteOwn;
        private final boolean canDeleteAll;

        Role(String persianName, String defaultColor, boolean canDraw,
             boolean canDeleteOwn, boolean canDeleteAll) {
            this.persianName = persianName;
            this.defaultColor = defaultColor;
            this.canDraw = canDraw;
            this.canDeleteOwn = canDeleteOwn;
            this.canDeleteAll = canDeleteAll;
        }

        public String getPersianName() { return persianName; }
        public String getDefaultColor() { return defaultColor; }
        public boolean canDraw() { return canDraw; }
        public boolean canDeleteOwn() { return canDeleteOwn; }
        public boolean canDeleteAll() { return canDeleteAll; }
    }

    private String username;
    private String password;
    private Role role;
    private boolean online;

    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.online = false;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public boolean isOnline() { return online; }

    public void setOnline(boolean online) { this.online = online; }
    public void setPassword(String password) { this.password = password; }

    public boolean canDraw() { return role.canDraw(); }
    public boolean canDeleteOwn() { return role.canDeleteOwn(); }
    public boolean canDeleteAll() { return role.canDeleteAll(); }

    public String getRolePersian() { return role.getPersianName(); }
    public String getDefaultColor() { return role.getDefaultColor(); }

    public String toNetworkFormat() {
        return username + ":" + role.name();
    }

    public static User fromNetworkFormat(String data, String password) {
        String[] parts = data.split(":");
        if (parts.length < 2) return null;
        return new User(parts[0], password, Role.valueOf(parts[1]));
    }

    @Override
    public String toString() {
        return username + " (" + getRolePersian() + ")";
    }
}
