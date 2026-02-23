package com.irdem.tunama.menus.clan;

import com.irdem.tunama.TunamaRPG;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Menú para jugadores sin clan
 */
public class ClanNoClanMenu implements InventoryHolder {
    private final TunamaRPG plugin;
    private Inventory inventory;

    public ClanNoClanMenu(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        inventory = Bukkit.createInventory(this, 27, "§6§lCLANES");

        // Botón: Crear Clan (slot 11)
        ItemStack createClan = new ItemStack(Material.EMERALD);
        ItemMeta createMeta = createClan.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName("§a§lCrear Clan");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Crea tu propio clan y lidera");
            lore.add("§7a tu grupo de jugadores");
            lore.add("");
            lore.add("§e» Click para crear un clan");
            createMeta.setLore(lore);
            createClan.setItemMeta(createMeta);
        }
        inventory.setItem(11, createClan);

        // Botón: Top 20 Clanes (slot 15)
        ItemStack topClans = new ItemStack(Material.DIAMOND);
        ItemMeta topMeta = topClans.getItemMeta();
        if (topMeta != null) {
            topMeta.setDisplayName("§6§lTop 20 Clanes");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Ve los mejores clanes del servidor");
            lore.add("§7ordenados por oro y kills de PvP");
            lore.add("");
            lore.add("§e» Click para ver el ranking");
            topMeta.setLore(lore);
            topClans.setItemMeta(topMeta);
        }
        inventory.setItem(15, topClans);

        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
