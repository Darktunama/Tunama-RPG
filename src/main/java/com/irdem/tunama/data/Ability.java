package com.irdem.tunama.data;

public class Ability {
    private String id;
    private String name;
    private String description;
    private String rpgClass;
    private int requiredLevel;
    private String manaCost;

    public Ability(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rpgClass = "General";
        this.requiredLevel = 1;
        this.manaCost = "0";
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getRpgClass() { return rpgClass; }
    public int getRequiredLevel() { return requiredLevel; }
    public String getManaCost() { return manaCost; }

    public void setRpgClass(String rpgClass) { this.rpgClass = rpgClass; }
    public void setRequiredLevel(int level) { this.requiredLevel = level; }
    public void setManaCost(String cost) { this.manaCost = cost; }
}
