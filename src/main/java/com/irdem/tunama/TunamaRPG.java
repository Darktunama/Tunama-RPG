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
import com.irdem.tunama.data.MissionManager;
import com.irdem.tunama.data.AchievementManager;
import com.irdem.tunama.data.AbilityManager;
import com.irdem.tunama.data.ExperienceManager;
import com.irdem.tunama.managers.BannedNamesManager;
import com.irdem.tunama.managers.PlayerBarsManager;
import com.irdem.tunama.managers.ClanManager;
import com.irdem.tunama.managers.ClanBankManager;
import com.irdem.tunama.managers.PetManager;
import com.irdem.tunama.managers.TransformationManager;
import com.irdem.tunama.listeners.AnvilInputListener;
import com.irdem.tunama.listeners.ChatInputListener;
import com.irdem.tunama.listeners.PetListener;
import com.irdem.tunama.listeners.PetMenuListener;

public class TunamaRPG extends JavaPlugin {

    private static TunamaRPG instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private RaceManager raceManager;
    private ClassManager classManager;
    private SubclassManager subclassManager;
    private MissionManager missionManager;
    private AchievementManager achievementManager;
    private AbilityManager abilityManager;
    private ExperienceManager experienceManager;
    private BannedNamesManager bannedNamesManager;
    private AnvilInputListener anvilInputListener;
    private ChatInputListener chatInputListener;
    private PlayerBarsManager playerBarsManager;
    private ClanManager clanManager;
    private ClanBankManager clanBankManager;
    private com.irdem.tunama.economy.EconomyManager economyManager;
    private com.irdem.tunama.managers.ItemManager itemManager;
    private com.irdem.tunama.managers.ScoreboardManager scoreboardManager;
    private PetManager petManager;
    private TransformationManager transformationManager;

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
        missionManager = new MissionManager(this);
        achievementManager = new AchievementManager(this);
        abilityManager = new AbilityManager(this);
        experienceManager = new ExperienceManager(this);
        bannedNamesManager = new BannedNamesManager(this);
        playerBarsManager = new PlayerBarsManager(this);

        // Inicializar sistema de clanes
        clanManager = new ClanManager(this, databaseManager.getConnection());
        clanBankManager = new ClanBankManager(this);

        // Inicializar sistema de economía (Vault)
        economyManager = new com.irdem.tunama.economy.EconomyManager(this);
        economyManager.setupEconomy();

        // Inicializar sistema de objetos
        itemManager = new com.irdem.tunama.managers.ItemManager(this);
        itemManager.loadItems();

        // Inicializar sistema de scoreboard
        scoreboardManager = new com.irdem.tunama.managers.ScoreboardManager(this);

        // Inicializar sistema de mascotas
        petManager = new PetManager(this);

        // Inicializar sistema de transformaciones (Druida)
        transformationManager = new TransformationManager(this);

        // Registrar comandos
        registerCommands();

        // Registrar listeners
        registerListeners();

        // Registrar PlaceholderAPI si está disponible
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                com.irdem.tunama.placeholders.RPGPlaceholders placeholders = new com.irdem.tunama.placeholders.RPGPlaceholders(this);
                if (placeholders.register()) {
                    getLogger().info("PlaceholderAPI encontrado! Variables registradas correctamente.");
                    getLogger().info("Variables disponibles: %rpg_raza%, %rpg_clase%, %rpg_subclase%, %rpg_clan%, %rpg_clantag%, %rpg_nivel%, %rpg_experiencia%");
                    getLogger().info("Para EssentialsX Chat usar: {PLACEHOLDERAPI_rpg_clantag}");
                } else {
                    getLogger().warning("PlaceholderAPI encontrado pero no se pudo registrar la expansión.");
                }
            } catch (Exception e) {
                getLogger().warning("Error al registrar PlaceholderAPI: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            getLogger().info("PlaceholderAPI no encontrado. Las variables no estarán disponibles.");
        }

        // Limpiar invocaciones que persistan de sesiones anteriores
        cleanupOldSummons();

        getLogger().info("================================");
        getLogger().info("TunamaRPG v" + getDescription().getVersion() + " ha sido activado!");
        getLogger().info("================================");
    }

    /**
     * Elimina entidades invocadas que persistan de sesiones anteriores.
     * Los metadatos no sobreviven reinicios, pero las entidades sí.
     * Se usa PersistentDataContainer para identificarlas.
     */
    private void cleanupOldSummons() {
        org.bukkit.NamespacedKey summonKey = new org.bukkit.NamespacedKey(this, "rpg-summon");
        org.bukkit.NamespacedKey petKey = new org.bukkit.NamespacedKey(this, "rpg-pet");
        int removed = 0;
        for (org.bukkit.World world : getServer().getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity instanceof org.bukkit.entity.LivingEntity) {
                    org.bukkit.persistence.PersistentDataContainer pdc = entity.getPersistentDataContainer();
                    if (pdc.has(summonKey, org.bukkit.persistence.PersistentDataType.STRING)
                        || pdc.has(petKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                        entity.remove();
                        removed++;
                    }
                }
            }
        }
        if (removed > 0) {
            getLogger().info("Eliminadas " + removed + " invocaciones de sesiones anteriores.");
        }
    }

    @Override
    public void onDisable() {
        // Detener el task de recarga automática de experiencia
        if (experienceManager != null) {
            experienceManager.stopAutoReload();
        }

        // Detener el task de actualización de barras
        if (playerBarsManager != null) {
            playerBarsManager.stopUpdateTask();
        }

        // Detener el task del scoreboard
        if (scoreboardManager != null) {
            scoreboardManager.stopTask();
        }

        // Guardar y eliminar todas las mascotas activas antes de cerrar
        if (petManager != null) {
            petManager.dismissAllPetsOnShutdown();
            petManager.stopAITask();
        }

        // Revertir transformaciones activas
        if (transformationManager != null) {
            transformationManager.revertAllTransformations();
        }

        // Cerrar la base de datos del banco de clanes
        if (clanBankManager != null) {
            clanBankManager.close();
        }

        // Desconectar de la base de datos
        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        getLogger().info("TunamaRPG ha sido desactivado!");
    }

    private void createDirectories() {
        String[] directories = {
            "clases",
            "razas",
            "subclases",
            "habilidades",
            "scoreboards",
            "misiones",
            "logros",
            "objetos",
            "mascotas"
        };
        
        for (String dir : directories) {
            java.io.File folder = new java.io.File(getDataFolder(), dir);
            if (!folder.exists()) {
                folder.mkdirs();
            }
        }
        
        // Copiar archivos desde resources si no existen
        copyResourceIfNotExists("experiencia.yml", "experiencia.yml");
        copyResourceIfNotExists("niveles.yml", "niveles.yml");
        copyResourceIfNotExists("scoreboard-config.yml", "scoreboard-config.yml");
        copyResourceIfNotExists("scoreboards/lobby.yml", "scoreboards/lobby.yml");
        
        // Copiar archivos de objetos desde resources
        copyResourceIfNotExists("objetos/amuleto_guerrero.yml", "objetos/amuleto_guerrero.yml");
        copyResourceIfNotExists("objetos/amuleto_legendario.yml", "objetos/amuleto_legendario.yml");
        copyResourceIfNotExists("objetos/amuleto_maestro.yml", "objetos/amuleto_maestro.yml");
        copyResourceIfNotExists("objetos/amuleto_mago.yml", "objetos/amuleto_mago.yml");
        copyResourceIfNotExists("objetos/anillo_agilidad.yml", "objetos/anillo_agilidad.yml");
        copyResourceIfNotExists("objetos/anillo_corrupto.yml", "objetos/anillo_corrupto.yml");
        copyResourceIfNotExists("objetos/anillo_fuerza.yml", "objetos/anillo_fuerza.yml");
        copyResourceIfNotExists("objetos/anillo_inteligencia.yml", "objetos/anillo_inteligencia.yml");
        copyResourceIfNotExists("objetos/anillo_naturaleza.yml", "objetos/anillo_naturaleza.yml");
        copyResourceIfNotExists("objetos/anillo_proteccion.yml", "objetos/anillo_proteccion.yml");
        copyResourceIfNotExists("objetos/anillo_sagrado.yml", "objetos/anillo_sagrado.yml");
        copyResourceIfNotExists("objetos/anillo_vida.yml", "objetos/anillo_vida.yml");
        copyResourceIfNotExists("objetos/collar_equilibrio.yml", "objetos/collar_equilibrio.yml");
        copyResourceIfNotExists("objetos/collar_poder.yml", "objetos/collar_poder.yml");

        // Copiar archivos de subclases desde resources
        String[] subclassFiles = {
            "bersker", "maestro-de-armas", "shaolin", "maestro-zen",
            "elementalista", "mago-de-combate", "brujo", "chaman",
            "francotirador", "guardabosques", "asesino", "asaltante",
            "paladin-sagrado", "paladin-del-caos", "lich", "caballero-de-la-muerte",
            "licantropo", "archidruida", "salvaguarda", "destructor",
            "maestro-de-la-manada", "combatiente-primigenio", "primarca",
            "sacerdote-corrupto", "maestro-de-las-trampas", "ingeniero"
        };
        for (String subclass : subclassFiles) {
            copyResourceIfNotExists("subclases/" + subclass + ".yml", "subclases/" + subclass + ".yml");
        }

        // Copiar archivos de misiones desde resources
        String[] missionFiles = {
            "derrota-5-zombies", "recauda-10-cristales",
            "derrota-jefe-esqueleto", "derrota-senor-caos",
            "derrota-titan-oscuro"
        };
        for (String mission : missionFiles) {
            copyResourceIfNotExists("misiones/" + mission + ".yml", "misiones/" + mission + ".yml");
        }

        // Copiar archivos de logros desde resources
        String[] achievementFiles = {
            "primer-paso", "crecimiento", "guerrero",
            "veterano", "maestro", "legenda",
            "elegido-razas", "profesional", "especialista"
        };
        for (String achievement : achievementFiles) {
            copyResourceIfNotExists("logros/" + achievement + ".yml", "logros/" + achievement + ".yml");
        }

        // Copiar archivos de habilidades desde resources
        String[] abilityFiles = {
            // Guerrero
            "corte-profundo", "embestida", "romper-corazas", "atronar",
            "sed-de-sangre", "torbellino-sangriento", "ejecutar", "ira-furibunda",
            // Mago
            "bola-fuego",
            "pica-de-hielo", "implosion-arcana", "llamarada",
            "ventisca", "sifon-de-mana", "salto-dimensional", "elemento-antiguo",
            // Arquero
            "flecha-rapida", "flecha-cargada", "flecha-penetrante",
            "multi-disparo", "flecha-rebotante", "flecha-negra",
            "disparo-al-corazon",
            // Pícaro (lluvia-flechas legacy)
            "lluvia-flechas",
            // Cazador
            "orden-de-ataque", "cura-animal", "resucitar-mascota",
            "rabia-animal", "potencia-de-la-manada",
            "golpe-sombras-animales", "segunda-mascota", "manada-necrotica",
            // Druida
            "forma-de-arana", "forma-de-zorro", "cura-natural",
            "forma-de-panda", "forma-de-lobo", "forma-de-oso",
            "forma-de-warden", "fuerza-de-la-naturaleza",
            // Invocador
            "erupcion-de-fuego", "elemental-de-fuego", "trueno-primigenio",
            "elemental-de-aire", "maremoto", "elemental-de-agua",
            "vulcano", "elemental-de-tierra"
        };
        for (String ability : abilityFiles) {
            copyResourceIfNotExists("habilidades/" + ability + ".yml", "habilidades/" + ability + ".yml");
        }
    }
    
    private void copyResourceIfNotExists(String resourcePath, String targetPath) {
        java.io.File targetFile = new java.io.File(getDataFolder(), targetPath);
        if (!targetFile.exists()) {
            try {
                // Asegurar que el directorio padre existe
                java.io.File parentDir = targetFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                // Intentar copiar usando saveResource primero
                try {
                    saveResource(resourcePath, false);
                    getLogger().info("Archivo copiado desde resources: " + targetPath);
                } catch (Exception e) {
                    // Si saveResource falla, intentar copia manual desde el JAR
                    java.io.InputStream inputStream = getResource(resourcePath);
                    if (inputStream != null) {
                        java.io.FileOutputStream outputStream = new java.io.FileOutputStream(targetFile);
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                        outputStream.close();
                        inputStream.close();
                        getLogger().info("Archivo copiado manualmente desde resources: " + targetPath);
                    } else {
                        throw new Exception("No se encontró el recurso: " + resourcePath);
                    }
                }
            } catch (Exception e) {
                getLogger().warning("No se pudo copiar el archivo " + targetPath + " desde resources: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void registerCommands() {
        RPGCommand rpgCommand = new RPGCommand(this);
        getCommand("rpg").setExecutor(rpgCommand);
        getCommand("rpg").setTabCompleter(rpgCommand);
        getCommand("equipo").setExecutor(new com.irdem.tunama.commands.EquipmentCommand(this));
        getCommand("estadisticas").setExecutor(new com.irdem.tunama.commands.StatsCommand(this));
        getCommand("habilidades").setExecutor(new com.irdem.tunama.commands.AbilitiesCommand(this));

        // Registrar comando de clanes
        com.irdem.tunama.commands.ClanCommand clanCommand = new com.irdem.tunama.commands.ClanCommand(this);
        getCommand("clan").setExecutor(clanCommand);
        getCommand("clan").setTabCompleter(clanCommand);
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new com.irdem.tunama.listeners.PlayerMenuListener(this), this);
        pm.registerEvents(new com.irdem.tunama.listeners.MenuClickListener(this), this);
        pm.registerEvents(new com.irdem.tunama.listeners.PlayerInventoryListener(this), this);
        pm.registerEvents(new com.irdem.tunama.listeners.EquipmentMenuListener(this), this);

        // Inicializar y registrar el listener del yunque
        anvilInputListener = new AnvilInputListener(this);
        pm.registerEvents(anvilInputListener, this);

        // Inicializar y registrar el listener del chat
        chatInputListener = new ChatInputListener(this);
        pm.registerEvents(chatInputListener, this);

        // Registrar listener de menús de clanes
        pm.registerEvents(new com.irdem.tunama.listeners.ClanMenuListener(this), this);

        // Registrar listener de combate (kills de mobs y jugadores)
        pm.registerEvents(new com.irdem.tunama.listeners.CombatListener(this), this);

        // Registrar listener del menú de habilidades
        pm.registerEvents(new com.irdem.tunama.listeners.AbilitiesMenuListener(this), this);
        pm.registerEvents(new com.irdem.tunama.listeners.AbilityBarListener(this), this);

        // Registrar listener de chat del clan (tag con hover info)
        pm.registerEvents(new com.irdem.tunama.listeners.ClanChatListener(this), this);

        // Registrar listeners de mascotas
        pm.registerEvents(new PetListener(this), this);
        pm.registerEvents(new PetMenuListener(this), this);

        // Registrar listener de transformaciones (bloquea acciones durante transformación)
        pm.registerEvents(new com.irdem.tunama.listeners.TransformationListener(this), this);
    }

    public static TunamaRPG getInstance() {
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

    public MissionManager getMissionManager() {
        return missionManager;
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public ExperienceManager getExperienceManager() {
        return experienceManager;
    }

    public BannedNamesManager getBannedNamesManager() {
        return bannedNamesManager;
    }

    public AnvilInputListener getAnvilInputListener() {
        return anvilInputListener;
    }

    public ChatInputListener getChatInputListener() {
        return chatInputListener;
    }

    public PlayerBarsManager getPlayerBarsManager() {
        return playerBarsManager;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public ClanBankManager getClanBankManager() {
        return clanBankManager;
    }

    public com.irdem.tunama.economy.EconomyManager getEconomyManager() {
        return economyManager;
    }

    public com.irdem.tunama.managers.ItemManager getItemManager() {
        return itemManager;
    }

    public com.irdem.tunama.managers.ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public PetManager getPetManager() {
        return petManager;
    }

    public TransformationManager getTransformationManager() {
        return transformationManager;
    }
}
