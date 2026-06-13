package com.opsmap.model;

public class TextShape extends MapShape {
    private static final long serialVersionUID = 1L;

    private double x, y;
    private String text;

    public TextShape(String owner, String color, double strokeWidth,
                     double x, double y, String text) {
        super(owner, color, strokeWidth);
        this.x = x;
        this.y = y;
        this.text = text;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public String getText() { return text; }

    @Override
    public String toNetworkFormat() {
        return String.format("TEXT:%s|%s|%s|%.1f|%.1f|%.1f|%s",
                id, owner, color, strokeWidth, x, y, text.replace("|", "~"));
    }

    public static TextShape parseFromNetwork(String data) {
        String[] p = data.split("\\|");
        if (p.length < 7) return null;
        TextShape shape = new TextShape(p[1], p[2],
                Double.parseDouble(p[3]),
                Double.parseDouble(p[4]),
                Double.parseDouble(p[5]),
                p[6].replace("~", "|"));
        shape.id = p[0];
        return shape;
    }
}
