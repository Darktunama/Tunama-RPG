package com.irdem.tunama.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.irdem.tunama.TunamaRPG;

public class PlayerListener implements Listener {

    private TunamaRPG plugin;

    public PlayerListener(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage("§a¡Bienvenido a TunamaRPG!");
        event.getPlayer().sendMessage("§7Usa /rpg help para ver los comandos disponibles");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Aquí se pueden guardar datos del jugador en la BD
    }
}
