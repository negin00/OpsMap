package com.opsmap.model;

public class RectangleShape extends MapShape {
    private static final long serialVersionUID = 1L;

    private double x, y, width, height;

    public RectangleShape(String owner, String color, double strokeWidth,
                          double x, double y, double width, double height) {
        super(owner, color, strokeWidth);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }

    @Override
    public String toNetworkFormat() {
        return String.format("RECT:%s|%s|%s|%.1f|%.1f|%.1f|%.1f|%.1f",
                id, owner, color, strokeWidth, x, y, width, height);
    }

    public static RectangleShape parseFromNetwork(String data) {
        String[] p = data.split("\\|");
        if (p.length < 8) return null;
        RectangleShape shape = new RectangleShape(p[1], p[2],
                Double.parseDouble(p[3]),
                Double.parseDouble(p[4]),
                Double.parseDouble(p[5]),
                Double.parseDouble(p[6]),
                Double.parseDouble(p[7]));
        shape.id = p[0];
        return shape;
    }
}
