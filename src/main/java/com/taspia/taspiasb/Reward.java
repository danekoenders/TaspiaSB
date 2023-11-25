package com.taspia.taspiasb;

import org.bukkit.Material;

import org.bukkit.Material;

public class Reward {
    private String name;
    private Material material;
    private String command;

    public Reward(String name, Material material, String command) {
        this.name = name;
        this.material = material;
        this.command = command;
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

