package com.irdem.tunama.listeners;

import com.irdem.tunama.TunamaRPG;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Listener que bloquea acciones normales mientras el jugador está transformado.
 * Las transformaciones de druida solo permiten usar habilidades de la forma animal.
 */
public class TransformationListener implements Listener {

    private final TunamaRPG plugin;

    public TransformationListener(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    /**
     * Bloquea romper bloques mientras está transformado
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (plugin.getTransformationManager().isTransformed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§c✗ No puedes romper bloques mientras estás transformado");
        }
    }

    /**
     * Bloquea colocar bloques mientras está transformado
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.getTransformationManager().isTransformed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§c✗ No puedes colocar bloques mientras estás transformado");
        }
    }

    /**
     * Bloquea tirar items mientras está transformado
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (plugin.getTransformationManager().isTransformed(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Bloquea cambiar items entre manos mientras está transformado
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (plugin.getTransformationManager().isTransformed(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Bloquea abrir inventarios externos mientras está transformado
     * Permite abrir el inventario del jugador (para ver habilidades)
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        if (plugin.getTransformationManager().isTransformed(player.getUniqueId())) {
            String typeName = event.getInventory().getType().name();

            // Permitir inventario del jugador y crafting (menús del plugin)
            if (typeName.equals("PLAYER") || typeName.equals("CRAFTING")) {
                return;
            }

            // Bloquear inventarios de bloques del mundo
            if (typeName.equals("CHEST") ||
                typeName.equals("FURNACE") ||
                typeName.equals("WORKBENCH") ||
                typeName.equals("ENCHANTING") ||
                typeName.equals("ANVIL") ||
                typeName.equals("BREWING") ||
                typeName.equals("HOPPER") ||
                typeName.equals("DISPENSER") ||
                typeName.equals("DROPPER") ||
                typeName.equals("SHULKER_BOX") ||
                typeName.equals("BARREL") ||
                typeName.equals("BLAST_FURNACE") ||
                typeName.equals("SMOKER") ||
                typeName.equals("STONECUTTER") ||
                typeName.equals("GRINDSTONE") ||
                typeName.equals("LOOM") ||
                typeName.equals("CARTOGRAPHY") ||
                typeName.equals("SMITHING")) {
                event.setCancelled(true);
                player.sendMessage("§c✗ No puedes usar esto mientras estás transformado");
            }
        }
    }

    /**
     * Bloquea interactuar con ciertos bloques mientras está transformado
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getTransformationManager().isTransformed(player.getUniqueId())) {
            return;
        }

        // Permitir click derecho (para usar habilidades en la hotbar)
        if (event.getAction().name().contains("RIGHT")) {
            return;
        }

        // Bloquear interacción con bloques interactuables
        if (event.getClickedBlock() != null) {
            String blockType = event.getClickedBlock().getType().name();

            // Bloquear puertas, botones, palancas, etc.
            if (blockType.contains("DOOR") ||
                blockType.contains("GATE") ||
                blockType.contains("BUTTON") ||
                blockType.contains("LEVER") ||
                blockType.contains("PRESSURE_PLATE") ||
                blockType.contains("TRAPDOOR")) {
                // Permitir estas interacciones básicas
                return;
            }

            // Bloquear cofres, hornos, mesas de crafteo, etc.
            if (blockType.contains("CHEST") ||
                blockType.contains("FURNACE") ||
                blockType.contains("CRAFTING") ||
                blockType.contains("ANVIL") ||
                blockType.contains("ENCHANT") ||
                blockType.contains("BREWING") ||
                blockType.contains("BARREL") ||
                blockType.contains("HOPPER") ||
                blockType.contains("DISPENSER") ||
                blockType.contains("DROPPER") ||
                blockType.contains("SHULKER")) {
                event.setCancelled(true);
            }
        }
    }
}
