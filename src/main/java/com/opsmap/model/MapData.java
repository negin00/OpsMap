package com.opsmap.model;

import java.io.*;
import java.util.*;

public class MapData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private List<MapShape> shapes;
    private String createdBy;
    private long createdAt;
    private long modifiedAt;

    public MapData(String name, List<MapShape> shapes, String createdBy) {
        this.name = name;
        this.shapes = new ArrayList<>(shapes);
        this.createdBy = createdBy;
        this.createdAt = System.currentTimeMillis();
        this.modifiedAt = this.createdAt;
    }

    public String getName() { return name; }
    public List<MapShape> getShapes() { return shapes; }
    public String getCreatedBy() { return createdBy; }
    public long getCreatedAt() { return createdAt; }
    public long getModifiedAt() { return modifiedAt; }

    public void updateModifiedTime() {
        this.modifiedAt = System.currentTimeMillis();
    }

    public boolean saveToFile(String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filename + ".opsmap"))) {
            oos.writeObject(this);
            return true;
        } catch (Exception e) {
            System.out.println("خطا در ذخیره: " + e.getMessage());
            return false;
        }
    }

    public static MapData loadFromFile(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filename))) {
            return (MapData) ois.readObject();
        } catch (Exception e) {
            System.out.println("خطا در بارگذاری: " + e.getMessage());
            return null;
        }
    }
}
