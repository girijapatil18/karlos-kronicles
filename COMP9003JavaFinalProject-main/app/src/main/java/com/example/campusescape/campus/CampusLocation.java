package com.example.campusescape.campus;

import android.graphics.Rect;

public class CampusLocation {

    private String name;

    private Rect area;

    private int levelNumber;

    private boolean unlocked;
    private boolean completed;

    public CampusLocation(String name,
                          Rect area,
                          int levelNumber,
                          boolean unlocked) {

        this.name = name;
        this.area = area;
        this.levelNumber = levelNumber;
        this.unlocked = unlocked;
    }

    // ================= GETTERS =================

    public String getName() {
        return name;
    }

    public Rect getArea() {
        return area;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public boolean isCompleted() {
        return completed;
    }

    // ================= SETTERS =================

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}