package com.opsmap.model;

import java.io.Serializable;

public abstract class MapShape implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String id;
    protected String owner;
    protected String color;
    protected double strokeWidth;
    protected long timestamp;

    public MapShape(String owner, String color, double strokeWidth) {
        this.id = owner + "_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        this.owner = owner;
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwner() { return owner; }
    public String getColor() { return color; }
    public double getStrokeWidth() { return strokeWidth; }
    public long getTimestamp() { return timestamp; }

    public abstract String toNetworkFormat();

    public static MapShape fromNetworkFormat(String data) {
        if (data == null || data.isEmpty()) return null;

        try {
            if (data.startsWith("LINE:")) {
                return LineShape.parseFromNetwork(data.substring(5));
            } else if (data.startsWith("RECT:")) {
                return RectangleShape.parseFromNetwork(data.substring(5));
            } else if (data.startsWith("TEXT:")) {
                return TextShape.parseFromNetwork(data.substring(5));
            } else if (data.startsWith("CIRCLE:")) {
                return CircleShape.parseFromNetwork(data.substring(7));
            }
        } catch (Exception e) {
            System.out.println("خطا در پارس شکل: " + e.getMessage());
        }
        return null;
    }
}
