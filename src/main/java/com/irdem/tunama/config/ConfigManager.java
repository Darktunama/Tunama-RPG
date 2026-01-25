package com.irdem.tunama.config;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

public class ConfigManager {

    private JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void createDefaultConfig() {
        config = new YamlConfiguration();
        
        // Configuración de Base de Datos
        config.set("database.type", "sqlite"); // sqlite o mysql
        config.set("database.sqlite.file", "plugins/TunameRPG/rpg.db");
        
        config.set("database.mysql.host", "localhost");
        config.set("database.mysql.port", 3306);
        config.set("database.mysql.database", "tunama_rpg");
        config.set("database.mysql.username", "root");
        config.set("database.mysql.password", "password");
        config.set("database.mysql.useSSL", false);
        
        // Configuración del Plugin
        config.set("plugin.debug", false);
        config.set("plugin.language", "es");
        
        // Configuración de Experiencia
        config.set("experience.base-multiplier", 1.0);
        config.set("experience.quest-multiplier", 1.5);
        config.set("experience.achievement-multiplier", 1.2);
        
        // Configuración de Estadísticas
        config.set("stats.life-per-level", 5);
        config.set("stats.mana-per-level", 3);
        config.set("stats.strength-per-level", 2);
        config.set("stats.agility-per-level", 2);
        config.set("stats.intelligence-per-level", 2);
        
        // Configuración de Clanes
        config.set("clans.min-members", 2);
        config.set("clans.max-members", 50);
        config.set("clans.creation-cost", 1000);
        
        // Compatibilidad con plugins
        config.set("plugins.vault.enabled", true);
        config.set("plugins.essentials.enabled", true);
        
        saveConfig();
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("No se pudo guardar la configuración: " + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }

    public String getSQLitePath() {
        return config.getString("database.sqlite.file", "plugins/TunameRPG/rpg.db");
    }

    public String getMySQLHost() {
        return config.getString("database.mysql.host", "localhost");
    }

    public int getMySQLPort() {
        return config.getInt("database.mysql.port", 3306);
    }

    public String getMySQLDatabase() {
        return config.getString("database.mysql.database", "tunama_rpg");
    }

    public String getMySQLUsername() {
        return config.getString("database.mysql.username", "root");
    }

    public String getMySQLPassword() {
        return config.getString("database.mysql.password", "password");
    }

    public boolean isMySQLSSL() {
        return config.getBoolean("database.mysql.useSSL", false);
    }
}
