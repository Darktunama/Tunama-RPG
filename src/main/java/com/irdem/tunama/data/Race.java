package com.irdem.tunama.data;

public class Race {
    private String id;
    private String name;
    private String description;
    private String advantages;
    private String disadvantages;
    private double agilityMultiplier;
    private double intelligenceMultiplier;
    private double lifeMultiplier;
    private double strengthMultiplier;
    private double corruptPowerCost;
    private double sagradoPowerCost;
    private double naturePowerCost;

    public Race(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.agilityMultiplier = 1.0;
        this.intelligenceMultiplier = 1.0;
        this.lifeMultiplier = 1.0;
        this.strengthMultiplier = 1.0;
        this.corruptPowerCost = 1.0;
        this.sagradoPowerCost = 1.0;
        this.naturePowerCost = 1.0;
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAdvantages() { return advantages; }
    public String getDisadvantages() { return disadvantages; }
    
    public double getAgilityMultiplier() { return agilityMultiplier; }
    public double getIntelligenceMultiplier() { return intelligenceMultiplier; }
    public double getLifeMultiplier() { return lifeMultiplier; }
    public double getStrengthMultiplier() { return strengthMultiplier; }
    public double getCorruptPowerCost() { return corruptPowerCost; }
    public double getSagradoPowerCost() { return sagradoPowerCost; }
    public double getNaturePowerCost() { return naturePowerCost; }
    
    public void setAdvantages(String advantages) { this.advantages = advantages; }
    public void setDisadvantages(String disadvantages) { this.disadvantages = disadvantages; }
    public void setAgilityMultiplier(double mult) { this.agilityMultiplier = mult; }
    public void setIntelligenceMultiplier(double mult) { this.intelligenceMultiplier = mult; }
    public void setLifeMultiplier(double mult) { this.lifeMultiplier = mult; }
    public void setStrengthMultiplier(double mult) { this.strengthMultiplier = mult; }
    public void setCorruptPowerCost(double cost) { this.corruptPowerCost = cost; }
    public void setSagradoPowerCost(double cost) { this.sagradoPowerCost = cost; }
    public void setNaturePowerCost(double cost) { this.naturePowerCost = cost; }
}
