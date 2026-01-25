package com.irdem.tunama.data;

public class Mission {
    private String id;
    private String name;
    private String description;
    private int requiredLevel;
    private int rewardExp;
    private String rewardItem;

    public Mission(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requiredLevel = 1;
        this.rewardExp = 0;
        this.rewardItem = null;
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getRequiredLevel() { return requiredLevel; }
    public int getRewardExp() { return rewardExp; }
    public String getRewardItem() { return rewardItem; }

    public void setRequiredLevel(int level) { this.requiredLevel = level; }
    public void setRewardExp(int exp) { this.rewardExp = exp; }
    public void setRewardItem(String item) { this.rewardItem = item; }
}
