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
import java.util.Map;

public class AbilitiesMenu implements InventoryHolder {
    private Inventory inventory;
    private TunamaRPG plugin;
    private Player player;
    private PlayerData playerData;

    public AbilitiesMenu(TunamaRPG plugin, Player player, PlayerData playerData) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = playerData;
        this.inventory = Bukkit.createInventory(this, 54, "§c⚔ Habilidades");
        setupItems();
    }

    private void setupItems() {
        int playerLevel = playerData.getLevel();
        String playerClass = playerData.getPlayerClass();
        
        if (playerClass == null || playerClass.isEmpty()) {
            inventory.setItem(22, createAbilityItem(Material.BARRIER, "§cSin Clase",
                "§7Primero debes seleccionar una clase"
            ));
            inventory.setItem(49, createAbilityItem(Material.BARRIER, "§cVolver",
                "§7Haz clic para volver al menú anterior"
            ));
            return;
        }

        // Obtener habilidades de la clase del jugador
        Map<String, com.irdem.tunama.data.Ability> classAbilities = plugin.getAbilityManager().getAbilitiesByClass(playerClass);
        int slot = 10;

        if (classAbilities.isEmpty()) {
            inventory.setItem(22, createAbilityItem(Material.BARRIER, "§cSin Habilidades",
                "§7Tu clase no tiene habilidades asignadas aún"
            ));
        } else {
            for (com.irdem.tunama.data.Ability ability : classAbilities.values()) {
                boolean isUnlocked = playerLevel >= ability.getRequiredLevel();
                
                inventory.setItem(slot++, createAbilityItem(
                    isUnlocked ? Material.BLAZE_ROD : Material.STICK,
                    (isUnlocked ? "§c" : "§8") + ability.getName(),
                    "§7" + ability.getDescription(),
                    "§7Clase: §f" + ability.getRpgClass(),
                    "§7Nivel requerido: §f" + ability.getRequiredLevel(),
                    "§7Costo de maná: §f" + ability.getManaCost(),
                    isUnlocked ? "§a✓ Desbloqueada" : "§c✗ Bloqueada"
                ));
                
                if (slot > 35) break;
            }
        }

        // Info de habilidades (slot 29)
        int totalAbilities = classAbilities.size();
        int unlockedAbilities = 0;
        for (com.irdem.tunama.data.Ability ability : classAbilities.values()) {
            if (playerLevel >= ability.getRequiredLevel()) {
                unlockedAbilities++;
            }
        }

        inventory.setItem(29, createAbilityInfo("§c⚔ Info de Habilidades",
            "§7Clase: §f" + playerClass,
            "§7Desbloqueadas: §f" + unlockedAbilities + "§7/§f" + totalAbilities,
            "§7Nivel: §f" + playerLevel
        ));

        // Botón Volver (slot 49)
        inventory.setItem(49, createAbilityItem(Material.BARRIER, "§cVolver",
            "§7Haz clic para volver al menú anterior"
        ));
    }

    private ItemStack createAbilityItem(Material material, String name, String... lore) {
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

    private ItemStack createAbilityInfo(String name, String... lore) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
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
