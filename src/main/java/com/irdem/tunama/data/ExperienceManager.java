package com.irdem.tunama.data;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitTask;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ExperienceManager {

    private JavaPlugin plugin;
    private File mobsExperienceFile;
    private File levelsExperienceFile;
    private FileConfiguration mobsExperienceConfig;
    private FileConfiguration levelsExperienceConfig;
    private Map<EntityType, Integer> mobExperienceCache;
    private Map<Integer, Long> levelExperienceCache;
    private long lastMobsFileModified;
    private long lastLevelsFileModified;
    private BukkitTask autoReloadTask;

    public ExperienceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.mobExperienceCache = new HashMap<>();
        this.levelExperienceCache = new HashMap<>();
        this.lastMobsFileModified = 0;
        this.lastLevelsFileModified = 0;

        loadConfigs();
        startAutoReload();
    }
    
    public void loadConfigs() {
        // Cargar archivo de experiencia de mobs
        mobsExperienceFile = new File(plugin.getDataFolder(), "experiencia.yml");
        if (mobsExperienceFile.exists()) {
            mobsExperienceConfig = YamlConfiguration.loadConfiguration(mobsExperienceFile);
            lastMobsFileModified = mobsExperienceFile.lastModified();
            loadMobsExperience();
        } else {
            plugin.getLogger().warning("No se encontró el archivo experiencia.yml");
            mobsExperienceConfig = new YamlConfiguration();
        }
        
        // Cargar archivo de experiencia de niveles
        levelsExperienceFile = new File(plugin.getDataFolder(), "niveles.yml");
        if (levelsExperienceFile.exists()) {
            levelsExperienceConfig = YamlConfiguration.loadConfiguration(levelsExperienceFile);
            lastLevelsFileModified = levelsExperienceFile.lastModified();
            loadLevelsExperience();
        } else {
            plugin.getLogger().warning("No se encontró el archivo niveles.yml");
            levelsExperienceConfig = new YamlConfiguration();
        }
    }
    
    private void loadMobsExperience() {
        mobExperienceCache.clear();
        if (mobsExperienceConfig != null) {
            for (String key : mobsExperienceConfig.getKeys(false)) {
                try {
                    EntityType entityType = EntityType.valueOf(key.toUpperCase());
                    int experience = mobsExperienceConfig.getInt(key, 0);
                    mobExperienceCache.put(entityType, experience);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Tipo de entidad inválido en experiencia.yml: " + key);
                }
            }
        }
    }
    
    private void loadLevelsExperience() {
        levelExperienceCache.clear();
        if (levelsExperienceConfig != null) {
            for (String key : levelsExperienceConfig.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    long experience = levelsExperienceConfig.getLong(key, 0);
                    levelExperienceCache.put(level, experience);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Nivel inválido en niveles.yml: " + key);
                }
            }
        }
    }
    
    /**
     * Inicia el task automático de recarga cada 5 minutos
     */
    private void startAutoReload() {
        // Ejecutar cada 5 minutos (6000 ticks)
        autoReloadTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin,
            this::checkAndReload, 6000L, 6000L);
    }

    /**
     * Detiene el task automático de recarga
     */
    public void stopAutoReload() {
        if (autoReloadTask != null) {
            autoReloadTask.cancel();
        }
    }

    /**
     * Verifica si los archivos han sido modificados y los recarga si es necesario
     */
    private void checkAndReload() {
        boolean needsReload = false;
        
        // Verificar experiencia.yml
        if (mobsExperienceFile != null && mobsExperienceFile.exists()) {
            long currentModified = mobsExperienceFile.lastModified();
            if (currentModified > lastMobsFileModified) {
                plugin.getLogger().info("Detectado cambio en experiencia.yml, recargando...");
                mobsExperienceConfig = YamlConfiguration.loadConfiguration(mobsExperienceFile);
                lastMobsFileModified = currentModified;
                loadMobsExperience();
                needsReload = true;
            }
        }
        
        // Verificar niveles.yml
        if (levelsExperienceFile != null && levelsExperienceFile.exists()) {
            long currentModified = levelsExperienceFile.lastModified();
            if (currentModified > lastLevelsFileModified) {
                plugin.getLogger().info("Detectado cambio en niveles.yml, recargando...");
                levelsExperienceConfig = YamlConfiguration.loadConfiguration(levelsExperienceFile);
                lastLevelsFileModified = currentModified;
                loadLevelsExperience();
                needsReload = true;
            }
        }
        
        if (needsReload) {
            plugin.getLogger().info("Archivos de experiencia recargados correctamente");
        }
    }
    
    /**
     * Recarga forzadamente los archivos de configuración
     */
    public void reload() {
        loadConfigs();
        plugin.getLogger().info("Archivos de experiencia recargados manualmente");
    }
    
    /**
     * Obtiene la experiencia que otorga un mob al ser eliminado
     */
    public int getMobExperience(EntityType entityType) {
        return mobExperienceCache.getOrDefault(entityType, 0);
    }

    /**
     * Obtiene la experiencia requerida para alcanzar un nivel específico
     */
    public long getExperienceForLevel(int level) {
        return levelExperienceCache.getOrDefault(level, 0L);
    }

    /**
     * Calcula el nivel basado en la experiencia total
     */
    public int calculateLevel(long totalExperience) {
        int calculatedLevel = 1;

        for (Map.Entry<Integer, Long> entry : levelExperienceCache.entrySet()) {
            if (totalExperience >= entry.getValue() && entry.getKey() > calculatedLevel) {
                calculatedLevel = entry.getKey();
            }
        }

        return calculatedLevel;
    }

    /**
     * Obtiene la experiencia necesaria para el siguiente nivel
     */
    public long getExperienceForNextLevel(int currentLevel) {
        int nextLevel = currentLevel + 1;
        return getExperienceForLevel(nextLevel);
    }
    
    /**
     * Obtiene la experiencia restante necesaria para subir de nivel
     */
    public long getRemainingExperienceForNextLevel(int currentLevel, long currentExperience) {
        long nextLevelExp = getExperienceForNextLevel(currentLevel);
        return Math.max(0, nextLevelExp - currentExperience);
    }
}
