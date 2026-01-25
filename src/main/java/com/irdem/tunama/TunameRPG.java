package com.irdem.tunama;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import com.irdem.tunama.config.ConfigManager;
import com.irdem.tunama.database.DatabaseManager;
import com.irdem.tunama.commands.RPGCommand;
import com.irdem.tunama.listeners.PlayerListener;
import com.irdem.tunama.data.RaceManager;
import com.irdem.tunama.data.ClassManager;
import com.irdem.tunama.data.SubclassManager;

public class TunameRPG extends JavaPlugin {

    private static TunameRPG instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private RaceManager raceManager;
    private ClassManager classManager;
    private SubclassManager subclassManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Crear carpetas del plugin
        createDirectories();
        
        // Cargar configuración
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Inicializar base de datos
        databaseManager = new DatabaseManager(this, configManager);
        if (!databaseManager.connect()) {
            getLogger().severe("No se pudo conectar a la base de datos!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Cargar datos de razas, clases y subclases
        raceManager = new RaceManager(this);
        classManager = new ClassManager(this);
        subclassManager = new SubclassManager(this);
        
        // Registrar comandos
        registerCommands();
        
        // Registrar listeners
        registerListeners();
        
        getLogger().info("================================");
        getLogger().info("TunameRPG v" + getDescription().getVersion() + " ha sido activado!");
        getLogger().info("================================");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("TunameRPG ha sido desactivado!");
    }

    private void createDirectories() {
        String[] directories = {
            "clases",
            "razas",
            "subclases",
            "habilidades",
            "misiones",
            "logros"
        };
        
        for (String dir : directories) {
            java.io.File folder = new java.io.File(getDataFolder(), dir);
            if (!folder.exists()) {
                folder.mkdirs();
            }
        }
    }

    private void registerCommands() {
        getCommand("rpg").setExecutor(new RPGCommand(this));
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);
    }

    public static TunameRPG getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public RaceManager getRaceManager() {
        return raceManager;
    }

    public ClassManager getClassManager() {
        return classManager;
    }

    public SubclassManager getSubclassManager() {
        return subclassManager;
    }
}
