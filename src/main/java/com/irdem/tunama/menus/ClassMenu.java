package com.irdem.tunama.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.RPGClass;
import com.irdem.tunama.data.Subclass;
import java.util.ArrayList;
import java.util.List;

public class ClassMenu implements InventoryHolder {

    private TunamaRPG plugin;
    private String selectedRace;
    private Inventory inventory;

    public ClassMenu(TunamaRPG plugin, String selectedRace) {
        this.plugin = plugin;
        this.selectedRace = selectedRace;
    }

    public void open(Player player) {
        inventory = Bukkit.createInventory(this, 45, "§6Selecciona tu Clase");

        // Obtener la raza para verificar restricciones
        com.irdem.tunama.data.Race race = plugin.getRaceManager().getRace(selectedRace.toLowerCase());

        int slot = 0;
        for (RPGClass rpgClass : plugin.getClassManager().getAllClasses().values()) {
            // Verificar si esta clase está restringida para la raza seleccionada
            if (race != null && race.isClassRestricted(rpgClass.getId())) {
                // Clase restringida, NO mostrar
                continue;
            }

            // Clase disponible
            inventory.setItem(slot, createClassItem(rpgClass));
            slot++;
            if (slot >= 45) break;
        }

        player.openInventory(inventory);
    }

    private ItemStack createClassItem(RPGClass rpgClass) {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§6" + rpgClass.getName());

            List<String> lore = new ArrayList<>();
            lore.add("§7" + rpgClass.getDescription());
            lore.add("");
            lore.add("§aVentajas:");
            lore.add("§f" + rpgClass.getAdvantages());
            lore.add("");
            lore.add("§cDesventajas:");
            lore.add("§f" + rpgClass.getDisadvantages());
            lore.add("");
            List<String> subclassNames = new ArrayList<>();
            for (String subId : rpgClass.getSubclasses()) {
                Subclass sub = plugin.getSubclassManager().getSubclass(subId);
                subclassNames.add(sub != null ? sub.getName() : subId);
            }
            lore.add("§9Subclases: §f" + String.join(", ", subclassNames));
            lore.add("");
            lore.add("§e→ Click para seleccionar");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    public String getSelectedRace() {
        return selectedRace;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
