package com.irdem.tunama.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

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
            race.setOrder(config.getInt("order", 999)); // 999 por defecto para que aparezcan al final
            race.setRestrictedClasses(config.getStringList("restricted-classes"));

            races.put(id, race);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al cargar raza desde " + file.getName() + ": " + e.getMessage());
        }
    }

    private void createDefaultRaces() {
        // Humano
        createRaceFile("humano", "Humano", "Raza Versátil pero sin destacar en nada",
            "No tiene debilidades",
            "No tiene bonus de Estadísticas",
            1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
            java.util.Arrays.asList("evocador", "druida"));

        // Elfo
        createRaceFile("elfo", "Elfo", "Raza débil en vitalidad pero de alto daño",
            "Cada punto invertido en Agilidad o Inteligencia suma 2 en vez de 1",
            "Para subir 1 punto de Poder Corrupto requiere 2 puntos",
            2.0, 2.0, 1.0, 1.0, 2.0, 1.0, 1.0,
            java.util.Arrays.asList("trampero", "sacerdote", "evocador"));

        // SemiElfo
        createRaceFile("semielfo", "SemiElfo", "Es lo que pasa cuando un Elfo y un Humano se juntan",
            "Cada punto invertido en Agilidad o Inteligencia suma 2 en vez de 1",
            "Para subir 1 punto de Poder Corrupto requiere 2 puntos",
            2.0, 2.0, 1.0, 1.0, 2.0, 1.0, 1.0,
            java.util.Arrays.asList("trampero", "evocador", "monje", "nigromante"));

        // Orco
        createRaceFile("orco", "Orco", "Nadie donde sabe de donde vinieron estas bestias salvajes y joder que son feas",
            "Cada punto invertido en Vida o Fuerza suma 2 en vez de 1",
            "Para subir 1 punto de Agilidad, Poder Sagrado o Poder Naturaleza requiere 2 puntos",
            2.0, 1.0, 2.0, 2.0, 1.0, 2.0, 2.0,
            java.util.Arrays.asList("picaro", "evocador"));

        // Tiflyn
        createRaceFile("tiflyn", "Tiflyn", "Nacidos de humanos maldecidos por el Infierno",
            "Cada punto invertido en Vida, Fuerza, Agilidad o Poder Corrupto suma 2 en vez de 1",
            "Cada punto Invertido en Poder Sagrado o Poder Naturaleza requiere 2 puntos",
            2.0, 1.0, 2.0, 2.0, 2.0, 2.0, 2.0,
            java.util.Arrays.asList("monje", "sacerdote", "druida"));

        // Enano
        createRaceFile("enano", "Enano", "Raza muy Orgullosa, sobre todo por que le llega hasta las rodillas su ejem.... Su barba, su barba",
            "Cada punto Invertido en Vida y Fuerza suma 2 en vez de 1",
            "Cada punto invertido en Inteligencia o Agilidad requiere 2 puntos",
            2.0, 0.5, 2.0, 2.0, 1.0, 1.0, 1.0,
            java.util.Arrays.asList("cazador", "picaro", "druida", "evocador"));

        // Dragoneante
        createRaceFile("dragoneante", "Dragoneante", "Son los nacidos del Dragon",
            "Cada punto Invertido en Inteligencia, Agilidad o Poder Sagrado da 2 puntos",
            "Cada punto invertido en Vida o Poder Corrupto requiere 2 puntos",
            2.0, 2.0, 0.5, 1.0, 2.0, 2.0, 1.0,
            java.util.Arrays.asList("nigromante", "invocador", "paladin", "guerrero"));

        // Goblin
        createRaceFile("goblin", "Goblin", "Son grandes acaparadores de fortunas pero difíciles de tratar",
            "Cada punto Invertido en Agilidad, Inteligencia o Poder Corrupto dan 2 puntos",
            "Cada punto invertido en Vida o Poder Sagrado requiere 2 puntos",
            2.0, 2.0, 0.5, 1.0, 2.0, 2.0, 1.0,
            java.util.Arrays.asList("evocador", "paladin", "guerrero", "druida"));

        // No Muerto
        createRaceFile("nomuerto", "No Muerto", "La nunca los freno por eso siempre vuelven",
            "Cada punto Invertido en Vida o Poder Corrupto da 2 puntos",
            "Cada punto invertido en Agilidad o Poder Naturaleza requiere 2 puntos",
            0.5, 1.0, 2.0, 1.0, 2.0, 1.0, 2.0,
            java.util.Arrays.asList("evocador", "sacerdote", "monje"));

        loadRaces();
    }

    private void createRaceFile(String id, String name, String description, String advantages, String disadvantages,
                                double agility, double intelligence, double life, double strength,
                                double corruptPower, double sagradoPower, double naturePower, List<String> restrictedClasses) {
        try {
            File file = new File(racesFolder, id + ".yml");
            if (!file.exists()) {
                // Asignar un orden basado en el ID para mantener consistencia
                int order = 999; // Por defecto
                switch (id) {
                    case "humano": order = 1; break;
                    case "elfo": order = 2; break;
                    case "enano": order = 3; break;
                    case "orco": order = 4; break;
                    case "goblin": order = 5; break;
                    case "semielfo": order = 6; break;
                    case "tiflyn": order = 7; break;
                    case "dragoneante": order = 8; break;
                    case "nomuerto": order = 9; break;
                }

                FileConfiguration config = new YamlConfiguration();
                config.set("id", id);
                config.set("name", name);
                config.set("order", order);
                config.set("description", description);
                config.set("advantages", advantages);
                config.set("disadvantages", disadvantages);
                config.set("multipliers.agility", agility);
                config.set("multipliers.intelligence", intelligence);
                config.set("multipliers.life", life);
                config.set("multipliers.strength", strength);
                config.set("costs.corrupt-power", corruptPower);
                config.set("costs.sagrado-power", sagradoPower);
                config.set("costs.nature-power", naturePower);
                config.set("restricted-classes", restrictedClasses);
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
        // Ordenar las razas por el campo order
        return races.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(Race::getOrder)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public boolean isValidRace(String id) {
        return races.containsKey(id.toLowerCase());
    }
}
