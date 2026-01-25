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

public class RankingMenu implements InventoryHolder {
    private Inventory inventory;
    private TunamaRPG plugin;
    private Player player;

    public RankingMenu(TunamaRPG plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "§6Mejores Jugadores");
        setupItems();
    }

    private void setupItems() {
        // Simulamos un ranking de jugadores
        // En producción, esto vendría de la base de datos
        
        int slot = 10;

        // Puesto 1
        inventory.setItem(slot++, createRankingItem("§6⭐ Posición 1", 
            "§7Jugador: §fKaiser",
            "§7Nivel: §f50",
            "§7EXP: §f500,000",
            "§7Raza: §fDragoneante",
            "§7Clase: §fGuerrero"
        ));

        // Puesto 2
        inventory.setItem(slot++, createRankingItem("§e⭐ Posición 2", 
            "§7Jugador: §fLunastar",
            "§7Nivel: §f47",
            "§7EXP: §f450,000",
            "§7Raza: §fElfo",
            "§7Clase: §fMago"
        ));

        // Puesto 3
        inventory.setItem(slot++, createRankingItem("§c⭐ Posición 3", 
            "§7Jugador: §fDarkShadow",
            "§7Nivel: §f45",
            "§7EXP: §f400,000",
            "§7Raza: §fSemielfo",
            "§7Clase: §fPícaro"
        ));

        // Puesto 4
        inventory.setItem(slot++, createRankingItem("§7Posición 4", 
            "§7Jugador: §fThunderStrike",
            "§7Nivel: §f42",
            "§7EXP: §f350,000",
            "§7Raza: §fEnano",
            "§7Clase: §fArquero"
        ));

        // Puesto 5
        inventory.setItem(slot++, createRankingItem("§7Posición 5", 
            "§7Jugador: §fMysticDream",
            "§7Nivel: §f40",
            "§7EXP: §f320,000",
            "§7Raza: §fHumano",
            "§7Clase: §fSacerdote"
        ));

        // Tu Posición
        PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (playerData != null) {
            inventory.setItem(29, createRankingItem(Material.PLAYER_HEAD, "§b📍 Tu Posición",
                "§7Jugador: §f" + playerData.getUsername(),
                "§7Nivel: §f" + playerData.getLevel(),
                "§7EXP: §f" + playerData.getExperience(),
                "§7Raza: §f" + (playerData.getRace() != null ? playerData.getRace() : "Sin asignar"),
                "§7Clase: §f" + (playerData.getPlayerClass() != null ? playerData.getPlayerClass() : "Sin asignar")
            ));
        }

        // Botón Volver (slot 49)
        inventory.setItem(49, createRankingItem(Material.BARRIER, "§cVolver", 
            "§7Haz clic para volver al menú anterior"
        ));
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
