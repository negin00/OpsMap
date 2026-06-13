package com.opsmap.model;

public class CircleShape extends MapShape {
    private static final long serialVersionUID = 1L;

    private double centerX, centerY, radius;

    public CircleShape(String owner, String color, double strokeWidth,
                       double centerX, double centerY, double radius) {
        super(owner, color, strokeWidth);
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
    }

    public double getCenterX() { return centerX; }
    public double getCenterY() { return centerY; }
    public double getRadius() { return radius; }

    @Override
    public String toNetworkFormat() {
        return String.format("CIRCLE:%s|%s|%s|%.1f|%.1f|%.1f|%.1f",
                id, owner, color, strokeWidth, centerX, centerY, radius);
    }

    public static CircleShape parseFromNetwork(String data) {
        String[] p = data.split("\\|");
        if (p.length < 7) return null;
        CircleShape shape = new CircleShape(p[1], p[2],
                Double.parseDouble(p[3]),
                Double.parseDouble(p[4]),
                Double.parseDouble(p[5]),
                Double.parseDouble(p[6]));
        shape.id = p[0];
        return shape;
    }
}
