package com.taspia.taspiasb;

import org.bukkit.Material;

import org.bukkit.Material;

public class Reward {
    private String name;
    private Material material;
    private String command;
    private int level;
    private String identifier;

    public Reward(int level, String identifier, String name, Material material, String command) {
        this.level = level;
        this.identifier = identifier;
        this.name = name;
        this.material = material;
        this.command = command;
    }

    public int getLevel() {
        return level;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public Material getMaterial() {
        return material;
    }

    public String getCommand() {
        return command;
    }
}

