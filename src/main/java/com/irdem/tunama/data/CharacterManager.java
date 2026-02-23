package com.irdem.tunama.data;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CharacterManager {

    private JavaPlugin plugin;
    private Connection connection;

    public CharacterManager(JavaPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * Obtiene el número máximo de personajes permitidos para un jugador basado en permisos
     */
    public int getMaxCharacters(Player player) {
        if (player.hasPermission("rpg.characters.20")) return 20;
        if (player.hasPermission("rpg.characters.10")) return 10;
        if (player.hasPermission("rpg.characters.5")) return 5;
        if (player.hasPermission("rpg.characters.3")) return 3;
        return 1; // Por defecto todos tienen rpg.characters.1
    }

    /**
     * Obtiene el slot activo del jugador
     */
    public int getActiveSlot(UUID uuid) {
        try {
            String query = "SELECT active_slot FROM active_characters WHERE uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("active_slot");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al obtener slot activo: " + e.getMessage());
        }
        return 1; // Por defecto slot 1
    }

    /**
     * Establece el slot activo del jugador
     */
    public void setActiveSlot(UUID uuid, int slot) {
        try {
            String query = "INSERT OR REPLACE INTO active_characters (uuid, active_slot, last_updated) " +
                          "VALUES (?, ?, CURRENT_TIMESTAMP)";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                stmt.setInt(2, slot);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            // Intentar sintaxis MySQL si falla SQLite
            try {
                String query = "INSERT INTO active_characters (uuid, active_slot) " +
                              "VALUES (?, ?) " +
                              "ON DUPLICATE KEY UPDATE active_slot = ?, last_updated = CURRENT_TIMESTAMP";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setInt(2, slot);
                    stmt.setInt(3, slot);
                    stmt.executeUpdate();
                }
            } catch (SQLException e2) {
                plugin.getLogger().severe("Error al establecer slot activo: " + e2.getMessage());
            }
        }
    }

    /**
     * Lista todos los personajes de un jugador
     */
    public List<CharacterInfo> getCharacters(UUID uuid) {
        List<CharacterInfo> characters = new ArrayList<>();
        try {
            String query = "SELECT character_slot, username, race, class, level, experience " +
                          "FROM players WHERE uuid = ? ORDER BY character_slot";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        CharacterInfo info = new CharacterInfo();
                        info.slot = rs.getInt("character_slot");
                        info.username = rs.getString("username");
                        info.race = rs.getString("race");
                        info.playerClass = rs.getString("class");
                        info.level = rs.getInt("level");
                        info.experience = rs.getLong("experience");
                        characters.add(info);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al listar personajes: " + e.getMessage());
        }
        return characters;
    }

    /**
     * Encuentra el primer slot vacío para crear un nuevo personaje
     */
    public int getFirstEmptySlot(UUID uuid, int maxSlots) {
        List<CharacterInfo> characters = getCharacters(uuid);
        for (int i = 1; i <= maxSlots; i++) {
            final int slot = i;
            boolean exists = characters.stream().anyMatch(c -> c.slot == slot);
            if (!exists) {
                return i;
            }
        }
        return -1; // No hay slots disponibles
    }

    /**
     * Verifica si un personaje existe en un slot específico
     */
    public boolean characterExists(UUID uuid, int slot) {
        try {
            String query = "SELECT COUNT(*) as count FROM players WHERE uuid = ? AND character_slot = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                stmt.setInt(2, slot);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("count") > 0;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al verificar personaje: " + e.getMessage());
        }
        return false;
    }

    /**
     * Elimina un personaje (comando renacer)
     */
    public boolean deleteCharacter(UUID uuid, int slot) {
        try {
            // Eliminar estadísticas del personaje
            String deleteStatsQuery = "DELETE FROM player_stats WHERE player_uuid = ?";
            String compositeUuid = uuid.toString() + "_" + slot;
            try (PreparedStatement stmt = connection.prepareStatement(deleteStatsQuery)) {
                stmt.setString(1, compositeUuid);
                stmt.executeUpdate();
            }

            // Eliminar personaje
            String deletePlayerQuery = "DELETE FROM players WHERE uuid = ? AND character_slot = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deletePlayerQuery)) {
                stmt.setString(1, uuid.toString());
                stmt.setInt(2, slot);
                int deleted = stmt.executeUpdate();

                if (deleted > 0) {
                    plugin.getLogger().info("Personaje eliminado: " + uuid + " slot " + slot);
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al eliminar personaje: " + e.getMessage());
        }
        return false;
    }

    /**
     * Clase interna para información de personaje
     */
    public static class CharacterInfo {
        public int slot;
        public String username;
        public String race;
        public String playerClass;
        public int level;
        public long experience;

        public boolean isEmpty() {
            return race == null || race.isEmpty() || playerClass == null || playerClass.isEmpty();
        }
    }
}
