package com.irdem.tunama.listeners;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.Clan;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listener que añade el tag del clan al chat con hover info.
 * Funciona directamente con el sistema de chat de Paper (Adventure API).
 */
public class ClanChatListener implements Listener {

    private final TunamaRPG plugin;

    public ClanChatListener(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            return;
        }

        // Construir el hover text con la información del clan
        Component hoverText = buildClanHoverInfo(clan);

        // Crear el tag del clan como componente con hover
        Component tagComponent = LegacyComponentSerializer.legacySection()
                .deserialize(clan.getFormattedTag())
                .hoverEvent(HoverEvent.showText(hoverText));

        // Modificar el renderer para anteponer el tag al nombre del jugador
        event.renderer((source, sourceDisplayName, message, audience) ->
                Component.empty()
                        .append(tagComponent)
                        .append(Component.text(" "))
                        .append(sourceDisplayName)
                        .append(Component.text(": "))
                        .append(message)
        );
    }

    /**
     * Construye el componente de hover con la información del clan usando Adventure API pura
     */
    private Component buildClanHoverInfo(Clan clan) {
        // Obtener el nombre del líder
        String leaderName = "Desconocido";
        try {
            OfflinePlayer leader = Bukkit.getOfflinePlayer(clan.getLeaderId());
            if (leader.getName() != null) {
                leaderName = leader.getName();
            }
        } catch (Exception e) {
            // Mantener "Desconocido"
        }

        // Construir hover text usando Adventure API pura (sin LegacyComponentSerializer)
        Component hover = Component.text(clan.getName() + " ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(LegacyComponentSerializer.legacySection().deserialize(clan.getFormattedTag()))
                .append(Component.newline())
                .append(Component.text("━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(Component.text("Líder: ", NamedTextColor.GRAY))
                .append(Component.text(leaderName, NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("Miembros: ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(clan.getMemberCount()), NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("Oro: ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(clan.getGold()), NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("PvP Kills: ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(clan.getPvpKills()), NamedTextColor.RED));

        if (!clan.getAllies().isEmpty()) {
            hover = hover.append(Component.newline())
                    .append(Component.text("Aliados: ", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(clan.getAllies().size()), NamedTextColor.GREEN));
        }

        if (!clan.getEnemies().isEmpty()) {
            hover = hover.append(Component.newline())
                    .append(Component.text("Enemigos: ", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(clan.getEnemies().size()), NamedTextColor.RED));
        }

        return hover;
    }
}
