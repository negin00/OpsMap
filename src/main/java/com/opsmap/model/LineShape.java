package com.opsmap.model;

public class LineShape extends MapShape {
    private static final long serialVersionUID = 1L;

    private double startX, startY, endX, endY;

    public LineShape(String owner, String color, double strokeWidth,
                     double startX, double startY, double endX, double endY) {
        super(owner, color, strokeWidth);
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getEndX() { return endX; }
    public double getEndY() { return endY; }

    @Override
    public String toNetworkFormat() {
        return String.format("LINE:%s|%s|%s|%.1f|%.1f|%.1f|%.1f|%.1f",
                id, owner, color, strokeWidth, startX, startY, endX, endY);
    }

    public static LineShape parseFromNetwork(String data) {
        String[] p = data.split("\\|");
        if (p.length < 8) return null;
        LineShape shape = new LineShape(p[1], p[2],
                Double.parseDouble(p[3]),
                Double.parseDouble(p[4]),
                Double.parseDouble(p[5]),
                Double.parseDouble(p[6]),
                Double.parseDouble(p[7]));
        shape.id = p[0];
        return shape;
    }
}
