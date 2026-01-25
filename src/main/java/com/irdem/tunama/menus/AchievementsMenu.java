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
        int completedAchievements = 0;

        // Cargar logros desde el AchievementManager
        java.util.Map<String, com.irdem.tunama.data.Achievement> allAchievements = plugin.getAchievementManager().getAllAchievements();
        
        for (com.irdem.tunama.data.Achievement achievement : allAchievements.values()) {
            boolean isCompleted = checkAchievementCompleted(achievement, playerLevel);
            
            inventory.setItem(slot++, createAchievementItem(isCompleted, "§6" + achievement.getName(),
                "§7" + achievement.getDescription(),
                "§7Categoría: §f" + achievement.getCategory(),
                "§7Requisito: §f" + achievement.getRequirement(),
                isCompleted ? "§a✓ Completado" : "§7Progreso: " + getProgressText(achievement, playerLevel)
            ));
            
            if (isCompleted) {
                completedAchievements++;
            }
            
            if (slot > 35) break;
        }

        // Info de logros (slot 29)
        int totalAchievements = allAchievements.size();

        inventory.setItem(29, createInfoItem("§6Info de Logros",
            "§7Completados: §f" + completedAchievements + "§7/§f" + totalAchievements,
            "§7Porcentaje: §f" + (completedAchievements * 100 / totalAchievements) + "%",
            "§7Nivel: §f" + playerLevel
        ));

        // Botón Volver (slot 49)
        inventory.setItem(49, createAchievementItem(Material.BARRIER, false, "§cVolver", 
            "§7Haz clic para volver al menú anterior"
        ));
    }

    private boolean checkAchievementCompleted(com.irdem.tunama.data.Achievement achievement, int playerLevel) {
        String category = achievement.getCategory().toLowerCase();
        int requirement = achievement.getRequirement();
        
        if (category.equals("nivel")) {
            return playerLevel >= requirement;
        } else if (category.equals("selección")) {
            if (achievement.getId().contains("raza")) {
                return playerData.getRace() != null && !playerData.getRace().isEmpty();
            } else if (achievement.getId().contains("clase")) {
                return playerData.getPlayerClass() != null && !playerData.getPlayerClass().isEmpty();
            } else if (achievement.getId().contains("subclase")) {
                return playerData.getSubclass() != null && !playerData.getSubclass().isEmpty();
            }
        }
        return false;
    }

    private String getProgressText(com.irdem.tunama.data.Achievement achievement, int playerLevel) {
        String category = achievement.getCategory().toLowerCase();
        int requirement = achievement.getRequirement();
        
        if (category.equals("nivel")) {
            return playerLevel + "/" + requirement;
        }
        return "No completado";
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
