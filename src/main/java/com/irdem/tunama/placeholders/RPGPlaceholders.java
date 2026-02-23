package com.irdem.tunama.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.PlayerData;

/**
 * Integración con PlaceholderAPI para variables del plugin RPG.
 *
 * Variables disponibles:
 * - %rpg_raza% - Muestra la raza del jugador
 * - %rpg_clase% - Muestra la clase del jugador
 * - %rpg_subclase% - Muestra la subclase del jugador
 * - %rpg_clan% - Muestra el clan del jugador
 * - %rpg_nivel% - Muestra el nivel del jugador
 * - %rpg_experiencia% - Muestra la experiencia del jugador
 */
public class RPGPlaceholders extends PlaceholderExpansion {

    private final TunamaRPG plugin;

    public RPGPlaceholders(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "rpg";
    }

    @Override
    public String getAuthor() {
        return "Irdem";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getRequiredPlugin() {
        return null;
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (player == null) {
            return "";
        }

        // Para clan_tag y clantag: no requerir que el jugador esté online
        // ni que tenga datos RPG - solo buscar si tiene clan
        String id = identifier.toLowerCase();
        if (id.equals("clan_tag") || id.equals("clantag")) {
            try {
                com.irdem.tunama.data.Clan clanForTag = plugin.getClanManager().getPlayerClan(player.getUniqueId());
                if (clanForTag != null) {
                    return clanForTag.getFormattedTag() + " ";
                }
                return "";
            } catch (Exception e) {
                plugin.getLogger().warning("Error al obtener clan_tag para " + player.getName() + ": " + e.getMessage());
                return "";
            }
        }

        // Para el resto de placeholders, el jugador debe estar online
        if (!player.isOnline()) {
            return "";
        }

        PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return "";
        }

        try {
            switch (id) {
                case "raza":
                    if (playerData.getRace() == null || playerData.getRace().isEmpty()) return "Sin asignar";
                    com.irdem.tunama.data.Race r = plugin.getRaceManager().getRace(playerData.getRace().toLowerCase());
                    return r != null ? r.getName() : playerData.getRace();

                case "clase":
                    // Si tiene subclase, mostrar la subclase en vez de la clase
                    if (playerData.getSubclass() != null && !playerData.getSubclass().isEmpty()) {
                        com.irdem.tunama.data.Subclass subForClass = plugin.getSubclassManager().getSubclass(playerData.getSubclass().toLowerCase());
                        return subForClass != null ? subForClass.getName() : playerData.getSubclass();
                    }
                    if (playerData.getPlayerClass() == null || playerData.getPlayerClass().isEmpty()) return "Sin asignar";
                    com.irdem.tunama.data.RPGClass c = plugin.getClassManager().getClass(playerData.getPlayerClass().toLowerCase());
                    return c != null ? c.getName() : playerData.getPlayerClass();

                case "clan":
                    com.irdem.tunama.data.Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
                    return clan != null ? clan.getName() : "Sin clan";

                case "nivel":
                case "level":
                    return String.valueOf(playerData.getLevel());

                case "experiencia":
                case "exp":
                case "experience":
                    return String.valueOf(playerData.getExperience());

                case "subclase":
                case "subclass":
                    if (playerData.getSubclass() == null || playerData.getSubclass().isEmpty()) return "Sin asignar";
                    com.irdem.tunama.data.Subclass sub = plugin.getSubclassManager().getSubclass(playerData.getSubclass().toLowerCase());
                    return sub != null ? sub.getName() : playerData.getSubclass();

                case "vida":
                case "health":
                    return String.valueOf(playerData.getStats().getHealth());

                case "fuerza":
                case "strength":
                    return String.valueOf(playerData.getStats().getStrength());

                case "agilidad":
                case "agility":
                    return String.valueOf(playerData.getStats().getAgility());

                case "inteligencia":
                case "intelligence":
                    return String.valueOf(playerData.getStats().getIntelligence());

                case "nombre":
                case "name":
                case "personaje":
                case "character":
                    return playerData.getUsername() != null ? playerData.getUsername() : player.getName();

                default:
                    return null;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error en placeholder rpg_" + identifier + ": " + e.getMessage());
            return "";
        }
    }
}
