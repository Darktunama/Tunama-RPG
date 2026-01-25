package com.irdem.tunama.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.PlayerData;
import java.util.ArrayList;
import java.util.List;

public class EquipmentListener implements Listener {

    private TunamaRPG plugin;

    public EquipmentListener(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Obtener o crear datos del jugador
        PlayerData playerData = plugin.getDatabaseManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        
        if (playerData != null) {
            // Actualizar los slots de equipo del jugador
            updatePlayerEquipmentSlots(player, playerData);
        }
    }

    private void updatePlayerEquipmentSlots(Player player, PlayerData playerData) {
        // Los slots 36-38 son para Boots, Leggings, Chestplate
        // Los slots 39 son para Helmet
        // Podemos usar slots adicionales en el inventario normal para mostrar equipo
        
        // Para este caso, vamos a marcar el inventario del jugador visualmente
        // Slot 0: Anillo 1
        // Slot 1: Anillo 2
        // Slot 2: Collar
        // Slot 3: Anillo 3
        // Slot 4: Anillo 4
        // Slot 5: Amuleto 1
        // Slot 6: Amuleto 2

        Inventory inventory = player.getInventory();
        
        // Anillo 1
        if (playerData.getRing1() != null && !playerData.getRing1().isEmpty()) {
            inventory.setItem(0, createEquipmentItem(Material.PAPER, "§6Anillo 1", playerData.getRing1()));
        }
        
        // Anillo 2
        if (playerData.getRing2() != null && !playerData.getRing2().isEmpty()) {
            inventory.setItem(1, createEquipmentItem(Material.PAPER, "§6Anillo 2", playerData.getRing2()));
        }
        
        // Collar
        if (playerData.getNecklace() != null && !playerData.getNecklace().isEmpty()) {
            inventory.setItem(2, createEquipmentItem(Material.AMETHYST_SHARD, "§bCollar", playerData.getNecklace()));
        }
        
        // Anillo 3
        if (playerData.getRing3() != null && !playerData.getRing3().isEmpty()) {
            inventory.setItem(3, createEquipmentItem(Material.PAPER, "§6Anillo 3", playerData.getRing3()));
        }
        
        // Anillo 4
        if (playerData.getRing4() != null && !playerData.getRing4().isEmpty()) {
            inventory.setItem(4, createEquipmentItem(Material.PAPER, "§6Anillo 4", playerData.getRing4()));
        }
        
        // Amuleto 1
        if (playerData.getAmulet1() != null && !playerData.getAmulet1().isEmpty()) {
            inventory.setItem(5, createEquipmentItem(Material.EMERALD, "§2Amuleto 1", playerData.getAmulet1()));
        }
        
        // Amuleto 2
        if (playerData.getAmulet2() != null && !playerData.getAmulet2().isEmpty()) {
            inventory.setItem(6, createEquipmentItem(Material.EMERALD, "§2Amuleto 2", playerData.getAmulet2()));
        }
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
}
