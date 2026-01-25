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

        // Misiones básicas (nivel 1+)
        if (playerLevel >= 1) {
            inventory.setItem(slot++, createMissionItem("§6Derrota 5 Zombies", 
                "§7Recompensa: 50 EXP",
                "§7Nivel requerido: §f1",
                "§eEstado: §aActiva"
            ));
        }

        // Misiones intermedias (nivel 10+)
        if (playerLevel >= 10) {
            inventory.setItem(slot++, createMissionItem("§6Recauda 10 Cristales", 
                "§7Recompensa: 150 EXP",
                "§7Nivel requerido: §f10",
                "§eEstado: §aActiva"
            ));
        }

        // Misiones avanzadas (nivel 20+)
        if (playerLevel >= 20) {
            inventory.setItem(slot++, createMissionItem("§6Derrota al Jefe Esqueleto", 
                "§7Recompensa: 500 EXP + Cofre de Recompensas",
                "§7Nivel requerido: §f20",
                "§eEstado: §aActiva"
            ));
        }

        // Misiones épicas (nivel 30+)
        if (playerLevel >= 30) {
            inventory.setItem(slot++, createMissionItem("§4Derrota al Señor del Caos", 
                "§7Recompensa: 2000 EXP + Objeto Legendario",
                "§7Nivel requerido: §f30",
                "§eEstado: §aActiva"
            ));
        }

        // Misiones bloqueadas
        if (playerLevel < 50) {
            inventory.setItem(slot++, createMissionItem(Material.BARRIER, "§cMisión Bloqueada",
                "§7Derrota al Titán Oscuro",
                "§7Nivel requerido: §f50",
                "§cEstado: §cBloqueada"
            ));
        }

        // Info de misiones (slot 29)
        int completedMissions = (playerLevel / 10);
        inventory.setItem(29, createMissionInfo("§6Info de Misiones",
            "§7Misiones completadas: §f" + completedMissions,
            "§7Misiones disponibles: §f" + (slot - 10),
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
