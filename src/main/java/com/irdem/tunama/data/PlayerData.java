package com.irdem.tunama.data;

import java.util.UUID;

public class PlayerData {
    private UUID uuid;
    private String username;
    private String race;
    private String playerClass;
    private String subclass;
    private int level;
    private long experience;
    private PlayerStats stats;
    private String clanName;

    public PlayerData(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.level = 1;
        this.experience = 0;
        this.stats = new PlayerStats();
    }

    // Getters y Setters
    public UUID getUUID() { return uuid; }
    public String getUsername() { return username; }
    public String getRace() { return race; }
    public String getPlayerClass() { return playerClass; }
    public String getSubclass() { return subclass; }
    public int getLevel() { return level; }
    public long getExperience() { return experience; }
    public PlayerStats getStats() { return stats; }
    public String getClanName() { return clanName; }
    
    public void setRace(String race) { this.race = race; }
    public void setPlayerClass(String playerClass) { this.playerClass = playerClass; }
    public void setSubclass(String subclass) { this.subclass = subclass; }
    public void setLevel(int level) { this.level = level; }
    public void addExperience(long amount) { this.experience += amount; }
    public void setClanName(String clanName) { this.clanName = clanName; }
}
