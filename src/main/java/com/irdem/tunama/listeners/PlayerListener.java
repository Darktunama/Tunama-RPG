package com.irdem.tunama.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.menus.RaceMenu;

public class PlayerListener implements Listener {

    private TunamaRPG plugin;

    public PlayerListener(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();

        // Crear las barras del jugador
        plugin.getPlayerBarsManager().createBars(player);

        // Crear scoreboard del jugador
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().createScoreboard(player);
        }

        // Verificar si es la primera vez que el jugador inicia sesión
        if (plugin.getDatabaseManager().isNewPlayer(player.getUniqueId())) {
            player.sendMessage("§a¡Bienvenido a TunamaRPG!");
            player.sendMessage("§6Por favor, selecciona tu raza en el menú que se abrirá...");

            // Abrir menú de razas
            RaceMenu raceMenu = new RaceMenu(plugin);
            raceMenu.open(player);
        } else {
            player.sendMessage("§a¡Bienvenido de vuelta a TunamaRPG!");
            player.sendMessage("§7Usa /rpg help para ver los comandos disponibles");

            // Cargar datos del personaje activo
            com.irdem.tunama.data.PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
            if (playerData != null && playerData.getUsername() != null && !playerData.getUsername().isEmpty()) {
                // Cambiar el display name al nombre del personaje
                player.displayName(net.kyori.adventure.text.Component.text(playerData.getUsername()));
                player.playerListName(net.kyori.adventure.text.Component.text(playerData.getUsername()));

                // Actualizar la vida máxima del jugador basándose en sus estadísticas
                updatePlayerMaxHealth(player, playerData);
            }
        }
    }

    /**
     * Restaura la vida de Minecraft a su valor por defecto (20 = 10 corazones).
     * La vida RPG se gestiona internamente y se muestra en la barra de boss.
     * No modificamos MAX_HEALTH para evitar llenar la pantalla de corazones.
     */
    private void updatePlayerMaxHealth(org.bukkit.entity.Player player, com.irdem.tunama.data.PlayerData playerData) {
        // Mantener la vida de Minecraft en 20 (10 corazones, valor vanilla)
        org.bukkit.attribute.AttributeInstance maxHealthAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(20);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Remover las barras del jugador
        plugin.getPlayerBarsManager().removeBars(event.getPlayer());

        // Remover scoreboard del jugador
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().removeScoreboard(event.getPlayer());
            plugin.getScoreboardManager().cleanupPlayer(event.getPlayer());
        }
    }
}
