package com.irdem.tunama.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AchievementManager {

    private Map<String, Achievement> achievements;
    private JavaPlugin plugin;
    private File achievementsFolder;

    public AchievementManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.achievements = new HashMap<>();
        this.achievementsFolder = new File(plugin.getDataFolder(), "logros");
        
        if (!achievementsFolder.exists()) {
            achievementsFolder.mkdirs();
        }
        
        loadAchievements();
    }

    public void loadAchievements() {
        achievements.clear();
        
        File[] files = achievementsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No se encontraron archivos de logros. Creando logros por defecto...");
            createDefaultAchievements();
            // Recargar los archivos recién creados
            files = achievementsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files == null || files.length == 0) return;
        }
        
        for (File file : files) {
            loadAchievementFromFile(file);
        }
        
        plugin.getLogger().info("Se cargaron " + achievements.size() + " logros correctamente");
    }

    private void loadAchievementFromFile(File file) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = config.getString("id");
            String name = config.getString("name");
            String description = config.getString("description");
            
            if (id == null || name == null) {
                plugin.getLogger().warning("Archivo de logro incompleto: " + file.getName());
                return;
            }
            
            Achievement achievement = new Achievement(id, name, description);
            achievement.setCategory(config.getString("category", "General"));
            achievement.setRequirement(config.getInt("requirement", 0));
            
            achievements.put(id, achievement);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al cargar logro desde " + file.getName() + ": " + e.getMessage());
        }
    }

    private void createDefaultAchievements() {
        createAchievementFile("primer-paso", "Primer Paso", "Alcanza nivel 5", "Nivel", 5);
        createAchievementFile("crecimiento", "Crecimiento", "Alcanza nivel 10", "Nivel", 10);
        createAchievementFile("guerrero", "Guerrero", "Alcanza nivel 15", "Nivel", 15);
        createAchievementFile("veterano", "Veterano", "Alcanza nivel 20", "Nivel", 20);
        createAchievementFile("maestro", "Maestro", "Alcanza nivel 30", "Nivel", 30);
        createAchievementFile("legenda", "Legenda", "Alcanza nivel 50", "Nivel", 50);
        createAchievementFile("elegido-razas", "Elegido de Razas", "Selecciona una raza", "Selección", 1);
        createAchievementFile("profesional", "Profesional", "Selecciona una clase", "Selección", 1);
        createAchievementFile("especialista", "Especialista", "Selecciona una subclase", "Selección", 1);
    }

    private void createAchievementFile(String id, String name, String description, 
                                       String category, int requirement) {
        try {
            File file = new File(achievementsFolder, id + ".yml");
            if (file.exists()) return;
            
            FileConfiguration config = new YamlConfiguration();
            config.set("id", id);
            config.set("name", name);
            config.set("description", description);
            config.set("category", category);
            config.set("requirement", requirement);
            
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al crear archivo de logro: " + e.getMessage());
        }
    }

    public Achievement getAchievement(String id) {
        return achievements.get(id.toLowerCase());
    }

    public boolean isValidAchievement(String id) {
        return achievements.containsKey(id.toLowerCase());
    }

    public Map<String, Achievement> getAllAchievements() {
        return new HashMap<>(achievements);
    }
}
