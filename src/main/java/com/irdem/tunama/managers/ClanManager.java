package com.irdem.tunama.managers;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.Clan;
import com.irdem.tunama.data.ClanLog;

import java.sql.*;
import java.util.*;

public class ClanManager {
    private final TunamaRPG plugin;
    private final Connection connection;
    private final Map<String, Clan> clansByTag;
    private final Map<Integer, Clan> clansById;
    private final Map<UUID, String> playerClanCache;
    private final Map<UUID, String> pendingInvites;

    public ClanManager(TunamaRPG plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
        this.clansByTag = new HashMap<>();
        this.clansById = new HashMap<>();
        this.playerClanCache = new HashMap<>();
        this.pendingInvites = new HashMap<>();

        createTables();
        loadClans();
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            // Tabla principal de clanes
            stmt.execute("CREATE TABLE IF NOT EXISTS clans (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "tag TEXT UNIQUE NOT NULL," +
                "name TEXT UNIQUE NOT NULL," +
                "leader_uuid TEXT NOT NULL," +
                "gold INTEGER DEFAULT 0," +
                "pvp_kills INTEGER DEFAULT 0," +
                "created_at INTEGER NOT NULL" +
            ")");

            // Migración: verificar y agregar columnas faltantes
            migrateClansTable();

            // Tabla de miembros del clan
            stmt.execute("CREATE TABLE IF NOT EXISTS clan_members (" +
                "clan_id INTEGER NOT NULL," +
                "player_uuid TEXT NOT NULL," +
                "rank TEXT NOT NULL," +
                "joined_at INTEGER NOT NULL," +
                "PRIMARY KEY (clan_id, player_uuid)," +
                "FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE" +
            ")");

            // Migración: verificar y agregar columnas faltantes en clan_members
            migrateClanMembersTable();

            // Tabla de alianzas
            stmt.execute("CREATE TABLE IF NOT EXISTS clan_allies (" +
                "clan_id INTEGER NOT NULL," +
                "ally_tag TEXT NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "PRIMARY KEY (clan_id, ally_tag)," +
                "FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE" +
            ")");

            // Tabla de enemigos
            stmt.execute("CREATE TABLE IF NOT EXISTS clan_enemies (" +
                "clan_id INTEGER NOT NULL," +
                "enemy_tag TEXT NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "PRIMARY KEY (clan_id, enemy_tag)," +
                "FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE" +
            ")");

            // Tabla de logs del clan
            stmt.execute("CREATE TABLE IF NOT EXISTS clan_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "clan_id INTEGER NOT NULL," +
                "player_uuid TEXT NOT NULL," +
                "player_name TEXT NOT NULL," +
                "action TEXT NOT NULL," +
                "amount INTEGER DEFAULT 0," +
                "item TEXT," +
                "timestamp INTEGER NOT NULL," +
                "FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE" +
            ")");

            plugin.getLogger().info("Tablas de clanes creadas/verificadas correctamente");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al crear tablas de clanes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Migra la tabla clans agregando columnas faltantes y renombrando columnas antiguas
     */
    private void migrateClansTable() {
        try {
            // Obtener las columnas existentes
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet columns = meta.getColumns(null, null, "clans", null);

            Set<String> existingColumns = new HashSet<>();
            while (columns.next()) {
                existingColumns.add(columns.getString("COLUMN_NAME").toLowerCase());
            }
            columns.close();

            // Verificar si existe la columna antigua 'clan_name' en lugar de 'name'
            boolean hasClanName = existingColumns.contains("clan_name");
            boolean hasName = existingColumns.contains("name");
            boolean hasClanTag = existingColumns.contains("clan_tag");
            boolean hasTag = existingColumns.contains("tag");

            // Si tiene la estructura antigua, necesitamos migrar la tabla completa
            if (hasClanName || hasClanTag) {
                plugin.getLogger().info("Detectada estructura antigua de tabla clans, migrando...");
                migrateOldClanTable(existingColumns);
                return;
            }

            // Si no tiene la estructura antigua, solo agregar columnas faltantes
            // Agregar columna 'tag' si no existe
            if (!hasTag) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("ALTER TABLE clans ADD COLUMN tag TEXT");
                    plugin.getLogger().info("✓ Columna 'tag' agregada a la tabla clans");
                } catch (SQLException e) {
                    plugin.getLogger().warning("No se pudo agregar columna 'tag': " + e.getMessage());
                }
            }

            // Agregar columna 'name' si no existe
            if (!hasName) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("ALTER TABLE clans ADD COLUMN name TEXT");
                    plugin.getLogger().info("✓ Columna 'name' agregada a la tabla clans");
                } catch (SQLException e) {
                    plugin.getLogger().warning("No se pudo agregar columna 'name': " + e.getMessage());
                }
            }

            // Agregar columna 'gold' si no existe
            if (!existingColumns.contains("gold")) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("ALTER TABLE clans ADD COLUMN gold INTEGER DEFAULT 0");
                    plugin.getLogger().info("✓ Columna 'gold' agregada a la tabla clans");
                } catch (SQLException e) {
                    plugin.getLogger().warning("No se pudo agregar columna 'gold': " + e.getMessage());
                }
            }

            // Agregar columna 'pvp_kills' si no existe
            if (!existingColumns.contains("pvp_kills")) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("ALTER TABLE clans ADD COLUMN pvp_kills INTEGER DEFAULT 0");
                    plugin.getLogger().info("✓ Columna 'pvp_kills' agregada a la tabla clans");
                } catch (SQLException e) {
                    plugin.getLogger().warning("No se pudo agregar columna 'pvp_kills': " + e.getMessage());
                }
            }

            // Agregar columna 'created_at' si no existe
            if (!existingColumns.contains("created_at")) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("ALTER TABLE clans ADD COLUMN created_at INTEGER DEFAULT 0");
                    plugin.getLogger().info("✓ Columna 'created_at' agregada a la tabla clans");
                } catch (SQLException e) {
                    plugin.getLogger().warning("No se pudo agregar columna 'created_at': " + e.getMessage());
                }
            }

            // Agregar columna 'leader_uuid' si no existe
            if (!existingColumns.contains("leader_uuid")) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("ALTER TABLE clans ADD COLUMN leader_uuid TEXT");
                    plugin.getLogger().info("✓ Columna 'leader_uuid' agregada a la tabla clans");
                } catch (SQLException e) {
                    plugin.getLogger().warning("No se pudo agregar columna 'leader_uuid': " + e.getMessage());
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Error al verificar columnas de la tabla clans: " + e.getMessage());
        }
    }

    /**
     * Migra la tabla clans desde la estructura antigua a la nueva
     */
    private void migrateOldClanTable(Set<String> existingColumns) {
        try (Statement stmt = connection.createStatement()) {
            // Verificar si la tabla tiene datos
            ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) FROM clans");
            int rowCount = 0;
            if (countRs.next()) {
                rowCount = countRs.getInt(1);
            }
            countRs.close();

            // Verificar que las columnas críticas existen para poder migrar
            boolean hasTagColumn = existingColumns.contains("clan_tag") || existingColumns.contains("tag");
            boolean hasNameColumn = existingColumns.contains("clan_name") || existingColumns.contains("name");
            boolean hasLeaderColumn = existingColumns.contains("leader_uuid") || existingColumns.contains("owner_uuid");

            // Si la tabla está vacía o no tiene las columnas necesarias, recrearla
            if (rowCount == 0) {
                plugin.getLogger().info("La tabla clans está vacía, recreando con nueva estructura...");
                stmt.execute("DROP TABLE clans");
                stmt.execute("CREATE TABLE clans (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "tag TEXT UNIQUE NOT NULL," +
                    "name TEXT UNIQUE NOT NULL," +
                    "leader_uuid TEXT NOT NULL," +
                    "gold INTEGER DEFAULT 0," +
                    "pvp_kills INTEGER DEFAULT 0," +
                    "created_at INTEGER NOT NULL" +
                ")");
                plugin.getLogger().info("✓ Tabla clans recreada con nueva estructura");
                return;
            }

            // Si faltan columnas críticas, avisar y recrear (se perderán los datos)
            if (!hasTagColumn || !hasNameColumn || !hasLeaderColumn) {
                plugin.getLogger().warning("¡ADVERTENCIA! La tabla clans tiene estructura incompatible.");
                plugin.getLogger().warning("Columnas encontradas: " + existingColumns.toString());
                plugin.getLogger().warning("Se necesitan: tag/clan_tag, name/clan_name, leader_uuid/owner_uuid");
                plugin.getLogger().warning("La tabla será recreada y se perderán " + rowCount + " clanes existentes.");

                stmt.execute("DROP TABLE clans");
                stmt.execute("CREATE TABLE clans (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "tag TEXT UNIQUE NOT NULL," +
                    "name TEXT UNIQUE NOT NULL," +
                    "leader_uuid TEXT NOT NULL," +
                    "gold INTEGER DEFAULT 0," +
                    "pvp_kills INTEGER DEFAULT 0," +
                    "created_at INTEGER NOT NULL" +
                ")");
                plugin.getLogger().warning("✓ Tabla clans recreada (datos anteriores eliminados)");
                return;
            }

            // Si llegamos aquí, podemos migrar los datos
            plugin.getLogger().info("Migrando " + rowCount + " clanes a la nueva estructura...");

            // Crear tabla temporal con la nueva estructura
            stmt.execute("CREATE TABLE IF NOT EXISTS clans_new (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "tag TEXT UNIQUE NOT NULL," +
                "name TEXT UNIQUE NOT NULL," +
                "leader_uuid TEXT NOT NULL," +
                "gold INTEGER DEFAULT 0," +
                "pvp_kills INTEGER DEFAULT 0," +
                "created_at INTEGER NOT NULL" +
            ")");

            // Determinar qué columnas usar para mapear
            String oldTagColumn = existingColumns.contains("clan_tag") ? "clan_tag" : "tag";
            String oldNameColumn = existingColumns.contains("clan_name") ? "clan_name" : "name";
            String oldLeaderColumn = existingColumns.contains("leader_uuid") ? "leader_uuid" : "owner_uuid";
            String oldGoldColumn = existingColumns.contains("gold") ? "gold" : "0";
            String oldKillsColumn = existingColumns.contains("pvp_kills") ? "pvp_kills" : "0";
            String oldCreatedColumn = existingColumns.contains("created_at") ? "created_at" :
                                     (existingColumns.contains("created") ? "created" : String.valueOf(System.currentTimeMillis()));

            // Copiar datos de la tabla antigua a la nueva
            String copyQuery = String.format(
                "INSERT INTO clans_new (id, tag, name, leader_uuid, gold, pvp_kills, created_at) " +
                "SELECT id, %s, %s, %s, %s, %s, %s FROM clans",
                oldTagColumn, oldNameColumn, oldLeaderColumn, oldGoldColumn, oldKillsColumn, oldCreatedColumn
            );

            stmt.execute(copyQuery);
            plugin.getLogger().info("✓ Datos copiados a la nueva estructura");

            // Eliminar tabla antigua
            stmt.execute("DROP TABLE clans");
            plugin.getLogger().info("✓ Tabla antigua eliminada");

            // Renombrar tabla nueva
            stmt.execute("ALTER TABLE clans_new RENAME TO clans");
            plugin.getLogger().info("✓ Migración de tabla clans completada exitosamente");

        } catch (SQLException e) {
            plugin.getLogger().severe("Error al migrar tabla clans: " + e.getMessage());
            e.printStackTrace();

            // En caso de error, intentar limpiar
            try (Statement cleanupStmt = connection.createStatement()) {
                cleanupStmt.execute("DROP TABLE IF EXISTS clans_new");
            } catch (SQLException cleanupError) {
                // Ignorar errores de limpieza
            }
        }
    }

    /**
     * Migra la tabla clan_members agregando columnas faltantes
     */
    private void migrateClanMembersTable() {
        try {
            // Obtener las columnas existentes
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet columns = meta.getColumns(null, null, "clan_members", null);

            Set<String> existingColumns = new HashSet<>();
            while (columns.next()) {
                existingColumns.add(columns.getString("COLUMN_NAME").toLowerCase());
            }
            columns.close();

            // Agregar columna 'rank' si no existe
            if (!existingColumns.contains("rank")) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("ALTER TABLE clan_members ADD COLUMN rank TEXT DEFAULT 'MIEMBRO'");
                    plugin.getLogger().info("✓ Columna 'rank' agregada a la tabla clan_members");
                } catch (SQLException e) {
                    plugin.getLogger().warning("No se pudo agregar columna 'rank': " + e.getMessage());
                }
            }

            // Agregar columna 'joined_at' si no existe
            if (!existingColumns.contains("joined_at")) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("ALTER TABLE clan_members ADD COLUMN joined_at INTEGER DEFAULT 0");
                    plugin.getLogger().info("✓ Columna 'joined_at' agregada a la tabla clan_members");
                } catch (SQLException e) {
                    plugin.getLogger().warning("No se pudo agregar columna 'joined_at': " + e.getMessage());
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Error al verificar columnas de la tabla clan_members: " + e.getMessage());
        }
    }

    private void loadClans() {
        try {
            String query = "SELECT * FROM clans";
            try (PreparedStatement stmt = connection.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String tag = rs.getString("tag");
                    String name = rs.getString("name");
                    UUID leaderId = UUID.fromString(rs.getString("leader_uuid"));
                    long gold = rs.getLong("gold");
                    int pvpKills = rs.getInt("pvp_kills");
                    long createdAt = rs.getLong("created_at");

                    Clan clan = new Clan(id, tag, name, leaderId);
                    clan.setGold(gold);
                    clan.setPvpKills(pvpKills);
                    clan.setCreatedAt(createdAt);

                    // Cargar miembros
                    loadClanMembers(clan);

                    // Cargar aliados
                    loadClanAllies(clan);

                    // Cargar enemigos
                    loadClanEnemies(clan);

                    clansByTag.put(tag.toLowerCase(), clan);
                    clansById.put(id, clan);

                    // Cachear clanes de jugadores
                    for (UUID memberId : clan.getMembers().keySet()) {
                        playerClanCache.put(memberId, tag.toLowerCase());
                    }
                }
            }

            plugin.getLogger().info("Cargados " + clansByTag.size() + " clanes");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al cargar clanes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadClanMembers(Clan clan) throws SQLException {
        String query = "SELECT player_uuid, rank FROM clan_members WHERE clan_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clan.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    Clan.ClanRank rank = Clan.ClanRank.valueOf(rs.getString("rank"));
                    clan.addMember(playerId, rank);
                }
            }
        }
    }

    private void loadClanAllies(Clan clan) throws SQLException {
        String query = "SELECT ally_tag FROM clan_allies WHERE clan_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clan.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    clan.addAlly(rs.getString("ally_tag"));
                }
            }
        }
    }

    private void loadClanEnemies(Clan clan) throws SQLException {
        String query = "SELECT enemy_tag FROM clan_enemies WHERE clan_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clan.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    clan.addEnemy(rs.getString("enemy_tag"));
                }
            }
        }
    }

    public Clan createClan(String tag, String name, UUID leaderId) throws SQLException {
        // Verificar que no exista el tag o nombre
        if (clansByTag.containsKey(tag.toLowerCase())) {
            throw new IllegalArgumentException("Ya existe un clan con ese tag");
        }

        String checkNameQuery = "SELECT COUNT(*) FROM clans WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement stmt = connection.prepareStatement(checkNameQuery)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new IllegalArgumentException("Ya existe un clan con ese nombre");
                }
            }
        }

        // Crear clan en la base de datos
        String insertQuery = "INSERT INTO clans (tag, name, leader_uuid, gold, pvp_kills, created_at) VALUES (?, ?, ?, 0, 0, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, tag);
            stmt.setString(2, name);
            stmt.setString(3, leaderId.toString());
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int clanId = rs.getInt(1);
                    Clan clan = new Clan(clanId, tag, name, leaderId);

                    // Añadir líder como miembro
                    addMemberToDatabase(clanId, leaderId, Clan.ClanRank.LEADER);
                    clan.addMember(leaderId, Clan.ClanRank.LEADER);

                    // Cachear
                    clansByTag.put(tag.toLowerCase(), clan);
                    clansById.put(clanId, clan);
                    playerClanCache.put(leaderId, tag.toLowerCase());

                    return clan;
                }
            }
        }

        throw new SQLException("Error al crear clan");
    }

    public void deleteClan(String tag) throws SQLException {
        Clan clan = getClanByTag(tag);
        if (clan == null) return;

        String upperTag = tag.toUpperCase();

        // Limpiar referencias de alianza/guerra en OTROS clanes (memoria y BD)
        for (Clan otherClan : clansByTag.values()) {
            if (otherClan.getId() != clan.getId()) {
                // Limpiar alianzas
                if (otherClan.isAlly(upperTag)) {
                    otherClan.removeAlly(upperTag);
                    removeAllyFromDatabase(otherClan.getId(), upperTag);
                }
                // Limpiar guerras
                if (otherClan.isEnemy(upperTag)) {
                    otherClan.removeEnemy(upperTag);
                    removeEnemyFromDatabase(otherClan.getId(), upperTag);
                }
            }
        }

        // Eliminar de cache
        for (UUID memberId : clan.getMembers().keySet()) {
            playerClanCache.remove(memberId);
        }
        clansByTag.remove(tag.toLowerCase());
        clansById.remove(clan.getId());

        // Eliminar de base de datos (CASCADE se encarga del resto)
        String query = "DELETE FROM clans WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clan.getId());
            stmt.executeUpdate();
        }
    }

    public void addMemberToDatabase(int clanId, UUID playerId, Clan.ClanRank rank) throws SQLException {
        String query = "INSERT INTO clan_members (clan_id, player_uuid, rank, joined_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clanId);
            stmt.setString(2, playerId.toString());
            stmt.setString(3, rank.name());
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }

    public void removeMemberFromDatabase(int clanId, UUID playerId) throws SQLException {
        String query = "DELETE FROM clan_members WHERE clan_id = ? AND player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clanId);
            stmt.setString(2, playerId.toString());
            stmt.executeUpdate();
        }
    }

    public void updateMemberRank(int clanId, UUID playerId, Clan.ClanRank newRank) throws SQLException {
        String query = "UPDATE clan_members SET rank = ? WHERE clan_id = ? AND player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, newRank.name());
            stmt.setInt(2, clanId);
            stmt.setString(3, playerId.toString());
            stmt.executeUpdate();
        }

        // Actualizar el objeto Clan en memoria
        Clan clan = getClanById(clanId);
        if (clan != null) {
            clan.getMembers().put(playerId, newRank);
        }
    }

    public void transferLeadership(int clanId, UUID newLeaderId) throws SQLException {
        // Obtener el clan actual para saber quién es el líder actual
        Clan clan = getClanById(clanId);
        if (clan == null) {
            throw new SQLException("Clan no encontrado");
        }

        UUID oldLeaderId = clan.getLeaderId();

        // Actualizar el líder en la tabla clans
        String updateClanQuery = "UPDATE clans SET leader_uuid = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateClanQuery)) {
            stmt.setString(1, newLeaderId.toString());
            stmt.setInt(2, clanId);
            stmt.executeUpdate();
        }

        // Degradar al líder anterior a comandante
        updateMemberRank(clanId, oldLeaderId, Clan.ClanRank.COMMANDER);

        // Promover al nuevo líder (debería ser comandante, pero lo establecemos como líder)
        updateMemberRank(clanId, newLeaderId, Clan.ClanRank.LEADER);

        // Actualizar el objeto Clan en memoria
        clan.setLeaderId(newLeaderId);
        clan.getMembers().put(oldLeaderId, Clan.ClanRank.COMMANDER);
        clan.getMembers().put(newLeaderId, Clan.ClanRank.LEADER);
    }

    public void updateClanGold(int clanId, long newGold) throws SQLException {
        String query = "UPDATE clans SET gold = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, newGold);
            stmt.setInt(2, clanId);
            stmt.executeUpdate();
        }
    }

    public void updateClanPvpKills(int clanId, int pvpKills) throws SQLException {
        String query = "UPDATE clans SET pvp_kills = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, pvpKills);
            stmt.setInt(2, clanId);
            stmt.executeUpdate();
        }
    }

    public void addAllyToDatabase(int clanId, String allyTag) throws SQLException {
        String query = "INSERT INTO clan_allies (clan_id, ally_tag, created_at) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clanId);
            stmt.setString(2, allyTag);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }

    public void removeAllyFromDatabase(int clanId, String allyTag) throws SQLException {
        String query = "DELETE FROM clan_allies WHERE clan_id = ? AND ally_tag = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clanId);
            stmt.setString(2, allyTag);
            stmt.executeUpdate();
        }
    }

    public void addEnemyToDatabase(int clanId, String enemyTag) throws SQLException {
        String query = "INSERT INTO clan_enemies (clan_id, enemy_tag, created_at) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clanId);
            stmt.setString(2, enemyTag);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }

    public void removeEnemyFromDatabase(int clanId, String enemyTag) throws SQLException {
        String query = "DELETE FROM clan_enemies WHERE clan_id = ? AND enemy_tag = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clanId);
            stmt.setString(2, enemyTag);
            stmt.executeUpdate();
        }
    }

    public void addLog(ClanLog log) throws SQLException {
        String query = "INSERT INTO clan_logs (clan_id, player_uuid, player_name, action, amount, item, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, log.getClanId());
            stmt.setString(2, log.getPlayerId().toString());
            stmt.setString(3, log.getPlayerName());
            stmt.setString(4, log.getAction());
            stmt.setLong(5, log.getAmount());
            stmt.setString(6, log.getItem());
            stmt.setLong(7, log.getTimestamp());
            stmt.executeUpdate();
        }
    }

    public List<ClanLog> getLogs(int clanId, int limit) throws SQLException {
        List<ClanLog> logs = new ArrayList<>();
        String query = "SELECT * FROM clan_logs WHERE clan_id = ? ORDER BY timestamp DESC LIMIT ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clanId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ClanLog log = new ClanLog(
                        rs.getInt("clan_id"),
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        rs.getString("action"),
                        rs.getLong("amount"),
                        rs.getString("item")
                    );
                    log.setId(rs.getInt("id"));
                    log.setTimestamp(rs.getLong("timestamp"));
                    logs.add(log);
                }
            }
        }
        return logs;
    }

    public List<ClanLog> getLogsPaginated(int clanId, int page, int perPage) throws SQLException {
        List<ClanLog> logs = new ArrayList<>();
        int offset = page * perPage;
        String query = "SELECT * FROM clan_logs WHERE clan_id = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clanId);
            stmt.setInt(2, perPage);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ClanLog log = new ClanLog(
                        rs.getInt("clan_id"),
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        rs.getString("action"),
                        rs.getLong("amount"),
                        rs.getString("item")
                    );
                    log.setId(rs.getInt("id"));
                    log.setTimestamp(rs.getLong("timestamp"));
                    logs.add(log);
                }
            }
        }
        return logs;
    }

    public int getLogsCount(int clanId) throws SQLException {
        String query = "SELECT COUNT(*) as count FROM clan_logs WHERE clan_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clanId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        }
        return 0;
    }

    public void cleanOldLogs() throws SQLException {
        // Eliminar logs más antiguos de 1 mes (30 días)
        long oneMonthAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        String query = "DELETE FROM clan_logs WHERE timestamp < ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, oneMonthAgo);
            stmt.executeUpdate();
        }
    }

    public Clan getClanByTag(String tag) {
        // Búsqueda directa por tag con colores
        Clan clan = clansByTag.get(tag.toLowerCase());
        if (clan != null) return clan;

        // Búsqueda por tag limpio (sin colores ni brackets)
        String cleanInput = tag.replaceAll("[&§][0-9a-fk-or]", "")
                               .replace("[", "").replace("]", "")
                               .toLowerCase().trim();
        for (Clan c : clansByTag.values()) {
            String cleanTag = c.getTag().replaceAll("[&§][0-9a-fk-or]", "")
                                        .replace("[", "").replace("]", "")
                                        .toLowerCase().trim();
            if (cleanTag.equals(cleanInput)) {
                return c;
            }
        }
        return null;
    }

    public Clan getClanById(int id) {
        return clansById.get(id);
    }

    public Clan getPlayerClan(UUID playerId) {
        String tag = playerClanCache.get(playerId);
        return tag != null ? clansByTag.get(tag) : null;
    }

    public boolean hasPlayerClan(UUID playerId) {
        return playerClanCache.containsKey(playerId);
    }

    public Map<UUID, String> getPlayerClanCache() {
        return playerClanCache;
    }

    public List<Clan> getTopClansByGold(int limit) {
        return clansByTag.values().stream()
            .sorted((c1, c2) -> Long.compare(c2.getGold(), c1.getGold()))
            .limit(limit)
            .toList();
    }

    public List<Clan> getTopClansByPvpKills(int limit) {
        return clansByTag.values().stream()
            .sorted((c1, c2) -> Integer.compare(c2.getPvpKills(), c1.getPvpKills()))
            .limit(limit)
            .toList();
    }

    public Map<UUID, String> getPendingInvites() {
        return pendingInvites;
    }

    public void addInvite(UUID playerId, String clanTag) {
        pendingInvites.put(playerId, clanTag);
    }

    public void removeInvite(UUID playerId) {
        pendingInvites.remove(playerId);
    }

    public String getInvite(UUID playerId) {
        return pendingInvites.get(playerId);
    }

    public boolean hasInvite(UUID playerId) {
        return pendingInvites.containsKey(playerId);
    }
}
