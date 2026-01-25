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
    
    // Slots de equipo
    private String ring1;
    private String ring2;
    private String ring3;
    private String ring4;
    private String necklace;
    private String amulet1;
    private String amulet2;

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
    
    // Equipment getters
    public String getRing1() { return ring1; }
    public String getRing2() { return ring2; }
    public String getRing3() { return ring3; }
    public String getRing4() { return ring4; }
    public String getNecklace() { return necklace; }
    public String getAmulet1() { return amulet1; }
    public String getAmulet2() { return amulet2; }
    
    public void setRace(String race) { this.race = race; }
    public void setPlayerClass(String playerClass) { this.playerClass = playerClass; }
    public void setSubclass(String subclass) { this.subclass = subclass; }
    public void setLevel(int level) { this.level = level; }
    public void addExperience(long amount) { this.experience += amount; }
    public void setClanName(String clanName) { this.clanName = clanName; }
    
    // Equipment setters
    public void setRing1(String ring1) { this.ring1 = ring1; }
    public void setRing2(String ring2) { this.ring2 = ring2; }
    public void setRing3(String ring3) { this.ring3 = ring3; }
    public void setRing4(String ring4) { this.ring4 = ring4; }
    public void setNecklace(String necklace) { this.necklace = necklace; }
    public void setAmulet1(String amulet1) { this.amulet1 = amulet1; }
    public void setAmulet2(String amulet2) { this.amulet2 = amulet2; }
}
