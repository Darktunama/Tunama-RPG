package com.irdem.tunama.database;

import org.bukkit.plugin.java.JavaPlugin;
import com.irdem.tunama.config.ConfigManager;
import com.irdem.tunama.data.Pet;
import com.irdem.tunama.data.PetCommand;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
            return false;
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
            // Tabla de Jugadores (con soporte multi-personaje)
            stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                "id INTEGER PRIMARY KEY," +
                "uuid TEXT NOT NULL," +
                "character_slot INTEGER DEFAULT 1," +
                "username TEXT NOT NULL," +
                "race TEXT NOT NULL," +
                "class TEXT NOT NULL," +
                "subclass TEXT," +
                "level INTEGER DEFAULT 1," +
                "experience INTEGER DEFAULT 0," +
                "clan_name TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE(uuid, character_slot)" +
            ")");

            // Migración: agregar character_slot si no existe
            try {
                stmt.execute("ALTER TABLE players ADD COLUMN character_slot INTEGER DEFAULT 1");
                plugin.getLogger().info("Columna character_slot agregada a tabla players");
            } catch (SQLException e) {
                // Columna ya existe, ignorar
            }

            // Migración: agregar clan_name si no existe
            try {
                stmt.execute("ALTER TABLE players ADD COLUMN clan_name TEXT");
                plugin.getLogger().info("Columna clan_name agregada a tabla players");
            } catch (SQLException e) {
                // Columna ya existe, ignorar
            }

            // Migración crítica: verificar y corregir constraint UNIQUE
            try {
                String checkConstraint = "SELECT sql FROM sqlite_master WHERE type='table' AND name='players'";
                try (ResultSet rs = stmt.executeQuery(checkConstraint)) {
                    if (rs.next()) {
                        String tableSql = rs.getString("sql");
                        // Si la tabla no tiene el constraint correcto UNIQUE(uuid, character_slot), migrarla
                        if (tableSql != null && !tableSql.contains("UNIQUE(uuid, character_slot)")) {
                            plugin.getLogger().warning("Detectado schema antiguo de tabla players. Migrando a soporte multi-personaje...");
                            migratePlayersTableSchema(stmt);
                            plugin.getLogger().info("Migración de tabla players completada exitosamente");
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error al verificar schema de tabla players: " + e.getMessage());
                e.printStackTrace();
            }

            // Tabla de personajes activos (rastrear qué personaje está usando cada jugador)
            stmt.execute("CREATE TABLE IF NOT EXISTS active_characters (" +
                "uuid TEXT PRIMARY KEY," +
                "active_slot INTEGER DEFAULT 1," +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")");

            // Migración: agregar columnas de equipo si no existen
            String[] equipmentColumns = {
                "ring1 TEXT", "ring2 TEXT", "ring3 TEXT", "ring4 TEXT",
                "necklace TEXT", "amulet1 TEXT", "amulet2 TEXT", "wings TEXT"
            };
            for (String column : equipmentColumns) {
                try {
                    stmt.execute("ALTER TABLE players ADD COLUMN " + column);
                    plugin.getLogger().info("Columna de equipo agregada: " + column);
                } catch (SQLException e) {
                    // Columna ya existe, ignorar
                }
            }

            // Migración: agregar columna ability_slots para guardar habilidades asignadas
            try {
                stmt.execute("ALTER TABLE players ADD COLUMN ability_slots TEXT");
                plugin.getLogger().info("Columna ability_slots agregada a tabla players");
            } catch (SQLException e) {
                // Columna ya existe, ignorar
            }

            // Tabla de Estadísticas
            stmt.execute("CREATE TABLE IF NOT EXISTS player_stats (" +
                "id INTEGER PRIMARY KEY," +
                "player_uuid TEXT UNIQUE NOT NULL," +
                "health INTEGER DEFAULT 10," +
                "strength INTEGER DEFAULT 1," +
                "agility INTEGER DEFAULT 1," +
                "intelligence INTEGER DEFAULT 1," +
                "sacred_power INTEGER DEFAULT 1," +
                "corrupt_power INTEGER DEFAULT 1," +
                "nature_power INTEGER DEFAULT 1," +
                "mob_kills INTEGER DEFAULT 0," +
                "player_kills INTEGER DEFAULT 0," +
                "FOREIGN KEY(player_uuid) REFERENCES players(uuid)" +
            ")");

            // Migración: agregar columnas de kills si no existen
            try {
                stmt.execute("ALTER TABLE player_stats ADD COLUMN mob_kills INTEGER DEFAULT 0");
                plugin.getLogger().info("Columna mob_kills agregada a tabla player_stats");
            } catch (SQLException e) {
                // Columna ya existe, ignorar
            }
            try {
                stmt.execute("ALTER TABLE player_stats ADD COLUMN player_kills INTEGER DEFAULT 0");
                plugin.getLogger().info("Columna player_kills agregada a tabla player_stats");
            } catch (SQLException e) {
                // Columna ya existe, ignorar
            }
            
            // Migración: agregar columna stat_points en players si no existe
            try {
                stmt.execute("ALTER TABLE players ADD COLUMN stat_points INTEGER DEFAULT 0");
                plugin.getLogger().info("Columna stat_points agregada a tabla players");
            } catch (SQLException e) {
                // Columna ya existe, ignorar
            }

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

            // Tabla de Mascotas
            stmt.execute("CREATE TABLE IF NOT EXISTS player_pets (" +
                "id TEXT PRIMARY KEY," +
                "owner_uuid TEXT NOT NULL," +
                "character_slot INTEGER DEFAULT 1," +
                "pet_type TEXT NOT NULL," +
                "custom_name TEXT," +
                "level INTEGER DEFAULT 1," +
                "experience INTEGER DEFAULT 0," +
                "current_health INTEGER," +
                "is_dead INTEGER DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")");

            // Agregar columna is_dead si no existe (migración)
            try {
                stmt.execute("ALTER TABLE player_pets ADD COLUMN is_dead INTEGER DEFAULT 0");
            } catch (SQLException ignored) {
                // La columna ya existe
            }

            plugin.getLogger().info("Tablas de SQLite creadas/verificadas correctamente");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al crear tablas SQLite: " + e.getMessage());
        }
    }

    /**
     * Migra la tabla players del esquema antiguo (UNIQUE uuid) al nuevo esquema (UNIQUE uuid, character_slot)
     */
    private void migratePlayersTableSchema(Statement stmt) throws SQLException {
        // 1. Renombrar tabla antigua
        stmt.execute("ALTER TABLE players RENAME TO players_old");
        plugin.getLogger().info("Tabla players renombrada a players_old");

        // 2. Crear nueva tabla con el schema correcto
        stmt.execute("CREATE TABLE players (" +
            "id INTEGER PRIMARY KEY," +
            "uuid TEXT NOT NULL," +
            "character_slot INTEGER DEFAULT 1," +
            "username TEXT NOT NULL," +
            "race TEXT NOT NULL," +
            "class TEXT NOT NULL," +
            "subclass TEXT," +
            "level INTEGER DEFAULT 1," +
            "experience INTEGER DEFAULT 0," +
            "clan_name TEXT," +
            "ring1 TEXT," +
            "ring2 TEXT," +
            "ring3 TEXT," +
            "ring4 TEXT," +
            "necklace TEXT," +
            "amulet1 TEXT," +
            "amulet2 TEXT," +
            "wings TEXT," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "UNIQUE(uuid, character_slot)" +
        ")");
        plugin.getLogger().info("Nueva tabla players creada con schema correcto");

        // 3. Copiar datos de la tabla antigua a la nueva
        // Verificar qué columnas existen en la tabla antigua
        String copyQuery = "INSERT INTO players (uuid, username, race, class, subclass, level, experience, character_slot";
        String selectQuery = "SELECT uuid, username, race, class, subclass, level, experience, 1 as character_slot";

        // Intentar incluir columnas opcionales si existen
        try (ResultSet rs = stmt.executeQuery("SELECT * FROM players_old LIMIT 1")) {
            java.sql.ResultSetMetaData metaData = rs.getMetaData();
            boolean hasClanName = false;
            boolean hasEquipment = false;

            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnName(i);
                if (columnName.equals("clan_name")) hasClanName = true;
                if (columnName.equals("ring1")) hasEquipment = true;
            }

            if (hasClanName) {
                copyQuery += ", clan_name";
                selectQuery += ", clan_name";
            }
            if (hasEquipment) {
                copyQuery += ", ring1, ring2, ring3, ring4, necklace, amulet1, amulet2, wings";
                selectQuery += ", ring1, ring2, ring3, ring4, necklace, amulet1, amulet2, wings";
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("No se pudieron verificar columnas opcionales, usando columnas básicas");
        }

        copyQuery += ") " + selectQuery + " FROM players_old";
        stmt.execute(copyQuery);

        // Contar registros copiados
        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM players")) {
            if (rs.next()) {
                int count = rs.getInt(1);
                plugin.getLogger().info("Copiados " + count + " registros a la nueva tabla players");
            }
        }

        // 4. Eliminar tabla antigua
        stmt.execute("DROP TABLE players_old");
        plugin.getLogger().info("Tabla players_old eliminada");
    }

    private void createMySQLTables() {
        try (Statement stmt = connection.createStatement()) {
            // Tabla de Jugadores (con soporte multi-personaje)
            stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "uuid VARCHAR(36) NOT NULL," +
                "character_slot INT DEFAULT 1," +
                "username VARCHAR(255) NOT NULL," +
                "race VARCHAR(50) NOT NULL," +
                "class VARCHAR(50) NOT NULL," +
                "subclass VARCHAR(50)," +
                "level INT DEFAULT 1," +
                "experience BIGINT DEFAULT 0," +
                "clan_name VARCHAR(255)," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE KEY unique_character (uuid, character_slot)" +
            ")");

            // Migración: agregar character_slot si no existe
            try {
                stmt.execute("ALTER TABLE players ADD COLUMN character_slot INT DEFAULT 1");
                plugin.getLogger().info("Columna character_slot agregada a tabla players (MySQL)");
            } catch (SQLException e) {
                // Columna ya existe, ignorar
            }

            // Migración: agregar clan_name si no existe
            try {
                stmt.execute("ALTER TABLE players ADD COLUMN clan_name VARCHAR(255)");
                plugin.getLogger().info("Columna clan_name agregada a tabla players (MySQL)");
            } catch (SQLException e) {
                // Columna ya existe, ignorar
            }

            // Tabla de personajes activos (rastrear qué personaje está usando cada jugador)
            stmt.execute("CREATE TABLE IF NOT EXISTS active_characters (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "active_slot INT DEFAULT 1," +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
            ")");

            // Migración: agregar columnas de equipo si no existen
            String[] equipmentColumns = {
                "ring1 VARCHAR(255)", "ring2 VARCHAR(255)", "ring3 VARCHAR(255)", "ring4 VARCHAR(255)",
                "necklace VARCHAR(255)", "amulet1 VARCHAR(255)", "amulet2 VARCHAR(255)", "wings VARCHAR(255)"
            };
            for (String column : equipmentColumns) {
                try {
                    stmt.execute("ALTER TABLE players ADD COLUMN " + column);
                    plugin.getLogger().info("Columna de equipo agregada (MySQL): " + column);
                } catch (SQLException e) {
                    // Columna ya existe, ignorar
                }
            }

            // Tabla de Estadísticas
            stmt.execute("CREATE TABLE IF NOT EXISTS player_stats (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid VARCHAR(36) UNIQUE NOT NULL," +
                "health INT DEFAULT 10," +
                "strength INT DEFAULT 1," +
                "agility INT DEFAULT 1," +
                "intelligence INT DEFAULT 1," +
                "sacred_power INT DEFAULT 1," +
                "corrupt_power INT DEFAULT 1," +
                "nature_power INT DEFAULT 1," +
                "mob_kills INT DEFAULT 0," +
                "player_kills INT DEFAULT 0," +
                "FOREIGN KEY(player_uuid) REFERENCES players(uuid)" +
            ")");

            // Migración: agregar columnas de kills si no existen
            try {
                stmt.execute("ALTER TABLE player_stats ADD COLUMN mob_kills INT DEFAULT 0");
                plugin.getLogger().info("Columna mob_kills agregada a tabla player_stats (MySQL)");
            } catch (SQLException e) {
                // Columna ya existe, ignorar
            }
            try {
                stmt.execute("ALTER TABLE player_stats ADD COLUMN player_kills INT DEFAULT 0");
                plugin.getLogger().info("Columna player_kills agregada a tabla player_stats (MySQL)");
            } catch (SQLException e) {
                // Columna ya existe, ignorar
            }
            
            // Migración: agregar columna stat_points en players si no existe
            try {
                stmt.execute("ALTER TABLE players ADD COLUMN stat_points INT DEFAULT 0");
                plugin.getLogger().info("Columna stat_points agregada a tabla players (MySQL)");
            } catch (SQLException e) {
                // Columna ya existe, ignorar
            }

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

            // Tabla de Mascotas
            stmt.execute("CREATE TABLE IF NOT EXISTS player_pets (" +
                "id VARCHAR(36) PRIMARY KEY," +
                "owner_uuid VARCHAR(36) NOT NULL," +
                "character_slot INT DEFAULT 1," +
                "pet_type VARCHAR(50) NOT NULL," +
                "custom_name VARCHAR(100)," +
                "level INT DEFAULT 1," +
                "experience INT DEFAULT 0," +
                "current_health INT," +
                "is_dead TINYINT DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")");

            // Agregar columna is_dead si no existe (migración)
            try {
                stmt.execute("ALTER TABLE player_pets ADD COLUMN is_dead TINYINT DEFAULT 0");
            } catch (SQLException ignored) {
                // La columna ya existe
            }

            plugin.getLogger().info("Tablas de MySQL creadas/verificadas correctamente");
        } catch (SQLException e) {
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
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al desconectar: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean isNewPlayer(java.util.UUID uuid) {
        if (connection == null) {
            plugin.getLogger().warning("No hay conexión a la base de datos");
            return true;
        }

        try {
            // Verificar si existe algún personaje para este UUID
            String query = "SELECT COUNT(*) as count FROM players WHERE uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("count") == 0;
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al verificar si es nuevo jugador: " + e.getMessage());
            return true;
        }
    }

    /**
     * Obtiene el CharacterManager
     */
    public com.irdem.tunama.data.CharacterManager getCharacterManager() {
        return new com.irdem.tunama.data.CharacterManager(plugin, connection);
    }

    public void saveNewPlayer(java.util.UUID uuid, String username, String raceId, String classId) {
        if (connection == null) {
            plugin.getLogger().severe("No hay conexión a la base de datos");
            return;
        }

        try {
            com.irdem.tunama.data.CharacterManager charManager = getCharacterManager();
            int activeSlot = charManager.getActiveSlot(uuid);

            // Usar UPDATE si existe, INSERT si no
            String updateQuery = "UPDATE players SET race = ?, class = ? WHERE uuid = ? AND character_slot = ?";
            try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                updateStmt.setString(1, raceId);
                updateStmt.setString(2, classId);
                updateStmt.setString(3, uuid.toString());
                updateStmt.setInt(4, activeSlot);
                int updated = updateStmt.executeUpdate();

                if (updated == 0) {
                    // El jugador no existe en este slot, crearlo
                    String insertQuery = "INSERT INTO players (uuid, character_slot, username, race, class, subclass, level, experience, stat_points, clan_name) " +
                                        "VALUES (?, ?, ?, ?, ?, '', 1, 0, 0, NULL)";
                    try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
                        stmt.setString(1, uuid.toString());
                        stmt.setInt(2, activeSlot);
                        stmt.setString(3, username);
                        stmt.setString(4, raceId);
                        stmt.setString(5, classId);
                        stmt.executeUpdate();
                    }
                }
            }

            // También insertar estadísticas por defecto si no existen
            // Usar UUID compuesto para player_stats: "uuid_slot"
            String compositeUuid = uuid.toString() + "_" + activeSlot;
            String checkStatsQuery = "SELECT player_uuid FROM player_stats WHERE player_uuid = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkStatsQuery)) {
                checkStmt.setString(1, compositeUuid);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        String statsQuery = "INSERT INTO player_stats (player_uuid) VALUES (?)";
                        try (PreparedStatement statsStmt = connection.prepareStatement(statsQuery)) {
                            statsStmt.setString(1, compositeUuid);
                            statsStmt.executeUpdate();
                        }
                    }
                }
            }

            plugin.getLogger().info("Jugador guardado: " + username + " (slot " + activeSlot + ")");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al guardar jugador: " + e.getMessage());
        }
    }

    public com.irdem.tunama.data.PlayerData getPlayerData(java.util.UUID uuid) {
        if (connection == null) {
            plugin.getLogger().warning("No hay conexión a la base de datos");
            return null;
        }

        try {
            com.irdem.tunama.data.CharacterManager charManager = getCharacterManager();
            int activeSlot = charManager.getActiveSlot(uuid);

            String query = "SELECT uuid, character_slot, username, race, class, subclass, level, experience, stat_points, clan_name, " +
                          "ring1, ring2, ring3, ring4, necklace, amulet1, amulet2, wings " +
                          "FROM players WHERE uuid = ? AND character_slot = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                stmt.setInt(2, activeSlot);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        com.irdem.tunama.data.PlayerData playerData = new com.irdem.tunama.data.PlayerData(
                            uuid, rs.getString("username")
                        );
                        playerData.setRace(rs.getString("race"));
                        playerData.setPlayerClass(rs.getString("class"));
                        playerData.setSubclass(rs.getString("subclass"));
                        playerData.setLevel(rs.getInt("level"));
                        playerData.addExperience(rs.getLong("experience"));
                        playerData.setStatPoints(rs.getInt("stat_points"));
                        playerData.setClanName(rs.getString("clan_name"));
                        // Cargar equipo
                        playerData.setRing1(rs.getString("ring1"));
                        playerData.setRing2(rs.getString("ring2"));
                        playerData.setRing3(rs.getString("ring3"));
                        playerData.setRing4(rs.getString("ring4"));
                        playerData.setNecklace(rs.getString("necklace"));
                        playerData.setAmulet1(rs.getString("amulet1"));
                        playerData.setAmulet2(rs.getString("amulet2"));
                        try { playerData.setWings(rs.getString("wings")); } catch (SQLException ignored) {}

                        // Cargar estadísticas
                        loadPlayerStats(playerData, activeSlot);

                        // Recalcular maná máximo con las stats cargadas
                        playerData.recalculateMaxMana();

                        return playerData;
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al obtener datos del jugador: " + e.getMessage());
            return null;
        }
    }
    
    public void loadPlayerStats(com.irdem.tunama.data.PlayerData playerData) {
        com.irdem.tunama.data.CharacterManager charManager = getCharacterManager();
        int activeSlot = charManager.getActiveSlot(playerData.getUUID());
        loadPlayerStats(playerData, activeSlot);
    }

    public void loadPlayerStats(com.irdem.tunama.data.PlayerData playerData, int slot) {
        if (connection == null) {
            plugin.getLogger().warning("No hay conexión a la base de datos");
            return;
        }

        try {
            // Usar UUID compuesto para player_stats: "uuid_slot"
            String compositeUuid = playerData.getUUID().toString() + "_" + slot;
            String query = "SELECT health, strength, agility, intelligence, sacred_power, corrupt_power, nature_power " +
                          "FROM player_stats WHERE player_uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, compositeUuid);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        playerData.getStats().setHealth(rs.getInt("health"));
                        playerData.getStats().setStrength(rs.getInt("strength"));
                        playerData.getStats().setAgility(rs.getInt("agility"));
                        playerData.getStats().setIntelligence(rs.getInt("intelligence"));
                        playerData.getStats().setSacredPower(rs.getInt("sacred_power"));
                        playerData.getStats().setCorruptPower(rs.getInt("corrupt_power"));
                        playerData.getStats().setNaturePower(rs.getInt("nature_power"));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al cargar estadísticas del jugador: " + e.getMessage());
        }
    }
    
    public void savePlayerStats(com.irdem.tunama.data.PlayerData playerData) {
        if (connection == null) {
            plugin.getLogger().severe("No hay conexión a la base de datos");
            return;
        }

        try {
            com.irdem.tunama.data.CharacterManager charManager = getCharacterManager();
            int activeSlot = charManager.getActiveSlot(playerData.getUUID());
            // Usar UUID compuesto para player_stats: "uuid_slot"
            String compositeUuid = playerData.getUUID().toString() + "_" + activeSlot;

            String query = "UPDATE player_stats SET health = ?, strength = ?, agility = ?, intelligence = ?, " +
                          "sacred_power = ?, corrupt_power = ?, nature_power = ? WHERE player_uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, playerData.getStats().getHealth());
                stmt.setInt(2, playerData.getStats().getStrength());
                stmt.setInt(3, playerData.getStats().getAgility());
                stmt.setInt(4, playerData.getStats().getIntelligence());
                stmt.setInt(5, playerData.getStats().getSacredPower());
                stmt.setInt(6, playerData.getStats().getCorruptPower());
                stmt.setInt(7, playerData.getStats().getNaturePower());
                stmt.setString(8, compositeUuid);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al guardar estadísticas del jugador: " + e.getMessage());
        }
    }

    /**
     * Guarda los slots de habilidades asignadas del jugador
     * @param uuid UUID del jugador
     * @param abilitySlots Map de slot (0-8) a ability ID
     */
    public void saveAbilitySlots(java.util.UUID uuid, java.util.Map<Integer, String> abilitySlots) {
        if (connection == null) return;

        try {
            com.irdem.tunama.data.CharacterManager charManager = getCharacterManager();
            int activeSlot = charManager.getActiveSlot(uuid);

            // Convertir Map a JSON simple
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (java.util.Map.Entry<Integer, String> entry : abilitySlots.entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
                first = false;
            }
            json.append("}");

            String query = "UPDATE players SET ability_slots = ? WHERE uuid = ? AND character_slot = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, json.toString());
                stmt.setString(2, uuid.toString());
                stmt.setInt(3, activeSlot);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al guardar ability slots: " + e.getMessage());
        }
    }

    /**
     * Carga los slots de habilidades asignadas del jugador
     * @param uuid UUID del jugador
     * @return Map de slot (0-8) a ability ID
     */
    public java.util.Map<Integer, String> loadAbilitySlots(java.util.UUID uuid) {
        java.util.Map<Integer, String> result = new java.util.HashMap<>();
        if (connection == null) return result;

        try {
            com.irdem.tunama.data.CharacterManager charManager = getCharacterManager();
            int activeSlot = charManager.getActiveSlot(uuid);

            String query = "SELECT ability_slots FROM players WHERE uuid = ? AND character_slot = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                stmt.setInt(2, activeSlot);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String json = rs.getString("ability_slots");
                        if (json != null && !json.isEmpty() && !json.equals("{}")) {
                            // Parsear JSON simple: {"0":"flecha-rapida","1":"multi-disparo"}
                            json = json.substring(1, json.length() - 1); // Quitar { }
                            if (!json.isEmpty()) {
                                String[] pairs = json.split(",");
                                for (String pair : pairs) {
                                    String[] keyValue = pair.split(":");
                                    if (keyValue.length == 2) {
                                        int slot = Integer.parseInt(keyValue[0].replace("\"", "").trim());
                                        String abilityId = keyValue[1].replace("\"", "").trim();
                                        result.put(slot, abilityId);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException | NumberFormatException e) {
            plugin.getLogger().severe("Error al cargar ability slots: " + e.getMessage());
        }
        return result;
    }

    public void updatePlayerData(com.irdem.tunama.data.PlayerData playerData) {
        if (connection == null) {
            plugin.getLogger().severe("No hay conexión a la base de datos");
            return;
        }

        try {
            com.irdem.tunama.data.CharacterManager charManager = getCharacterManager();
            int activeSlot = charManager.getActiveSlot(playerData.getUUID());

            String query = "UPDATE players SET race = ?, class = ?, subclass = ?, level = ?, " +
                          "experience = ?, stat_points = ?, clan_name = ?, ring1 = ?, ring2 = ?, ring3 = ?, ring4 = ?, " +
                          "necklace = ?, amulet1 = ?, amulet2 = ?, wings = ? WHERE uuid = ? AND character_slot = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, playerData.getRace());
                stmt.setString(2, playerData.getPlayerClass());
                stmt.setString(3, playerData.getSubclass());
                stmt.setInt(4, playerData.getLevel());
                stmt.setLong(5, playerData.getExperience());
                stmt.setInt(6, playerData.getStatPoints());
                stmt.setString(7, playerData.getClanName());
                stmt.setString(8, playerData.getRing1());
                stmt.setString(9, playerData.getRing2());
                stmt.setString(10, playerData.getRing3());
                stmt.setString(11, playerData.getRing4());
                stmt.setString(12, playerData.getNecklace());
                stmt.setString(13, playerData.getAmulet1());
                stmt.setString(14, playerData.getAmulet2());
                stmt.setString(15, playerData.getWings());
                stmt.setString(16, playerData.getUUID().toString());
                stmt.setInt(17, activeSlot);
                stmt.executeUpdate();
            }

            // Guardar estadísticas también
            savePlayerStats(playerData);

            plugin.getLogger().info("Datos del jugador actualizados: " + playerData.getUsername() + " (slot " + activeSlot + ")");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al actualizar datos del jugador: " + e.getMessage());
        }
    }

    public void createPlayerIfNotExists(java.util.UUID uuid, String username) {
        if (connection == null) {
            plugin.getLogger().severe("No hay conexión a la base de datos");
            return;
        }

        try {
            com.irdem.tunama.data.CharacterManager charManager = getCharacterManager();
            int activeSlot = charManager.getActiveSlot(uuid);

            String query;
            if (databaseType.equalsIgnoreCase("sqlite")) {
                query = "INSERT OR IGNORE INTO players (uuid, character_slot, username, race, class, subclass, level, experience, stat_points, clan_name) " +
                       "VALUES (?, ?, ?, '', '', '', 1, 0, 0, NULL)";
            } else {
                query = "INSERT INTO players (uuid, character_slot, username, race, class, subclass, level, experience, stat_points, clan_name) " +
                       "VALUES (?, ?, ?, '', '', '', 1, 0, 0, NULL) " +
                       "ON DUPLICATE KEY UPDATE uuid=uuid";
            }

            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                stmt.setInt(2, activeSlot);
                stmt.setString(3, username);
                int result = stmt.executeUpdate();

                if (result > 0) {
                    plugin.getLogger().info("Nuevo jugador creado: " + username + " (slot " + activeSlot + ")");
                }
            }
        } catch (SQLException e) {
            // Ignorar si ya existe (puede ocurrir por race condition)
            String errorMsg = e.getMessage();
            if (errorMsg != null && !errorMsg.contains("UNIQUE") && !errorMsg.contains("Duplicate entry")) {
                plugin.getLogger().severe("Error al crear nuevo jugador: " + errorMsg);
            }
        }
    }

    public com.irdem.tunama.data.PlayerData getOrCreatePlayerData(java.util.UUID uuid, String username) {
        if (connection == null) {
            plugin.getLogger().severe("No hay conexión a la base de datos");
            return null;
        }
        
        try {
            // Intentar crear si no existe (atomic operation)
            createPlayerIfNotExists(uuid, username);
            
            // Obtener datos (no necesitamos Thread.sleep, la operación es atómica)
            com.irdem.tunama.data.PlayerData playerData = getPlayerData(uuid);
            
            // Si aún no existe después de intentar crearlo, crear con datos por defecto
            if (playerData == null) {
                // Reintentar una vez más
                createPlayerIfNotExists(uuid, username);
                playerData = getPlayerData(uuid);
            }
            
            return playerData;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al obtener/crear datos del jugador: " + e.getMessage());
            return null;
        }
    }

    public java.util.List<java.util.Map<String, Object>> getTopPlayersByLevel(int limit) {
        if (connection == null) {
            plugin.getLogger().warning("No hay conexión a la base de datos");
            return new java.util.ArrayList<>();
        }

        java.util.List<java.util.Map<String, Object>> topPlayers = new java.util.ArrayList<>();
        try {
            // Subconsulta para obtener solo el personaje de mayor nivel por UUID
            String query = "SELECT p.username, p.level, p.experience, p.race, p.class, " +
                          "COALESCE(ps.mob_kills, 0) as mob_kills, " +
                          "COALESCE(ps.player_kills, 0) as player_kills " +
                          "FROM players p " +
                          "INNER JOIN (" +
                          "    SELECT uuid, MAX(level) as max_level, MAX(experience) as max_exp " +
                          "    FROM players " +
                          "    WHERE username IS NOT NULL AND username != '' " +
                          "    GROUP BY uuid" +
                          ") max_p ON p.uuid = max_p.uuid AND p.level = max_p.max_level AND p.experience = max_p.max_exp " +
                          "LEFT JOIN player_stats ps ON (p.uuid || '_' || p.character_slot) = ps.player_uuid " +
                          "ORDER BY p.level DESC, p.experience DESC " +
                          "LIMIT ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        java.util.Map<String, Object> playerInfo = new java.util.HashMap<>();
                        playerInfo.put("username", rs.getString("username"));
                        playerInfo.put("level", rs.getInt("level"));
                        playerInfo.put("experience", rs.getLong("experience"));
                        playerInfo.put("race", rs.getString("race"));
                        playerInfo.put("class", rs.getString("class"));
                        playerInfo.put("mob_kills", rs.getInt("mob_kills"));
                        playerInfo.put("player_kills", rs.getInt("player_kills"));
                        topPlayers.add(playerInfo);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al obtener top de nivel: " + e.getMessage());
        }
        return topPlayers;
    }

    public java.util.List<java.util.Map<String, Object>> getTopPlayersByMobKills(int limit) {
        if (connection == null) {
            plugin.getLogger().warning("No hay conexión a la base de datos");
            return new java.util.ArrayList<>();
        }

        java.util.List<java.util.Map<String, Object>> topPlayers = new java.util.ArrayList<>();
        try {
            // Subconsulta para obtener el personaje con más mob kills por UUID
            String query = "SELECT p.username, p.level, COALESCE(ps.mob_kills, 0) as mob_kills " +
                          "FROM players p " +
                          "INNER JOIN (" +
                          "    SELECT p2.uuid, p2.character_slot, MAX(COALESCE(ps2.mob_kills, 0)) as max_kills " +
                          "    FROM players p2 " +
                          "    LEFT JOIN player_stats ps2 ON (p2.uuid || '_' || p2.character_slot) = ps2.player_uuid " +
                          "    WHERE p2.username IS NOT NULL AND p2.username != '' " +
                          "    GROUP BY p2.uuid" +
                          ") max_k ON p.uuid = max_k.uuid " +
                          "LEFT JOIN player_stats ps ON (p.uuid || '_' || p.character_slot) = ps.player_uuid " +
                          "WHERE COALESCE(ps.mob_kills, 0) = max_k.max_kills " +
                          "ORDER BY mob_kills DESC " +
                          "LIMIT ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        java.util.Map<String, Object> playerInfo = new java.util.HashMap<>();
                        playerInfo.put("username", rs.getString("username"));
                        playerInfo.put("level", rs.getInt("level"));
                        playerInfo.put("mob_kills", rs.getInt("mob_kills"));
                        topPlayers.add(playerInfo);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al obtener top de kills de mobs: " + e.getMessage());
        }
        return topPlayers;
    }

    public java.util.List<java.util.Map<String, Object>> getTopPlayersByPlayerKills(int limit) {
        if (connection == null) {
            plugin.getLogger().warning("No hay conexión a la base de datos");
            return new java.util.ArrayList<>();
        }

        java.util.List<java.util.Map<String, Object>> topPlayers = new java.util.ArrayList<>();
        try {
            // Subconsulta para obtener el personaje con más player kills por UUID
            String query = "SELECT p.username, p.level, COALESCE(ps.player_kills, 0) as player_kills " +
                          "FROM players p " +
                          "INNER JOIN (" +
                          "    SELECT p2.uuid, p2.character_slot, MAX(COALESCE(ps2.player_kills, 0)) as max_kills " +
                          "    FROM players p2 " +
                          "    LEFT JOIN player_stats ps2 ON (p2.uuid || '_' || p2.character_slot) = ps2.player_uuid " +
                          "    WHERE p2.username IS NOT NULL AND p2.username != '' " +
                          "    GROUP BY p2.uuid" +
                          ") max_k ON p.uuid = max_k.uuid " +
                          "LEFT JOIN player_stats ps ON (p.uuid || '_' || p.character_slot) = ps.player_uuid " +
                          "WHERE COALESCE(ps.player_kills, 0) = max_k.max_kills " +
                          "ORDER BY player_kills DESC " +
                          "LIMIT ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        java.util.Map<String, Object> playerInfo = new java.util.HashMap<>();
                        playerInfo.put("username", rs.getString("username"));
                        playerInfo.put("level", rs.getInt("level"));
                        playerInfo.put("player_kills", rs.getInt("player_kills"));
                        topPlayers.add(playerInfo);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al obtener top de kills de jugadores: " + e.getMessage());
        }
        return topPlayers;
    }

    public void incrementMobKills(java.util.UUID playerUuid, int characterSlot) {
        if (connection == null) return;

        String playerKey = playerUuid.toString() + "_" + characterSlot;

        try {
            // Primero intentar actualizar
            String updateQuery = "UPDATE player_stats SET mob_kills = mob_kills + 1 WHERE player_uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(updateQuery)) {
                stmt.setString(1, playerKey);
                int updated = stmt.executeUpdate();

                // Si no existe, insertar
                if (updated == 0) {
                    String insertQuery = "INSERT INTO player_stats (player_uuid, mob_kills, player_kills) VALUES (?, 1, 0)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                        insertStmt.setString(1, playerKey);
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al incrementar mob_kills: " + e.getMessage());
        }
    }

    public void incrementPlayerKills(java.util.UUID playerUuid, int characterSlot) {
        if (connection == null) return;

        String playerKey = playerUuid.toString() + "_" + characterSlot;

        try {
            // Primero intentar actualizar
            String updateQuery = "UPDATE player_stats SET player_kills = player_kills + 1 WHERE player_uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(updateQuery)) {
                stmt.setString(1, playerKey);
                int updated = stmt.executeUpdate();

                // Si no existe, insertar
                if (updated == 0) {
                    String insertQuery = "INSERT INTO player_stats (player_uuid, mob_kills, player_kills) VALUES (?, 0, 1)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                        insertStmt.setString(1, playerKey);
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al incrementar player_kills: " + e.getMessage());
        }
    }

    // ==================== SISTEMA DE MASCOTAS ====================

    /**
     * Carga todas las mascotas de un jugador para un slot de personaje específico
     */
    public List<Pet> loadPlayerPets(UUID playerUuid, int characterSlot) {
        List<Pet> pets = new ArrayList<>();
        if (connection == null) return pets;

        String query = "SELECT * FROM player_pets WHERE owner_uuid = ? AND character_slot = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setInt(2, characterSlot);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Pet pet = new Pet();
                    pet.setId(rs.getString("id"));
                    pet.setOwnerUuid(playerUuid);
                    pet.setCharacterSlot(characterSlot);
                    pet.setTypeId(rs.getString("pet_type"));
                    pet.setCustomName(rs.getString("custom_name"));
                    pet.setLevel(rs.getInt("level"));
                    pet.setExperience(rs.getInt("experience"));
                    pet.setCurrentHealth(rs.getInt("current_health"));
                    pet.setDead(rs.getInt("is_dead") == 1); // Cargar estado de muerte
                    pet.setActive(false); // Las mascotas cargadas empiezan inactivas
                    pet.setCurrentCommand(PetCommand.FOLLOW); // Comando por defecto
                    pets.add(pet);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al cargar mascotas del jugador: " + e.getMessage());
        }

        return pets;
    }

    /**
     * Guarda o actualiza una mascota en la base de datos
     */
    public void savePet(Pet pet) {
        if (connection == null) return;

        // Verificar si ya existe
        String checkQuery = "SELECT id FROM player_pets WHERE id = ?";
        boolean exists = false;

        try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
            checkStmt.setString(1, pet.getId());
            try (ResultSet rs = checkStmt.executeQuery()) {
                exists = rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al verificar mascota existente: " + e.getMessage());
            return;
        }

        if (exists) {
            // Actualizar
            String updateQuery = "UPDATE player_pets SET pet_type = ?, custom_name = ?, level = ?, " +
                    "experience = ?, current_health = ?, is_dead = ? WHERE id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(updateQuery)) {
                stmt.setString(1, pet.getTypeId());
                stmt.setString(2, pet.getCustomName());
                stmt.setInt(3, pet.getLevel());
                stmt.setInt(4, pet.getExperience());
                stmt.setInt(5, pet.getCurrentHealth());
                stmt.setInt(6, pet.isDead() ? 1 : 0);
                stmt.setString(7, pet.getId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error al actualizar mascota: " + e.getMessage());
            }
        } else {
            // Insertar
            String insertQuery = "INSERT INTO player_pets (id, owner_uuid, character_slot, pet_type, " +
                    "custom_name, level, experience, current_health, is_dead) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
                stmt.setString(1, pet.getId());
                stmt.setString(2, pet.getOwnerUuid().toString());
                stmt.setInt(3, pet.getCharacterSlot());
                stmt.setString(4, pet.getTypeId());
                stmt.setString(5, pet.getCustomName());
                stmt.setInt(6, pet.getLevel());
                stmt.setInt(7, pet.getExperience());
                stmt.setInt(8, pet.getCurrentHealth());
                stmt.setInt(9, pet.isDead() ? 1 : 0);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error al insertar mascota: " + e.getMessage());
            }
        }
    }

    /**
     * Elimina una mascota de la base de datos
     */
    public void deletePet(String petId) {
        if (connection == null) return;

        String query = "DELETE FROM player_pets WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, petId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al eliminar mascota: " + e.getMessage());
        }
    }

    /**
     * Actualiza solo la vida de una mascota (para saves rápidos)
     */
    public void updatePetHealth(String petId, int health) {
        if (connection == null) return;

        String query = "UPDATE player_pets SET current_health = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, health);
            stmt.setString(2, petId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al actualizar vida de mascota: " + e.getMessage());
        }
    }

    /**
     * Obtiene el número de mascotas que tiene un jugador
     */
    public int getPetCount(UUID playerUuid, int characterSlot) {
        if (connection == null) return 0;

        String query = "SELECT COUNT(*) FROM player_pets WHERE owner_uuid = ? AND character_slot = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setInt(2, characterSlot);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al contar mascotas: " + e.getMessage());
        }

        return 0;
    }
}
