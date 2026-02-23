package com.irdem.tunama.managers;

import com.irdem.tunama.TunamaRPG;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.*;

/**
 * Gestiona el banco del clan almacenando items en una base de datos SQLite separada
 */
public class ClanBankManager {
    private final TunamaRPG plugin;
    private Connection connection;

    public ClanBankManager(TunamaRPG plugin) {
        this.plugin = plugin;
        this.connection = null;
        initializeDatabase();
    }

    /**
     * Inicializa la base de datos SQLite para el banco de clanes
     */
    private void initializeDatabase() {
        try {
            // Crear el directorio de datos si no existe
            java.io.File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            // Crear conexión a la base de datos SQLite separada
            String dbPath = dataFolder.getAbsolutePath() + java.io.File.separator + "bancoclan.db";
            String url = "jdbc:sqlite:" + dbPath;

            connection = DriverManager.getConnection(url);

            // Crear tabla del banco si no existe
            createTable();

            plugin.getLogger().info("Base de datos del banco de clanes inicializada: bancoclan.db");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al inicializar base de datos del banco de clanes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Crea la tabla del banco de clanes
     */
    private void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS clan_bank (" +
                "clan_id INTEGER PRIMARY KEY," +
                "items BLOB" +
            ")");
        }
    }

    /**
     * Cierra la conexión a la base de datos
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Base de datos del banco de clanes cerrada");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al cerrar base de datos del banco de clanes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Guarda los items del banco del clan
     */
    public void saveBankItems(int clanId, ItemStack[] items) throws SQLException {
        byte[] serializedItems = serializeItems(items);

        String query = "INSERT OR REPLACE INTO clan_bank (clan_id, items) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clanId);
            stmt.setBytes(2, serializedItems);
            stmt.executeUpdate();
        }
    }

    /**
     * Carga los items del banco del clan
     */
    public ItemStack[] loadBankItems(int clanId) throws SQLException {
        String query = "SELECT items FROM clan_bank WHERE clan_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clanId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] serializedItems = rs.getBytes("items");
                    if (serializedItems != null) {
                        return deserializeItems(serializedItems);
                    }
                }
            }
        }

        // Si no hay datos, retornar array vacío de 270 slots (6 páginas de 45 slots cada una)
        return new ItemStack[270];
    }

    /**
     * Serializa un array de ItemStacks a bytes
     */
    private byte[] serializeItems(ItemStack[] items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }

            return outputStream.toByteArray();
        } catch (Exception e) {
            plugin.getLogger().severe("Error al serializar items del banco: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deserializa bytes a un array de ItemStacks
     */
    private ItemStack[] deserializeItems(byte[] data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            int length = dataInput.readInt();
            ItemStack[] items = new ItemStack[length];

            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            return items;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al deserializar items del banco: " + e.getMessage());
            e.printStackTrace();
            return new ItemStack[270];
        }
    }
}
