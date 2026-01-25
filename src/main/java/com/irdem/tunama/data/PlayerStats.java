package com.irdem.tunama.data;

public class PlayerStats {
    private int health;
    private int strength;
    private int agility;
    private int intelligence;
    private int sacredPower;
    private int corruptPower;
    private int naturePower;

    public PlayerStats() {
        this.health = 10;
        this.strength = 1;
        this.agility = 1;
        this.intelligence = 1;
        this.sacredPower = 1;
        this.corruptPower = 1;
        this.naturePower = 1;
    }

    // Getters y Setters
    public int getHealth() { return health; }
    public int getStrength() { return strength; }
    public int getAgility() { return agility; }
    public int getIntelligence() { return intelligence; }
    public int getSacredPower() { return sacredPower; }
    public int getCorruptPower() { return corruptPower; }
    public int getNaturePower() { return naturePower; }
    
    public void setHealth(int value) { this.health = value; }
    public void setStrength(int value) { this.strength = value; }
    public void setAgility(int value) { this.agility = value; }
    public void setIntelligence(int value) { this.intelligence = value; }
    public void setSacredPower(int value) { this.sacredPower = value; }
    public void setCorruptPower(int value) { this.corruptPower = value; }
    public void setNaturePower(int value) { this.naturePower = value; }
    
    public void addHealth(int amount) { this.health += amount; }
    public void addStrength(int amount) { this.strength += amount; }
    public void addAgility(int amount) { this.agility += amount; }
    public void addIntelligence(int amount) { this.intelligence += amount; }
    public void addSacredPower(int amount) { this.sacredPower += amount; }
    public void addCorruptPower(int amount) { this.corruptPower += amount; }
    public void addNaturePower(int amount) { this.naturePower += amount; }
}
