package com.irdem.tunama.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.Race;
import java.util.ArrayList;
import java.util.List;

public class RaceMenu implements InventoryHolder {

    private TunamaRPG plugin;
    private Inventory inventory;

    public RaceMenu(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        inventory = Bukkit.createInventory(this, 45, "§6Selecciona tu Raza");
        
        int slot = 0;
        for (Race race : plugin.getRaceManager().getAllRaces().values()) {
            inventory.setItem(slot, createRaceItem(race));
            slot++;
            if (slot >= 45) break;
        }
        
        player.openInventory(inventory);
    }

    private ItemStack createRaceItem(Race race) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§6" + race.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7" + race.getDescription());
            lore.add("");
            lore.add("§aVentajas:");
            lore.add("§f" + race.getAdvantages());
            lore.add("");
            lore.add("§cDesventajas:");
            lore.add("§f" + race.getDisadvantages());
            lore.add("");
            lore.add("§e→ Click para seleccionar");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
