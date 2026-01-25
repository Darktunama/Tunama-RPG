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
        // Verificar si es la primera vez que el jugador inicia sesión
        if (plugin.getDatabaseManager().isNewPlayer(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage("§a¡Bienvenido a TunamaRPG!");
            event.getPlayer().sendMessage("§6Por favor, selecciona tu raza en el menú que se abrirá...");
            
            // Abrir menú de razas
            RaceMenu raceMenu = new RaceMenu(plugin);
            raceMenu.open(event.getPlayer());
        } else {
            event.getPlayer().sendMessage("§a¡Bienvenido de vuelta a TunamaRPG!");
            event.getPlayer().sendMessage("§7Usa /rpg help para ver los comandos disponibles");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Aquí se pueden guardar datos del jugador en la BD
    }
}
