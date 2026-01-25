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
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")");

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
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")");

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
}
