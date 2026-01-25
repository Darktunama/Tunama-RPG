package com.irdem.tunama.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.PlayerData;
import java.util.ArrayList;
import java.util.List;

public class MissionsMenu implements InventoryHolder {
    private Inventory inventory;
    private TunamaRPG plugin;
    private Player player;
    private PlayerData playerData;

    public MissionsMenu(TunamaRPG plugin, Player player, PlayerData playerData) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = playerData;
        this.inventory = Bukkit.createInventory(this, 54, "§6Misiones");
        setupItems();
    }

    private void setupItems() {
        int playerLevel = playerData.getLevel();
        int slot = 10;

        // Cargar misiones desde el MissionManager
        java.util.Map<String, com.irdem.tunama.data.Mission> allMissions = plugin.getMissionManager().getAllMissions();
        
        for (com.irdem.tunama.data.Mission mission : allMissions.values()) {
            if (playerLevel >= mission.getRequiredLevel()) {
                // Misión disponible
                inventory.setItem(slot++, createMissionItem("§6" + mission.getName(), 
                    "§7" + mission.getDescription(),
                    "§7Nivel requerido: §f" + mission.getRequiredLevel(),
                    "§7Recompensa: §f" + mission.getRewardExp() + " EXP",
                    "§eEstado: §aActiva"
                ));
            } else {
                // Misión bloqueada
                if (slot <= 35) {
                    inventory.setItem(slot++, createMissionItem(Material.BARRIER, "§c" + mission.getName(),
                        "§7" + mission.getDescription(),
                        "§7Nivel requerido: §f" + mission.getRequiredLevel(),
                        "§cEstado: §cBloqueada"
                    ));
                }
            }
            if (slot > 35) break;
        }

        // Info de misiones (slot 29)
        int totalMissions = allMissions.size();
        int completedMissions = (playerLevel / 10);
        int availableMissions = 0;
        for (com.irdem.tunama.data.Mission mission : allMissions.values()) {
            if (playerLevel >= mission.getRequiredLevel()) {
                availableMissions++;
            }
        }
        
        inventory.setItem(29, createMissionInfo("§6Info de Misiones",
            "§7Misiones completadas: §f" + completedMissions,
            "§7Misiones disponibles: §f" + availableMissions + "§7/§f" + totalMissions,
            "§7Nivel: §f" + playerLevel
        ));

        // Botón Volver (slot 49)
        inventory.setItem(49, createMissionItem(Material.BARRIER, "§cVolver", 
            "§7Haz clic para volver al menú anterior"
        ));
    }

    private ItemStack createMissionItem(String name, String... lore) {
        return createMissionItem(Material.WRITABLE_BOOK, name, lore);
    }

    private ItemStack createMissionItem(Material material, String name, String... lore) {
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

    private ItemStack createMissionInfo(String name, String... lore) {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
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
