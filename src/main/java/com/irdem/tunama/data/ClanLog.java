package com.irdem.tunama.data;

import java.util.UUID;

public class ClanLog {
    private int id;
    private int clanId;
    private UUID playerId;
    private String playerName;
    private String action;
    private long amount;
    private String item;
    private long timestamp;

    public ClanLog(int clanId, UUID playerId, String playerName, String action, long amount, String item) {
        this.clanId = clanId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.action = action;
        this.amount = amount;
        this.item = item;
        this.timestamp = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClanId() { return clanId; }
    public void setClanId(int clanId) { this.clanId = clanId; }

    public UUID getPlayerId() { return playerId; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
