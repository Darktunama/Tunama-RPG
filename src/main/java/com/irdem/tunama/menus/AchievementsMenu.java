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

public class AchievementsMenu implements InventoryHolder {
    private Inventory inventory;
    private TunamaRPG plugin;
    private Player player;
    private PlayerData playerData;

    public AchievementsMenu(TunamaRPG plugin, Player player, PlayerData playerData) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = playerData;
        this.inventory = Bukkit.createInventory(this, 54, "§6Logros");
        setupItems();
    }

    private void setupItems() {
        int playerLevel = playerData.getLevel();
        int slot = 10;

        // Logro: Primer Paso (nivel 1+)
        inventory.setItem(slot++, createAchievementItem(true, "§6Primer Paso",
            "§7Alcanza nivel 5",
            playerLevel >= 5 ? "§a✓ Completado" : "§7Progreso: " + playerLevel + "/5"
        ));

        // Logro: Crecimiento (nivel 10+)
        inventory.setItem(slot++, createAchievementItem(playerLevel >= 10, "§6Crecimiento",
            "§7Alcanza nivel 10",
            playerLevel >= 10 ? "§a✓ Completado" : "§7Progreso: " + playerLevel + "/10"
        ));

        // Logro: Guerrero (nivel 15+)
        inventory.setItem(slot++, createAchievementItem(playerLevel >= 15, "§6Guerrero",
            "§7Alcanza nivel 15",
            playerLevel >= 15 ? "§a✓ Completado" : "§7Progreso: " + playerLevel + "/15"
        ));

        // Logro: Veterano (nivel 20+)
        inventory.setItem(slot++, createAchievementItem(playerLevel >= 20, "§4Veterano",
            "§7Alcanza nivel 20",
            playerLevel >= 20 ? "§a✓ Completado" : "§7Progreso: " + playerLevel + "/20"
        ));

        // Logro: Maestro (nivel 30+)
        inventory.setItem(slot++, createAchievementItem(playerLevel >= 30, "§5Maestro",
            "§7Alcanza nivel 30",
            playerLevel >= 30 ? "§a✓ Completado" : "§7Progreso: " + playerLevel + "/30"
        ));

        // Logro: Legenda (nivel 50+)
        inventory.setItem(slot++, createAchievementItem(playerLevel >= 50, "§4Legenda",
            "§7Alcanza nivel 50",
            playerLevel >= 50 ? "§a✓ Completado" : "§7Progreso: " + playerLevel + "/50"
        ));

        // Logro: Selección de Raza
        boolean hasRace = playerData.getRace() != null && !playerData.getRace().isEmpty();
        inventory.setItem(slot++, createAchievementItem(hasRace, "§6Elegido de Razas",
            "§7Selecciona una raza",
            hasRace ? "§a✓ Completado" : "§cNo completado"
        ));

        // Logro: Selección de Clase
        boolean hasClass = playerData.getPlayerClass() != null && !playerData.getPlayerClass().isEmpty();
        inventory.setItem(slot++, createAchievementItem(hasClass, "§6Profesional",
            "§7Selecciona una clase",
            hasClass ? "§a✓ Completado" : "§cNo completado"
        ));

        // Logro: Selección de Subclase
        boolean hasSubclass = playerData.getSubclass() != null && !playerData.getSubclass().isEmpty();
        inventory.setItem(slot++, createAchievementItem(hasSubclass, "§5Especialista",
            "§7Selecciona una subclase",
            hasSubclass ? "§a✓ Completado" : "§cNo completado"
        ));

        // Info de logros (slot 29)
        int totalAchievements = 9;
        int completedAchievements = 0;
        if (playerLevel >= 5) completedAchievements++;
        if (playerLevel >= 10) completedAchievements++;
        if (playerLevel >= 15) completedAchievements++;
        if (playerLevel >= 20) completedAchievements++;
        if (playerLevel >= 30) completedAchievements++;
        if (playerLevel >= 50) completedAchievements++;
        if (hasRace) completedAchievements++;
        if (hasClass) completedAchievements++;
        if (hasSubclass) completedAchievements++;

        inventory.setItem(29, createInfoItem("§6Info de Logros",
            "§7Completados: §f" + completedAchievements + "§7/§f" + totalAchievements,
            "§7Porcentaje: §f" + (completedAchievements * 100 / totalAchievements) + "%"
        ));

        // Botón Volver (slot 49)
        inventory.setItem(49, createAchievementItem(Material.BARRIER, false, "§cVolver", 
            "§7Haz clic para volver al menú anterior"
        ));
    }

    private ItemStack createAchievementItem(boolean completed, String name, String... lore) {
        Material material = completed ? Material.GOLD_BLOCK : Material.BARRIER;
        return createAchievementItem(material, completed, name, lore);
    }

    private ItemStack createAchievementItem(Material material, boolean completed, String name, String... lore) {
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

    private ItemStack createInfoItem(String name, String... lore) {
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
