package com.irdem.tunama.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.PlayerData;

/**
 * Personaliza el inventario del jugador (E) para mostrar slots de equipo adicionales.
 */
public class PlayerInventoryListener implements Listener {
    private final TunamaRPG plugin;

    public PlayerInventoryListener(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        // Solo si es el inventario del jugador (tipo PLAYER)
        if (!event.getInventory().getType().equals(InventoryType.PLAYER)) {
            return;
        }
        
        // Obtener datos del jugador
        PlayerData playerData = plugin.getDatabaseManager()
            .getOrCreatePlayerData(player.getUniqueId(), player.getName());
        if (playerData == null) return;

        // Mostrar información de equipo en el chat (workaround visual)
        // Nota: Spigot 1.20.1 no permite modificar directamente la UI vanilla
        // Se muestra información en lugar de items reales
        showEquipmentInfo(player, playerData);
    }

    private void showEquipmentInfo(Player player, PlayerData playerData) {
        // Mostrar una representación visual en el chat
        player.sendMessage("§7═══════════════════════════════════════");
        player.sendMessage("§6⚔ EQUIPO ACTUAL:");
        player.sendMessage("§7───────────────────────────────────────");
        player.sendMessage("§e● Anillo 1: §f" + getEquipmentName(playerData.getRing1()));
        player.sendMessage("§e● Anillo 2: §f" + getEquipmentName(playerData.getRing2()));
        player.sendMessage("§e● Anillo 3: §f" + getEquipmentName(playerData.getRing3()));
        player.sendMessage("§e● Anillo 4: §f" + getEquipmentName(playerData.getRing4()));
        player.sendMessage("§c● Collar: §f" + getEquipmentName(playerData.getNecklace()));
        player.sendMessage("§d● Amuleto 1: §f" + getEquipmentName(playerData.getAmulet1()));
        player.sendMessage("§d● Amuleto 2: §f" + getEquipmentName(playerData.getAmulet2()));
        player.sendMessage("§7═══════════════════════════════════════");
        player.sendMessage("§7Escribe §f/rpg equipo §7para cambiar tu equipo");
    }

    private String getEquipmentName(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return "§8Sin equipar";
        }
        return itemId;
    }
}
