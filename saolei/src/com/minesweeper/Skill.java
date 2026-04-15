package com.minesweeper;

import java.awt.*;

public class Skill {
    private String name;
    private String description;
    private boolean used;
    private Color color;

    public Skill(String name, String description, Color color) {
        this.name = name;
        this.description = description;
        this.used = false;
        this.color = color;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
    public Color getColor() { return color; }
}