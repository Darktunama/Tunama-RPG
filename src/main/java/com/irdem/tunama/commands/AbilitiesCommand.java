package com.irdem.tunama.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.Ability;
import com.irdem.tunama.data.PlayerData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AbilitiesCommand implements CommandExecutor {

    private final TunamaRPG plugin;

    private static final Map<String, String> WEAPON_NAMES = new HashMap<>();
    static {
        WEAPON_NAMES.put("BOW", "arco");
        WEAPON_NAMES.put("CROSSBOW", "ballesta");
        WEAPON_NAMES.put("DIAMOND_SWORD", "espada de diamante");
        WEAPON_NAMES.put("NETHERITE_SWORD", "espada de netherita");
        WEAPON_NAMES.put("IRON_SWORD", "espada de hierro");
        WEAPON_NAMES.put("TRIDENT", "tridente");
        WEAPON_NAMES.put("SHIELD", "escudo");
    }

    private String translateWeaponName(String materialName) {
        return WEAPON_NAMES.getOrDefault(materialName.toUpperCase(), materialName.toLowerCase().replace("_", " "));
    }
    public static final String MENU_TITLE = "§5§l⚔ Habilidades";

    public AbilitiesCommand(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c✗ Este comando solo puede ser usado por jugadores!");
            return true;
        }

        Player player = (Player) sender;

        PlayerData playerData = plugin.getDatabaseManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        if (playerData == null) {
            player.sendMessage("§c✗ Error: No se pudo obtener tus datos.");
            return true;
        }

        String playerClass = playerData.getPlayerClass();
        if (playerClass == null || playerClass.isEmpty()) {
            player.sendMessage("§c✗ Primero debes seleccionar una clase");
            return true;
        }

        openAbilitiesMenu(player, playerData);
        return true;
    }

    private void openAbilitiesMenu(Player player, PlayerData playerData) {
        String playerClass = playerData.getPlayerClass();
        Map<String, Ability> allClassAbilities = plugin.getAbilityManager().getAbilitiesByClass(playerClass);

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

        // Filtrar habilidades según la clase
        List<Ability> sortedAbilities = new ArrayList<>();
        boolean isDruida = playerClass.equalsIgnoreCase("druida");

        for (Ability ability : allClassAbilities.values()) {
            String id = ability.getId().toLowerCase();

            if (isDruida) {
                // Para el Druida: solo incluir las habilidades de la lista blanca
                if (druidaAllowedAbilities.contains(id)) {
                    sortedAbilities.add(ability);
                }
            } else {
                // Para otras clases: excluir solo las que tienen formOnly=true
                if (!ability.isFormOnly()) {
                    sortedAbilities.add(ability);
                }
            }
        }

        sortedAbilities.sort((a, b) -> Integer.compare(a.getRequiredLevel(), b.getRequiredLevel()));

        int size = Math.max(9, ((sortedAbilities.size() / 9) + 1) * 9);
        if (size > 54) size = 54;
        Inventory menu = Bukkit.createInventory(null, size, MENU_TITLE);

        int playerLevel = playerData.getLevel();
        int currentMana = playerData.getCurrentMana();

        int slot = 0;
        for (Ability ability : sortedAbilities) {
            if (slot >= size) break;

            boolean unlocked = playerLevel >= ability.getRequiredLevel();
            int manaCost = 0;
            try { manaCost = Integer.parseInt(ability.getManaCost()); } catch (NumberFormatException ignored) {}
            boolean hasEnoughMana = currentMana >= manaCost;

            Material mat;
            if (unlocked) {
                mat = Material.BLAZE_ROD;
                try { mat = Material.valueOf(ability.getMaterial()); } catch (IllegalArgumentException ignored) {}
            } else {
                mat = Material.GRAY_DYE;
            }

            ItemStack abilityItem = new ItemStack(mat);
            ItemMeta meta = abilityItem.getItemMeta();
            if (meta != null) {
                if (unlocked) {
                    meta.setDisplayName("§a⚔ " + ability.getName());
                } else {
                    meta.setDisplayName("§c✗ " + ability.getName() + " §7(Bloqueada)");
                }

                List<String> lore = new ArrayList<>();
                lore.add("§7" + ability.getDescription());
                lore.add("");
                lore.add("§7Coste de maná: " + (hasEnoughMana ? "§9" : "§c") + ability.getManaCost());
                lore.add("§7Maná actual: §9" + currentMana);
                lore.add("§7Nivel requerido: §f" + ability.getRequiredLevel());

                if (!ability.getRequiredWeapon().isEmpty()) {
                    String weaponName = translateWeaponName(ability.getRequiredWeapon());
                    lore.add("§7Arma requerida: §e" + weaponName);
                }

                if (ability.getRange() > 0) {
                    lore.add("§7Rango: §f" + ability.getRange() + "m");
                }
                if (ability.getCastTime() > 0) {
                    lore.add("§7Tiempo de casteo: §f" + ability.getCastTime() + "s");
                }
                if (ability.getCooldown() > 0) {
                    lore.add("§7Cooldown: §c" + ability.getCooldown() + "s");
                }
                if (ability.getArmorPenetration() > 0) {
                    lore.add("§7Penetración de armadura: §6" + (int)(ability.getArmorPenetration() * 100) + "%");
                }

                lore.add("");
                if (unlocked) {
                    if (hasEnoughMana) {
                        lore.add("§e⚡ Click §7▸ Asignar al slot actual");
                        lore.add("§7Una habilidad por slot (1-8).");
                        if (!ability.getRequiredWeapon().isEmpty()) {
                            lore.add("§7Requiere §e" + translateWeaponName(ability.getRequiredWeapon()) + " §7en slot §f1§7.");
                        }
                        lore.add("");
                        lore.add("§d⚔ Doble-tap §fF §dpara modo habilidades");
                        lore.add("§7Luego pulsa §f1-8 §7para lanzar.");
                        if ("static".equalsIgnoreCase(ability.getCastMode())) {
                            lore.add("§c⚠ Requiere estar quieto");
                        } else {
                            lore.add("§a✓ Puedes moverte");
                        }
                    } else {
                        lore.add("§c✗ Maná insuficiente");
                    }
                } else {
                    lore.add("§c✗ Nivel " + ability.getRequiredLevel() + " requerido");
                }

                meta.setLore(lore);
                if (unlocked && ability.getCustomModelData() > 0) {
                    meta.setCustomModelData(ability.getCustomModelData());
                }
                abilityItem.setItemMeta(meta);
            }
            menu.setItem(slot, abilityItem);
            slot++;
        }

        player.openInventory(menu);
    }
}
