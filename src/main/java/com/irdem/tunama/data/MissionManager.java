package com.irdem.tunama.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MissionManager {

    private Map<String, Mission> missions;
    private JavaPlugin plugin;
    private File missionsFolder;

    public MissionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.missions = new HashMap<>();
        this.missionsFolder = new File(plugin.getDataFolder(), "misiones");
        
        if (!missionsFolder.exists()) {
            missionsFolder.mkdirs();
        }
        
        loadMissions();
    }

    public void loadMissions() {
        missions.clear();
        
        File[] files = missionsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No se encontraron archivos de misiones. Creando misiones por defecto...");
            createDefaultMissions();
            // Recargar los archivos recién creados
            files = missionsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files == null || files.length == 0) return;
        }
        
        for (File file : files) {
            loadMissionFromFile(file);
        }
        
        plugin.getLogger().info("Se cargaron " + missions.size() + " misiones correctamente");
    }

    private void loadMissionFromFile(File file) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = config.getString("id");
            String name = config.getString("name");
            String description = config.getString("description");
            
            if (id == null || name == null) {
                plugin.getLogger().warning("Archivo de misión incompleto: " + file.getName());
                return;
            }
            
            Mission mission = new Mission(id, name, description);
            mission.setRequiredLevel(config.getInt("required-level", 1));
            mission.setRewardExp(config.getInt("reward-exp", 0));
            mission.setRewardItem(config.getString("reward-item"));
            
            missions.put(id, mission);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al cargar misión desde " + file.getName() + ": " + e.getMessage());
        }
    }

    private void createDefaultMissions() {
        createMissionFile("derrota-5-zombies", "Derrota 5 Zombies", 
            "Derrota 5 zombies para ganar experiencia", 1, 50, null);
        
        createMissionFile("recauda-10-cristales", "Recauda 10 Cristales", 
            "Recauda 10 cristales dispersos en el mundo", 10, 150, "DIAMOND");
        
        createMissionFile("derrota-jefe-esqueleto", "Derrota al Jefe Esqueleto", 
            "Derrota al jefe esqueleto en el cementerio", 20, 500, "DIAMOND_SWORD");
        
        createMissionFile("derrota-señor-caos", "Derrota al Señor del Caos", 
            "Derrota al Señor del Caos en el Nether", 30, 2000, "NETHERITE_SWORD");
        
        createMissionFile("derrota-titan-oscuro", "Derrota al Titán Oscuro", 
            "Derrota al Titán Oscuro en el End", 50, 5000, "NETHERITE_PICKAXE");
    }

    private void createMissionFile(String id, String name, String description, 
                                   int requiredLevel, int rewardExp, String rewardItem) {
        try {
            File file = new File(missionsFolder, id + ".yml");
            if (file.exists()) return;
            
            FileConfiguration config = new YamlConfiguration();
            config.set("id", id);
            config.set("name", name);
            config.set("description", description);
            config.set("required-level", requiredLevel);
            config.set("reward-exp", rewardExp);
            config.set("reward-item", rewardItem);
            
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al crear archivo de misión: " + e.getMessage());
        }
    }

    public Mission getMission(String id) {
        return missions.get(id.toLowerCase());
    }

    public boolean isValidMission(String id) {
        return missions.containsKey(id.toLowerCase());
    }

    public Map<String, Mission> getAllMissions() {
        return new HashMap<>(missions);
    }
}
