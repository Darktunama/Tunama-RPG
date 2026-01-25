package com.irdem.tunama.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class RaceManager {

    private Map<String, Race> races;
    private JavaPlugin plugin;
    private File racesFolder;

    public RaceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.races = new HashMap<>();
        this.racesFolder = new File(plugin.getDataFolder(), "razas");
        
        // Crear carpeta si no existe
        if (!racesFolder.exists()) {
            racesFolder.mkdirs();
        }
        
        loadRaces();
    }

    public void loadRaces() {
        races.clear();
        
        File[] files = racesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No se encontraron archivos de razas. Creando razas por defecto...");
            createDefaultRaces();
            return;
        }
        
        for (File file : files) {
            loadRaceFromFile(file);
        }
        
        plugin.getLogger().info("Se cargaron " + races.size() + " razas correctamente");
    }

    private void loadRaceFromFile(File file) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = config.getString("id");
            String name = config.getString("name");
            String description = config.getString("description");
            
            if (id == null || name == null) {
                plugin.getLogger().warning("Archivo de raza incompleto: " + file.getName());
                return;
            }
            
            Race race = new Race(id, name, description);
            race.setAdvantages(config.getString("advantages", "Sin ventajas"));
            race.setDisadvantages(config.getString("disadvantages", "Sin desventajas"));
            race.setAgilityMultiplier(config.getDouble("multipliers.agility", 1.0));
            race.setIntelligenceMultiplier(config.getDouble("multipliers.intelligence", 1.0));
            race.setLifeMultiplier(config.getDouble("multipliers.life", 1.0));
            race.setStrengthMultiplier(config.getDouble("multipliers.strength", 1.0));
            race.setCorruptPowerCost(config.getDouble("costs.corrupt-power", 1.0));
            race.setSagradoPowerCost(config.getDouble("costs.sagrado-power", 1.0));
            race.setNaturePowerCost(config.getDouble("costs.nature-power", 1.0));
            
            races.put(id, race);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al cargar raza desde " + file.getName() + ": " + e.getMessage());
        }
    }

    private void createDefaultRaces() {
        createRaceFile("humano", "Humano", "Raza Versátil pero sin destacar en nada",
            "Puede usar todas las clases excepto Evocador y Druida",
            "No tiene bonus de Estadísticas");
        
        createRaceFile("elfo", "Elfo", "Raza débil en vitalidad pero de alto daño",
            "Cada punto invertido en Agilidad o Inteligencia suma 2 en vez de 1",
            "Para subir 1 punto de Poder Corrupto requiere 2 puntos");
        
        createRaceFile("semielfo", "SemiElfo", "Es lo que pasa cuando un Elfo y un Humano se juntan",
            "Puede usar todas las clases excepto Trampero, Evocador, Monje y Nigromante",
            "Cada punto invertido en Agilidad o Inteligencia suma 2 en vez de 1");
        
        loadRaces();
    }

    private void createRaceFile(String id, String name, String description, String advantages, String disadvantages) {
        try {
            File file = new File(racesFolder, id + ".yml");
            if (!file.exists()) {
                FileConfiguration config = new YamlConfiguration();
                config.set("id", id);
                config.set("name", name);
                config.set("description", description);
                config.set("advantages", advantages);
                config.set("disadvantages", disadvantages);
                config.set("multipliers.agility", 1.0);
                config.set("multipliers.intelligence", 1.0);
                config.set("multipliers.life", 1.0);
                config.set("multipliers.strength", 1.0);
                config.set("costs.corrupt-power", 1.0);
                config.set("costs.sagrado-power", 1.0);
                config.set("costs.nature-power", 1.0);
                config.save(file);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error al crear archivo de raza: " + e.getMessage());
        }
    }

    public Race getRace(String id) {
        return races.get(id.toLowerCase());
    }

    public Map<String, Race> getAllRaces() {
        return new HashMap<>(races);
    }

    public boolean isValidRace(String id) {
        return races.containsKey(id.toLowerCase());
    }
}
