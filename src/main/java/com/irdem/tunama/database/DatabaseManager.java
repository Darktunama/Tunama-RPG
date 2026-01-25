package com.irdem.tunama.database;

import org.bukkit.plugin.java.JavaPlugin;
import com.irdem.tunama.config.ConfigManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseManager {

    private JavaPlugin plugin;
    private ConfigManager configManager;
    private Connection connection;
    private String databaseType;

    public DatabaseManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.databaseType = configManager.getDatabaseType();
    }

    public boolean connect() {
        try {
            if (databaseType.equalsIgnoreCase("sqlite")) {
                return connectSQLite();
            } else if (databaseType.equalsIgnoreCase("mysql")) {
                return connectMySQL();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error al conectar a la BD: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private boolean connectSQLite() throws Exception {
        Class.forName("org.sqlite.JDBC");
        String path = configManager.getSQLitePath();
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        plugin.getLogger().info("Conectado a SQLite: " + path);
        createSQLiteTables();
        return true;
    }

    private boolean connectMySQL() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=%s",
            configManager.getMySQLHost(),
            configManager.getMySQLPort(),
            configManager.getMySQLDatabase(),
            configManager.isMySQLSSL()
        );
        connection = DriverManager.getConnection(
            url,
            configManager.getMySQLUsername(),
            configManager.getMySQLPassword()
        );
        plugin.getLogger().info("Conectado a MySQL: " + configManager.getMySQLHost());
        createMySQLTables();
        return true;
    }

    private void createSQLiteTables() {
        try (Statement stmt = connection.createStatement()) {
            // Tabla de Jugadores
            stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                "id INTEGER PRIMARY KEY," +
                "uuid TEXT UNIQUE NOT NULL," +
                "username TEXT NOT NULL," +
                "race TEXT NOT NULL," +
                "class TEXT NOT NULL," +
                "subclass TEXT," +
                "level INTEGER DEFAULT 1," +
                "experience LONG DEFAULT 0," +
                "clan_name TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")");

            // Migración: agregar clan_name si no existe
            try {
                stmt.execute("ALTER TABLE players ADD COLUMN clan_name TEXT");
                plugin.getLogger().info("Columna clan_name agregada a tabla players");
            } catch (Exception e) {
                // Columna ya existe, ignorar
            }

            // Tabla de Estadísticas
            stmt.execute("CREATE TABLE IF NOT EXISTS player_stats (" +
                "id INTEGER PRIMARY KEY," +
                "player_uuid TEXT UNIQUE NOT NULL," +
                "health INTEGER DEFAULT 100," +
                "strength INTEGER DEFAULT 10," +
                "agility INTEGER DEFAULT 10," +
                "intelligence INTEGER DEFAULT 10," +
                "sacred_power INTEGER DEFAULT 10," +
                "corrupt_power INTEGER DEFAULT 10," +
                "nature_power INTEGER DEFAULT 10," +
                "FOREIGN KEY(player_uuid) REFERENCES players(uuid)" +
            ")");

            // Tabla de Misiones
            stmt.execute("CREATE TABLE IF NOT EXISTS player_quests (" +
                "id INTEGER PRIMARY KEY," +
                "player_uuid TEXT NOT NULL," +
                "quest_id TEXT NOT NULL," +
                "completed INTEGER DEFAULT 0," +
                "completed_date TIMESTAMP," +
                "FOREIGN KEY(player_uuid) REFERENCES players(uuid)" +
            ")");

            // Tabla de Logros
            stmt.execute("CREATE TABLE IF NOT EXISTS player_achievements (" +
                "id INTEGER PRIMARY KEY," +
                "player_uuid TEXT NOT NULL," +
                "achievement_id TEXT NOT NULL," +
                "completed INTEGER DEFAULT 0," +
                "completed_date TIMESTAMP," +
                "FOREIGN KEY(player_uuid) REFERENCES players(uuid)" +
            ")");

            // Tabla de Clanes
            stmt.execute("CREATE TABLE IF NOT EXISTS clans (" +
                "id INTEGER PRIMARY KEY," +
                "clan_name TEXT UNIQUE NOT NULL," +
                "leader_uuid TEXT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY(leader_uuid) REFERENCES players(uuid)" +
            ")");

            // Tabla de Miembros de Clanes
            stmt.execute("CREATE TABLE IF NOT EXISTS clan_members (" +
                "id INTEGER PRIMARY KEY," +
                "clan_id INTEGER NOT NULL," +
                "player_uuid TEXT NOT NULL," +
                "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY(clan_id) REFERENCES clans(id)," +
                "FOREIGN KEY(player_uuid) REFERENCES players(uuid)" +
            ")");

            plugin.getLogger().info("Tablas de SQLite creadas/verificadas correctamente");
        } catch (Exception e) {
            plugin.getLogger().severe("Error al crear tablas SQLite: " + e.getMessage());
        }
    }

    private void createMySQLTables() {
        try (Statement stmt = connection.createStatement()) {
            // Tabla de Jugadores
            stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "uuid VARCHAR(36) UNIQUE NOT NULL," +
                "username VARCHAR(255) NOT NULL," +
                "race VARCHAR(50) NOT NULL," +
                "class VARCHAR(50) NOT NULL," +
                "subclass VARCHAR(50)," +
                "level INT DEFAULT 1," +
                "experience BIGINT DEFAULT 0," +
                "clan_name VARCHAR(255)," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")");

            // Migración: agregar clan_name si no existe
            try {
                stmt.execute("ALTER TABLE players ADD COLUMN clan_name VARCHAR(255)");
                plugin.getLogger().info("Columna clan_name agregada a tabla players (MySQL)");
            } catch (Exception e) {
                // Columna ya existe, ignorar
            }

            // Tabla de Estadísticas
            stmt.execute("CREATE TABLE IF NOT EXISTS player_stats (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid VARCHAR(36) UNIQUE NOT NULL," +
                "health INT DEFAULT 100," +
                "strength INT DEFAULT 10," +
                "agility INT DEFAULT 10," +
                "intelligence INT DEFAULT 10," +
                "sacred_power INT DEFAULT 10," +
                "corrupt_power INT DEFAULT 10," +
                "nature_power INT DEFAULT 10," +
                "FOREIGN KEY(player_uuid) REFERENCES players(uuid)" +
            ")");

            // Tabla de Misiones
            stmt.execute("CREATE TABLE IF NOT EXISTS player_quests (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "quest_id VARCHAR(50) NOT NULL," +
                "completed TINYINT DEFAULT 0," +
                "completed_date TIMESTAMP NULL," +
                "FOREIGN KEY(player_uuid) REFERENCES players(uuid)" +
            ")");

            // Tabla de Logros
            stmt.execute("CREATE TABLE IF NOT EXISTS player_achievements (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "achievement_id VARCHAR(50) NOT NULL," +
                "completed TINYINT DEFAULT 0," +
                "completed_date TIMESTAMP NULL," +
                "FOREIGN KEY(player_uuid) REFERENCES players(uuid)" +
            ")");

            // Tabla de Clanes
            stmt.execute("CREATE TABLE IF NOT EXISTS clans (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "clan_name VARCHAR(50) UNIQUE NOT NULL," +
                "leader_uuid VARCHAR(36) NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY(leader_uuid) REFERENCES players(uuid)" +
            ")");

            // Tabla de Miembros de Clanes
            stmt.execute("CREATE TABLE IF NOT EXISTS clan_members (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "clan_id INT NOT NULL," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY(clan_id) REFERENCES clans(id)," +
                "FOREIGN KEY(player_uuid) REFERENCES players(uuid)" +
            ")");

            plugin.getLogger().info("Tablas de MySQL creadas/verificadas correctamente");
        } catch (Exception e) {
            plugin.getLogger().severe("Error al crear tablas MySQL: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Desconectado de la base de datos");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error al desconectar: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isNewPlayer(java.util.UUID uuid) {
        try {
            String query = "SELECT COUNT(*) as count FROM players WHERE uuid = ?";
            java.sql.PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, uuid.toString());
            java.sql.ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") == 0;
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al verificar si es nuevo jugador: " + e.getMessage());
            return true;
        }
    }

    public void saveNewPlayer(java.util.UUID uuid, String username, String raceId, String classId) {
        try {
            String insertQuery = "INSERT INTO players (uuid, username, race, class) VALUES (?, ?, ?, ?)";
            java.sql.PreparedStatement stmt = connection.prepareStatement(insertQuery);
            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            stmt.setString(3, raceId);
            stmt.setString(4, classId);
            stmt.executeUpdate();
            
            // También insertar estadísticas por defecto
            String statsQuery = "INSERT INTO player_stats (player_uuid) VALUES (?)";
            java.sql.PreparedStatement statsStmt = connection.prepareStatement(statsQuery);
            statsStmt.setString(1, uuid.toString());
            statsStmt.executeUpdate();
            
            plugin.getLogger().info("Nuevo jugador guardado: " + username);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al guardar nuevo jugador: " + e.getMessage());
        }
    }

    public com.irdem.tunama.data.PlayerData getPlayerData(java.util.UUID uuid) {
        try {
            String query = "SELECT uuid, username, race, class, subclass, level, experience, clan_name " +
                          "FROM players WHERE uuid = ?";
            java.sql.PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, uuid.toString());
            java.sql.ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                com.irdem.tunama.data.PlayerData playerData = new com.irdem.tunama.data.PlayerData(
                    uuid, rs.getString("username")
                );
                playerData.setRace(rs.getString("race"));
                playerData.setPlayerClass(rs.getString("class"));
                playerData.setSubclass(rs.getString("subclass"));
                playerData.setLevel(rs.getInt("level"));
                playerData.addExperience(rs.getLong("experience"));
                playerData.setClanName(rs.getString("clan_name"));
                return playerData;
            }
            return null;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al obtener datos del jugador: " + e.getMessage());
            return null;
        }
    }

    public void updatePlayerData(com.irdem.tunama.data.PlayerData playerData) {
        try {
            String query = "UPDATE players SET race = ?, class = ?, subclass = ?, level = ?, " +
                          "experience = ?, clan_name = ? WHERE uuid = ?";
            java.sql.PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, playerData.getRace());
            stmt.setString(2, playerData.getPlayerClass());
            stmt.setString(3, playerData.getSubclass());
            stmt.setInt(4, playerData.getLevel());
            stmt.setLong(5, playerData.getExperience());
            stmt.setString(6, playerData.getClanName());
            stmt.setString(7, playerData.getUUID().toString());
            stmt.executeUpdate();
            
            plugin.getLogger().info("Datos del jugador actualizados: " + playerData.getUsername());
        } catch (Exception e) {
            plugin.getLogger().severe("Error al actualizar datos del jugador: " + e.getMessage());
        }
    }

    public void createPlayerIfNotExists(java.util.UUID uuid, String username) {
        try {
            String query;
            if (databaseType.equalsIgnoreCase("sqlite")) {
                query = "INSERT OR IGNORE INTO players (uuid, username, race, class, subclass, level, experience, clan_name) " +
                       "VALUES (?, ?, '', '', '', 1, 0, NULL)";
            } else {
                query = "INSERT INTO players (uuid, username, race, class, subclass, level, experience, clan_name) " +
                       "VALUES (?, ?, '', '', '', 1, 0, NULL) " +
                       "ON DUPLICATE KEY UPDATE uuid=uuid";
            }
            
            java.sql.PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                plugin.getLogger().info("Nuevo jugador creado: " + username);
            }
        } catch (Exception e) {
            // Ignorar si ya existe (puede ocurrir por race condition)
            if (!e.getMessage().contains("UNIQUE")) {
                plugin.getLogger().severe("Error al crear nuevo jugador: " + e.getMessage());
            }
        }
    }

    public com.irdem.tunama.data.PlayerData getOrCreatePlayerData(java.util.UUID uuid, String username) {
        try {
            // Intentar crear si no existe (atomic operation)
            createPlayerIfNotExists(uuid, username);
            
            // Pequeña pausa para asegurar que se guardó
            Thread.sleep(50);
            
            // Obtener datos
            com.irdem.tunama.data.PlayerData playerData = getPlayerData(uuid);
            return playerData;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al obtener/crear datos del jugador: " + e.getMessage());
            return null;
        }
    }
}
