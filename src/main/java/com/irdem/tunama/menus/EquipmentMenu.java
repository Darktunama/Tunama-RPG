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

public class EquipmentMenu implements InventoryHolder {
    private Inventory inventory;
    private TunamaRPG plugin;
    private Player player;
    private PlayerData playerData;

    public EquipmentMenu(TunamaRPG plugin, Player player, PlayerData playerData) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = playerData;
        this.inventory = Bukkit.createInventory(this, 27, "§6Equipo del Jugador");
        setupItems();
    }

    private void setupItems() {
        // Anillo 1 (slot 10)
        inventory.setItem(10, createEquipmentItem(Material.PAPER, "§6Anillo 1", playerData.getRing1()));
        
        // Anillo 2 (slot 11)
        inventory.setItem(11, createEquipmentItem(Material.PAPER, "§6Anillo 2", playerData.getRing2()));
        
        // Collar (slot 12)
        inventory.setItem(12, createEquipmentItem(Material.AMETHYST_SHARD, "§bCollar", playerData.getNecklace()));
        
        // Anillo 3 (slot 13)
        inventory.setItem(13, createEquipmentItem(Material.PAPER, "§6Anillo 3", playerData.getRing3()));
        
        // Anillo 4 (slot 14)
        inventory.setItem(14, createEquipmentItem(Material.PAPER, "§6Anillo 4", playerData.getRing4()));
        
        // Amuleto 1 (slot 16)
        inventory.setItem(16, createEquipmentItem(Material.EMERALD, "§2Amuleto 1", playerData.getAmulet1()));
        
        // Amuleto 2 (slot 17)
        inventory.setItem(17, createEquipmentItem(Material.EMERALD, "§2Amuleto 2", playerData.getAmulet2()));

        // Volver (slot 22)
        inventory.setItem(22, createItem(Material.BARRIER, "§cVolver", 
            "§7Haz clic para volver al menú principal"
        ));
    }

    private ItemStack createEquipmentItem(Material material, String name, String equipped) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            if (equipped != null && !equipped.isEmpty()) {
                lore.add("§f" + equipped);
            } else {
                lore.add("§7Vacío");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
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
