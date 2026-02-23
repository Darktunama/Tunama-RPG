package com.irdem.tunama.listeners;

import com.irdem.tunama.TunamaRPG;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener para manejar la entrada de nombres de personajes a través del yunque
 */
public class AnvilInputListener implements Listener {

    private final TunamaRPG plugin;

    // Mapa para rastrear qué jugadores están en proceso de creación de personaje
    private final Map<UUID, PendingCharacter> pendingCharacters;

    public AnvilInputListener(TunamaRPG plugin) {
        this.plugin = plugin;
        this.pendingCharacters = new HashMap<>();
    }

    /**
     * Registra que un jugador está esperando ingresar un nombre de personaje
     */
    public void registerPendingCharacter(Player player, String raceId, String classId) {
        pendingCharacters.put(player.getUniqueId(), new PendingCharacter(raceId, classId));
    }

    @EventHandler
    public void onAnvilClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Verificar si el jugador tiene un personaje pendiente
        if (!pendingCharacters.containsKey(player.getUniqueId())) {
            return;
        }

        // Verificar si es un yunque por tipo
        if (event.getInventory().getType() != InventoryType.ANVIL) {
            return;
        }

        // Solo procesar clicks en el slot de resultado (slot 2)
        if (event.getRawSlot() != 2) {
            return;
        }

        event.setCancelled(true);

        // Obtener el texto de renombre del yunque usando AnvilView
        String characterName = null;
        if (event.getView() instanceof AnvilView) {
            AnvilView anvilView = (AnvilView) event.getView();
            characterName = anvilView.getRenameText();
        }

        if (characterName == null || characterName.trim().isEmpty()) {
            player.sendMessage("§c✗ Debes ingresar un nombre para el personaje");
            return;
        }

        characterName = characterName.trim();

        // Validar el nombre
        if (!plugin.getBannedNamesManager().isNameValid(characterName)) {
            String errorMsg = plugin.getBannedNamesManager().getErrorMessage(characterName);
            player.sendMessage(errorMsg);
            return;
        }

        // Nombre válido, crear el personaje
        PendingCharacter pending = pendingCharacters.get(player.getUniqueId());

        player.closeInventory();

        // Guardar el personaje con el nombre elegido
        plugin.getDatabaseManager().saveNewPlayer(
            player.getUniqueId(),
            characterName,
            pending.raceId,
            pending.classId
        );

        // Cambiar el display name del jugador
        player.displayName(net.kyori.adventure.text.Component.text(characterName));
        player.playerListName(net.kyori.adventure.text.Component.text(characterName));

        player.sendMessage("§a✓ Personaje creado exitosamente: §f" + characterName);
        player.sendMessage("§e¡Bienvenido a TunamaRPG, " + characterName + "!");

        // Limpiar el registro
        pendingCharacters.remove(player.getUniqueId());
    }

    @EventHandler
    public void onAnvilClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        // Verificar si el jugador tenía un personaje pendiente y cerró el yunque sin completar
        if (pendingCharacters.containsKey(player.getUniqueId())) {
            // Verificar si es un yunque
            if (event.getInventory().getType() == InventoryType.ANVIL) {
                String name = null;
                if (event.getView() instanceof AnvilView) {
                    AnvilView anvilView = (AnvilView) event.getView();
                    name = anvilView.getRenameText();
                }

                // Si no hay nombre válido, cancelar la creación
                if (name == null || name.trim().isEmpty() || !plugin.getBannedNamesManager().isNameValid(name.trim())) {
                    player.sendMessage("§c✗ Creación de personaje cancelada");
                    player.sendMessage("§7No se ingresó un nombre válido");

                    // Limpiar el registro
                    pendingCharacters.remove(player.getUniqueId());

                    // Volver al menú de personajes después de un breve delay
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        com.irdem.tunama.menus.CharacterSelectionMenu menu = new com.irdem.tunama.menus.CharacterSelectionMenu(plugin);
                        menu.open(player);
                    }, 10L);
                }
            }
        }
    }

    /**
     * Clase interna para almacenar información de personajes pendientes
     */
    private static class PendingCharacter {
        final String raceId;
        final String classId;

        PendingCharacter(String raceId, String classId) {
            this.raceId = raceId;
            this.classId = classId;
        }
    }
}
