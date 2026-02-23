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
import com.irdem.tunama.data.RPGClass;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        // Obtener stats del equipo
        Map<String, Integer> equipStats = playerData.getEquipmentStats(plugin.getItemManager());
        int equipHealth = equipStats.getOrDefault("health", 0);
        int equipStrength = equipStats.getOrDefault("strength", 0);
        int equipAgility = equipStats.getOrDefault("agility", 0);
        int equipIntelligence = equipStats.getOrDefault("intelligence", 0);
        int equipSacred = equipStats.getOrDefault("sacred", 0);
        int equipCorrupt = equipStats.getOrDefault("corrupt", 0);
        int equipNature = equipStats.getOrDefault("nature", 0);

        // Calcular estadísticas ajustadas por raza + equipo
        int baseHealth = playerData.getStats().getHealth();
        int baseStrength = playerData.getStats().getStrength();
        int baseAgility = playerData.getStats().getAgility();
        int baseIntelligence = playerData.getStats().getIntelligence();

        int totalHealth = (int)((baseHealth + equipHealth) * lifeMult);
        int totalStrength = (int)((baseStrength + equipStrength) * strengthMult);
        int totalAgility = (int)((baseAgility + equipAgility) * agilityMult);
        int totalIntelligence = (int)((baseIntelligence + equipIntelligence) * intelligenceMult);

        // Fila 1: Estadísticas Base (slots 10-13)
        int statPoints = playerData.getStatPoints();
        String clickHint = statPoints > 0 ? "§a§l¡Haz clic para aumentar!" : "§7No tienes puntos disponibles";
        String shiftHint = statPoints > 0 ? "§e⚡ Shift+Click §7▸ Sube 5 puntos a la vez" : "";

        inventory.setItem(10, createStatItem(Material.REDSTONE, "§cVida",
            "§7Base: §f" + baseHealth,
            equipHealth > 0 ? "§7Equipo: §a+" + equipHealth : "",
            "§7Total (con raza): §f" + totalHealth,
            "§7Multiplicador: §f" + String.format("%.2f", lifeMult) + "x",
            "",
            clickHint,
            shiftHint
        ));

        inventory.setItem(11, createStatItem(Material.NETHERITE_PICKAXE, "§cFuerza",
            "§7Base: §f" + baseStrength,
            equipStrength > 0 ? "§7Equipo: §a+" + equipStrength : "",
            "§7Total (con raza): §f" + totalStrength,
            "§7Multiplicador: §f" + String.format("%.2f", strengthMult) + "x",
            "",
            clickHint,
            shiftHint
        ));

        int speedBonus = totalAgility / 50;
        inventory.setItem(12, createStatItem(Material.FEATHER, "§eAgilidad",
            "§7Base: §f" + baseAgility,
            equipAgility > 0 ? "§7Equipo: §a+" + equipAgility : "",
            "§7Total (con raza): §f" + totalAgility,
            "§7Multiplicador: §f" + String.format("%.2f", agilityMult) + "x",
            speedBonus > 0 ? "§7Bonus velocidad: §a+" + speedBonus : "",
            "",
            clickHint,
            shiftHint
        ));

        inventory.setItem(13, createStatItem(Material.ENCHANTED_BOOK, "§5Inteligencia",
            "§7Base: §f" + baseIntelligence,
            equipIntelligence > 0 ? "§7Equipo: §a+" + equipIntelligence : "",
            "§7Total (con raza): §f" + totalIntelligence,
            "§7Multiplicador: §f" + String.format("%.2f", intelligenceMult) + "x",
            "",
            clickHint,
            shiftHint
        ));

        // Fila 2: Poderes (slots 19-21)
        // Obtener costes de la raza para los poderes
        double sacredCost = race != null ? race.getSagradoPowerCost() : 1.0;
        double corruptCost = race != null ? race.getCorruptPowerCost() : 1.0;
        double natureCost = race != null ? race.getNaturePowerCost() : 1.0;

        int baseSacred = playerData.getStats().getSacredPower();
        int baseCorrupt = playerData.getStats().getCorruptPower();
        int baseNature = playerData.getStats().getNaturePower();

        inventory.setItem(19, createStatItem(Material.GLOW_BERRIES, "§6Poder Sagrado",
            "§7Base: §f" + baseSacred,
            equipSacred > 0 ? "§7Equipo: §a+" + equipSacred : "",
            "§7Total: §f" + (baseSacred + equipSacred),
            "§7Coste: §f" + String.format("%.1f", sacredCost) + " puntos",
            "",
            clickHint,
            shiftHint
        ));

        inventory.setItem(20, createStatItem(Material.AMETHYST_SHARD, "§4Poder Corrupto",
            "§7Base: §f" + baseCorrupt,
            equipCorrupt > 0 ? "§7Equipo: §a+" + equipCorrupt : "",
            "§7Total: §f" + (baseCorrupt + equipCorrupt),
            "§7Coste: §f" + String.format("%.1f", corruptCost) + " puntos",
            "",
            clickHint,
            shiftHint
        ));

        inventory.setItem(21, createStatItem(Material.MOSS_BLOCK, "§2Poder de la Naturaleza",
            "§7Base: §f" + baseNature,
            equipNature > 0 ? "§7Equipo: §a+" + equipNature : "",
            "§7Total: §f" + (baseNature + equipNature),
            "§7Coste: §f" + String.format("%.1f", natureCost) + " puntos",
            "",
            clickHint,
            shiftHint
        ));

        // Info del Jugador (slot 29)
        String raceDisplay = "Sin asignar";
        if (playerData.getRace() != null && !playerData.getRace().isEmpty()) {
            raceDisplay = race != null ? race.getName() : playerData.getRace();
        }
        RPGClass rpgClass = playerData.getPlayerClass() != null && !playerData.getPlayerClass().isEmpty() ?
            plugin.getClassManager().getClass(playerData.getPlayerClass().toLowerCase()) : null;
        String classDisplay = rpgClass != null ? rpgClass.getName() : "Sin asignar";
        
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Raza: §f" + raceDisplay);
        infoLore.add("§7Clase: §f" + classDisplay);
        infoLore.add("§7Nivel: §f" + playerData.getLevel());
        infoLore.add("§7EXP: §f" + playerData.getExperience());
        infoLore.add("");
        infoLore.add("§e§lPuntos de Estadística Disponibles: §f§l" + statPoints);
        if (statPoints > 0) {
            infoLore.add("§7Haz clic en una estadística para");
            infoLore.add("§7distribuir tus puntos");
        }
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName("§6Info del Jugador");
            skullMeta.setLore(infoLore);
            playerHead.setItemMeta(skullMeta);
        }
        inventory.setItem(29, playerHead);

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
                // Filtrar líneas vacías (usadas para stats de equipo opcionales)
                if (line != null && !line.isEmpty()) {
                    loreList.add(line);
                }
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
