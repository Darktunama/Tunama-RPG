package com.irdem.tunama.listeners;

import org.bukkit.event.Listener;
import com.irdem.tunama.TunamaRPG;

/**
 * Listener para eventos relacionados con el equipo del jugador.
 *
 * Nota: La funcionalidad de mostrar equipo se maneja a través del menú de equipo (/equipo)
 * en lugar de modificar directamente el inventario del jugador para evitar interferencias
 * con items normales.
 */
public class EquipmentListener implements Listener {

    private TunamaRPG plugin;

    public EquipmentListener(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    // Los eventos relacionados con equipo se manejan a través del EquipmentMenu
    // y EquipmentMenuListener, no aquí
}
