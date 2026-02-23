package com.irdem.tunama.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    // Cache global de maná para que persista entre llamadas a getPlayerData()
    private static final Map<UUID, Integer> manaCache = new HashMap<>();

    private UUID uuid;
    private String username;
    private String race;
    private String playerClass;
    private String subclass;
    private int level;
    private long experience;
    private PlayerStats stats;
    private int statPoints; // Puntos de estadística disponibles para distribuir
    private String clanName;
    private int currentMana;
    private int maxMana;
    
    // Slots de equipo
    private String ring1;
    private String ring2;
    private String ring3;
    private String ring4;
    private String necklace;
    private String amulet1;
    private String amulet2;
    private String wings;

    public PlayerData(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.level = 1;
        this.experience = 0;
        this.stats = new PlayerStats();
        this.statPoints = 0;
        this.maxMana = 100 + (this.stats.getIntelligence() * 5);
        // Usar maná cacheado si existe, sino empezar con maná lleno
        if (manaCache.containsKey(uuid)) {
            this.currentMana = Math.min(manaCache.get(uuid), this.maxMana);
        } else {
            this.currentMana = this.maxMana;
        }
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
    public int getStatPoints() { return statPoints; }
    public String getClanName() { return clanName; }

    
    // Equipment getters
    public String getRing1() { return ring1; }
    public String getRing2() { return ring2; }
    public String getRing3() { return ring3; }
    public String getRing4() { return ring4; }
    public String getNecklace() { return necklace; }
    public String getAmulet1() { return amulet1; }
    public String getAmulet2() { return amulet2; }
    public String getWings() { return wings; }
    
    public void setRace(String race) { this.race = race; }
    public void setPlayerClass(String playerClass) { this.playerClass = playerClass; }
    public void setSubclass(String subclass) { this.subclass = subclass; }
    public void setLevel(int level) { this.level = level; }
    public void addExperience(long amount) { this.experience += amount; }
    public void setStatPoints(int statPoints) { this.statPoints = statPoints; }
    public void addStatPoints(int amount) { this.statPoints += amount; }
    public boolean useStatPoint() { 
        if (this.statPoints > 0) {
            this.statPoints--;
            return true;
        }
        return false;
    }
    public void setClanName(String clanName) { this.clanName = clanName; }

    public int getCurrentMana() { return currentMana; }
    public int getMaxMana() { return maxMana; }
    public void setCurrentMana(int mana) {
        this.currentMana = Math.max(0, Math.min(mana, maxMana));
        manaCache.put(uuid, this.currentMana);
    }
    public void setMaxMana(int maxMana) { this.maxMana = maxMana; }
    public void recalculateMaxMana() {
        int oldMaxMana = this.maxMana;
        this.maxMana = 100 + (this.stats.getIntelligence() * 5);

        // Si el máximo aumentó, restaurar currentMana desde el cache
        if (this.maxMana > oldMaxMana && manaCache.containsKey(uuid)) {
            this.currentMana = Math.min(manaCache.get(uuid), this.maxMana);
        }
    }
    public boolean useMana(int amount) {
        if (currentMana >= amount) {
            currentMana -= amount;
            manaCache.put(uuid, currentMana);
            return true;
        }
        return false;
    }
    public void regenMana(int amount) {
        this.currentMana = Math.min(currentMana + amount, maxMana);
        manaCache.put(uuid, currentMana);
    }
    public static void clearManaCache(UUID uuid) { manaCache.remove(uuid); }
    
    // Equipment setters
    public void setRing1(String ring1) { this.ring1 = ring1; }
    public void setRing2(String ring2) { this.ring2 = ring2; }
    public void setRing3(String ring3) { this.ring3 = ring3; }
    public void setRing4(String ring4) { this.ring4 = ring4; }
    public void setNecklace(String necklace) { this.necklace = necklace; }
    public void setAmulet1(String amulet1) { this.amulet1 = amulet1; }
    public void setAmulet2(String amulet2) { this.amulet2 = amulet2; }
    public void setWings(String wings) { this.wings = wings; }

    /**
     * Obtiene todos los IDs de objetos equipados
     */
    public String[] getEquippedItemIds() {
        return new String[] { ring1, ring2, ring3, ring4, necklace, amulet1, amulet2, wings };
    }

    /**
     * Calcula los stats totales otorgados por el equipo
     * @param itemManager El ItemManager para obtener los objetos
     * @return Map con los stats totales del equipo
     */
    public Map<String, Integer> getEquipmentStats(com.irdem.tunama.managers.ItemManager itemManager) {
        Map<String, Integer> equipStats = new HashMap<>();

        for (String itemId : getEquippedItemIds()) {
            if (itemId == null || itemId.isEmpty()) continue;

            // Extraer el ID del objeto del nombre (puede tener códigos de color)
            String cleanId = extractItemId(itemId);
            RPGItem rpgItem = itemManager.getItem(cleanId);

            if (rpgItem != null && rpgItem.getStats() != null) {
                for (Map.Entry<String, Integer> stat : rpgItem.getStats().entrySet()) {
                    equipStats.merge(stat.getKey(), stat.getValue(), Integer::sum);
                }
            }
        }

        return equipStats;
    }

    /**
     * Extrae el ID limpio del objeto (sin códigos de color)
     */
    private String extractItemId(String itemName) {
        if (itemName == null) return null;
        // Remover códigos de color de Minecraft (§x)
        String clean = itemName.replaceAll("§[0-9a-fklmnor]", "");
        // Convertir a minúsculas y reemplazar espacios
        return clean.toLowerCase().trim();
    }
}
