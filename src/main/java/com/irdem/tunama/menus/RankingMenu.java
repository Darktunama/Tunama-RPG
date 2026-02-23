package com.irdem.tunama.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.Race;
import com.irdem.tunama.data.RPGClass;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RankingMenu implements InventoryHolder {
    private Inventory inventory;
    private TunamaRPG plugin;
    private Player player;

    public RankingMenu(TunamaRPG plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "§6Top Jugadores");
        setupItems();
    }

    private void setupItems() {
        // Top 5 Nivel (slots 10-14)
        List<Map<String, Object>> topLevel = plugin.getDatabaseManager().getTopPlayersByLevel(5);
        int slot = 10;
        for (int i = 0; i < topLevel.size() && i < 5; i++) {
            Map<String, Object> playerInfo = topLevel.get(i);
            String position = getPositionColor(i + 1) + "⭐ Posición " + (i + 1);
            String raceId = (String) playerInfo.get("race");
            String classId = (String) playerInfo.get("class");
            Race r = raceId != null && !raceId.isEmpty() ? plugin.getRaceManager().getRace(raceId.toLowerCase()) : null;
            RPGClass c = classId != null && !classId.isEmpty() ? plugin.getClassManager().getClass(classId.toLowerCase()) : null;
            String raceDisplay = r != null ? r.getName() : (raceId != null ? raceId : "Sin asignar");
            String classDisplay = c != null ? c.getName() : (classId != null ? classId : "Sin asignar");
            inventory.setItem(slot++, createRankingItem(position,
                "§7Jugador: §f" + playerInfo.get("username"),
                "§7Nivel: §f" + playerInfo.get("level"),
                "§7EXP: §f" + playerInfo.get("experience"),
                "§7Raza: §f" + raceDisplay,
                "§7Clase: §f" + classDisplay
            ));
        }

        // Top 5 Kills Mobs (slots 19-23)
        List<Map<String, Object>> topMobKills = plugin.getDatabaseManager().getTopPlayersByMobKills(5);
        slot = 19;
        for (int i = 0; i < topMobKills.size() && i < 5; i++) {
            Map<String, Object> playerInfo = topMobKills.get(i);
            String position = getPositionColor(i + 1) + "⭐ Posición " + (i + 1);
            inventory.setItem(slot++, createRankingItem(Material.ZOMBIE_HEAD, position, 
                "§7Jugador: §f" + playerInfo.get("username"),
                "§7Kills de Mobs: §f" + playerInfo.get("mob_kills"),
                "§7Nivel: §f" + playerInfo.get("level")
            ));
        }

        // Top 5 Kills Jugadores (slots 28-32)
        List<Map<String, Object>> topPlayerKills = plugin.getDatabaseManager().getTopPlayersByPlayerKills(5);
        slot = 28;
        for (int i = 0; i < topPlayerKills.size() && i < 5; i++) {
            Map<String, Object> playerInfo = topPlayerKills.get(i);
            String position = getPositionColor(i + 1) + "⭐ Posición " + (i + 1);
            inventory.setItem(slot++, createRankingItem(Material.PLAYER_HEAD, position, 
                "§7Jugador: §f" + playerInfo.get("username"),
                "§7Kills de Jugadores: §f" + playerInfo.get("player_kills"),
                "§7Nivel: §f" + playerInfo.get("level")
            ));
        }

        // Títulos de secciones
        inventory.setItem(9, createRankingItem(Material.GOLD_BLOCK, "§6Top 5 Nivel", "§7Jugadores con mayor nivel"));
        inventory.setItem(18, createRankingItem(Material.ZOMBIE_HEAD, "§cTop 5 Kills Mobs", "§7Jugadores con más kills de mobs"));
        inventory.setItem(27, createRankingItem(Material.PLAYER_HEAD, "§4Top 5 Kills Jugadores", "§7Jugadores con más kills de jugadores"));

        // Botón Volver (slot 49)
        inventory.setItem(49, createRankingItem(Material.BARRIER, "§cVolver", 
            "§7Haz clic para volver al menú anterior"
        ));
    }

    private String getPositionColor(int position) {
        switch (position) {
            case 1: return "§6";
            case 2: return "§e";
            case 3: return "§c";
            default: return "§7";
        }
    }

    private ItemStack createRankingItem(String name, String... lore) {
        return createRankingItem(Material.GOLD_BLOCK, name, lore);
    }

    private ItemStack createRankingItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }
}
