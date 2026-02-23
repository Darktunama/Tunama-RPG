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
import com.irdem.tunama.data.RPGClass;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        Map<String, com.irdem.tunama.data.Ability> allClassAbilities = plugin.getAbilityManager().getAbilitiesByClass(playerClass);

        // Lista blanca de habilidades permitidas para el Druida
        Set<String> druidaAllowedAbilities = new HashSet<>(Arrays.asList(
            "cura-natural",
            "forma-de-arana",
            "forma-de-lobo",
            "forma-de-oso",
            "forma-de-warden",
            "forma-de-panda",
            "forma-de-zorro",
            "fuerza-de-la-naturaleza"
        ));

        // Crear nueva lista filtrada (no modificar la original)
        Map<String, com.irdem.tunama.data.Ability> classAbilities = new java.util.HashMap<>();

        boolean isDruida = playerClass.equalsIgnoreCase("druida");
        plugin.getLogger().info("[AbilitiesMenu] Clase: " + playerClass + ", isDruida: " + isDruida);

        for (Map.Entry<String, com.irdem.tunama.data.Ability> entry : allClassAbilities.entrySet()) {
            String id = entry.getValue().getId().toLowerCase();

            if (isDruida) {
                // Para el Druida: solo incluir las habilidades de la lista blanca
                if (druidaAllowedAbilities.contains(id)) {
                    classAbilities.put(entry.getKey(), entry.getValue());
                    plugin.getLogger().info("[AbilitiesMenu] Druida - Incluyendo: " + id);
                } else {
                    plugin.getLogger().info("[AbilitiesMenu] Druida - Excluyendo: " + id);
                }
            } else {
                // Para otras clases: excluir solo las que tienen formOnly=true
                if (!entry.getValue().isFormOnly()) {
                    classAbilities.put(entry.getKey(), entry.getValue());
                }
            }
        }

        plugin.getLogger().info("[AbilitiesMenu] Habilidades finales para mostrar: " + classAbilities.size());

        int slot = 10;

        if (classAbilities.isEmpty()) {
            inventory.setItem(22, createAbilityItem(Material.BARRIER, "§cSin Habilidades",
                "§7Tu clase no tiene habilidades asignadas aún"
            ));
        } else {
            for (com.irdem.tunama.data.Ability ability : classAbilities.values()) {
                boolean isUnlocked = playerLevel >= ability.getRequiredLevel();
                RPGClass abilityClass = ability.getRpgClass() != null ?
                    plugin.getClassManager().getClass(ability.getRpgClass().toLowerCase()) : null;
                String classDisplayName = abilityClass != null ? abilityClass.getName() : (ability.getRpgClass() != null ? ability.getRpgClass() : "?");

                inventory.setItem(slot++, createAbilityItem(
                    isUnlocked ? Material.BLAZE_ROD : Material.STICK,
                    (isUnlocked ? "§c" : "§8") + ability.getName(),
                    "§7" + ability.getDescription(),
                    "§7Clase: §f" + classDisplayName,
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

        RPGClass playerRpgClass = plugin.getClassManager().getClass(playerClass.toLowerCase());
        String classDisplayName = playerRpgClass != null ? playerRpgClass.getName() : playerClass;
        inventory.setItem(29, createAbilityInfo("§c⚔ Info de Habilidades",
            "§7Clase: §f" + classDisplayName,
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
