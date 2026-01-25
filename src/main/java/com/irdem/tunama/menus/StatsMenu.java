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
import com.irdem.tunama.data.Race;
import java.util.ArrayList;
import java.util.List;

public class StatsMenu implements InventoryHolder {
    private Inventory inventory;
    private TunamaRPG plugin;
    private Player player;
    private PlayerData playerData;

    public StatsMenu(TunamaRPG plugin, Player player, PlayerData playerData) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = playerData;
        this.inventory = Bukkit.createInventory(this, 54, "§6Estadísticas");
        setupItems();
    }

    private void setupItems() {
        // Obtener multiplicadores de la raza
        Race race = playerData.getRace() != null ? 
            plugin.getRaceManager().getRace(playerData.getRace().toLowerCase()) : null;
        
        double agilityMult = race != null ? race.getAgilityMultiplier() : 1.0;
        double intelligenceMult = race != null ? race.getIntelligenceMultiplier() : 1.0;
        double lifeMult = race != null ? race.getLifeMultiplier() : 1.0;
        double strengthMult = race != null ? race.getStrengthMultiplier() : 1.0;

        // Calcular estadísticas ajustadas por raza
        int totalHealth = (int)(playerData.getStats().getHealth() * lifeMult);
        int totalStrength = (int)(playerData.getStats().getStrength() * strengthMult);
        int totalAgility = (int)(playerData.getStats().getAgility() * agilityMult);
        int totalIntelligence = (int)(playerData.getStats().getIntelligence() * intelligenceMult);

        // Fila 1: Estadísticas Base (slots 10-13)
        inventory.setItem(10, createStatItem(Material.REDSTONE, "§cVida", 
            "§7Base: §f" + playerData.getStats().getHealth(),
            "§7Total (con raza): §f" + totalHealth,
            "§7Multiplicador: §f" + String.format("%.2f", lifeMult) + "x"
        ));

        inventory.setItem(11, createStatItem(Material.NETHERITE_PICKAXE, "§cFuerza", 
            "§7Base: §f" + playerData.getStats().getStrength(),
            "§7Total (con raza): §f" + totalStrength,
            "§7Multiplicador: §f" + String.format("%.2f", strengthMult) + "x"
        ));

        inventory.setItem(12, createStatItem(Material.FEATHER, "§eAgilidad", 
            "§7Base: §f" + playerData.getStats().getAgility(),
            "§7Total (con raza): §f" + totalAgility,
            "§7Multiplicador: §f" + String.format("%.2f", agilityMult) + "x"
        ));

        inventory.setItem(13, createStatItem(Material.ENCHANTED_BOOK, "§5Inteligencia", 
            "§7Base: §f" + playerData.getStats().getIntelligence(),
            "§7Total (con raza): §f" + totalIntelligence,
            "§7Multiplicador: §f" + String.format("%.2f", intelligenceMult) + "x"
        ));

        // Fila 2: Poderes (slots 19-21)
        inventory.setItem(19, createStatItem(Material.GLOW_BERRIES, "§6Poder Sagrado", 
            "§7Nivel: §f" + playerData.getStats().getSacredPower(),
            "§7Haz clic para subir"
        ));

        inventory.setItem(20, createStatItem(Material.AMETHYST_SHARD, "§4Poder Corrupto", 
            "§7Nivel: §f" + playerData.getStats().getCorruptPower(),
            "§7Haz clic para subir"
        ));

        inventory.setItem(21, createStatItem(Material.MOSS_BLOCK, "§2Poder de la Naturaleza", 
            "§7Nivel: §f" + playerData.getStats().getNaturePower(),
            "§7Haz clic para subir"
        ));

        // Info del Jugador (slot 29)
        inventory.setItem(29, createStatItem(Material.PLAYER_HEAD, "§6Info del Jugador",
            "§7Raza: §f" + (playerData.getRace() != null ? playerData.getRace() : "Sin asignar"),
            "§7Clase: §f" + (playerData.getPlayerClass() != null ? playerData.getPlayerClass() : "Sin asignar"),
            "§7Nivel: §f" + playerData.getLevel(),
            "§7EXP: §f" + playerData.getExperience()
        ));

        // Botón Volver (slot 49)
        inventory.setItem(49, createStatItem(Material.BARRIER, "§cVolver", 
            "§7Haz clic para volver al menú anterior"
        ));
    }

    private ItemStack createStatItem(Material material, String name, String... lore) {
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
