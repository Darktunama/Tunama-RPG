package com.irdem.tunama.data;

import java.util.*;

public class Clan {
    private int id;
    private String tag;
    private String name;
    private UUID leaderId;
    private long gold;
    private int pvpKills;
    private long createdAt;
    private Map<UUID, ClanRank> members;
    private Set<String> allies;
    private Set<String> enemies;

    public Clan(int id, String tag, String name, UUID leaderId) {
        this.id = id;
        this.tag = tag;
        this.name = name;
        this.leaderId = leaderId;
        this.gold = 0;
        this.pvpKills = 0;
        this.createdAt = System.currentTimeMillis();
        this.members = new HashMap<>();
        this.allies = new HashSet<>();
        this.enemies = new HashSet<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    /**
     * Retorna el tag formateado con colores y brackets rojos
     * Los colores en el tag usan & como prefijo que se convierte a §
     * Ejemplo: tag="&aVerde" -> "§c[§a§lVerde§c]§r"
     */
    public String getFormattedTag() {
        // Convertir códigos de color & a §
        String coloredTag = tag.replace("&", "§");
        // Añadir bold y brackets rojos
        return "§c[§r" + coloredTag + "§c]§r";
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getLeaderId() { return leaderId; }
    public void setLeaderId(UUID leaderId) { this.leaderId = leaderId; }

    public long getGold() { return gold; }
    public void setGold(long gold) { this.gold = gold; }
    public void addGold(long amount) { this.gold += amount; }
    public void removeGold(long amount) { this.gold -= amount; }

    public int getPvpKills() { return pvpKills; }
    public void setPvpKills(int pvpKills) { this.pvpKills = pvpKills; }
    public void addPvpKill() { this.pvpKills++; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<UUID, ClanRank> getMembers() { return members; }
    public void setMembers(Map<UUID, ClanRank> members) { this.members = members; }
    public void addMember(UUID uuid, ClanRank rank) { this.members.put(uuid, rank); }
    public void removeMember(UUID uuid) { this.members.remove(uuid); }
    public ClanRank getMemberRank(UUID uuid) { return members.get(uuid); }
    public boolean isMember(UUID uuid) { return members.containsKey(uuid); }
    public int getMemberCount() { return members.size(); }

    public Set<String> getAllies() { return allies; }
    public void setAllies(Set<String> allies) { this.allies = allies; }
    public void addAlly(String clanTag) { this.allies.add(clanTag); }
    public void removeAlly(String clanTag) { this.allies.remove(clanTag); }
    public boolean isAlly(String clanTag) { return allies.contains(clanTag); }

    public Set<String> getEnemies() { return enemies; }
    public void setEnemies(Set<String> enemies) { this.enemies = enemies; }
    public void addEnemy(String clanTag) { this.enemies.add(clanTag); }
    public void removeEnemy(String clanTag) { this.enemies.remove(clanTag); }
    public boolean isEnemy(String clanTag) { return enemies.contains(clanTag); }

    public enum ClanRank {
        LEADER("Líder"),
        COMMANDER("Comandante"),
        MEMBER("Miembro");

        private final String displayName;

        ClanRank(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
