package com.irdem.tunama.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AbilityManager {

    private Map<String, Ability> abilities;
    private JavaPlugin plugin;
    private File abilitiesFolder;

    public AbilityManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.abilities = new HashMap<>();
        this.abilitiesFolder = new File(plugin.getDataFolder(), "habilidades");
        
        if (!abilitiesFolder.exists()) {
            abilitiesFolder.mkdirs();
        }
        
        loadAbilities();
    }

    public void loadAbilities() {
        abilities.clear();

        // Copiar habilidades desde resources si no existen en la carpeta de datos
        copyDefaultAbilityFiles();

        // Cargar habilidades de la carpeta principal
        File[] files = abilitiesFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No se encontraron archivos de habilidades. Creando habilidades por defecto...");
            createDefaultAbilities();
            return;
        }

        for (File file : files) {
            loadAbilityFromFile(file);
        }

        // Cargar habilidades de la subcarpeta habilidades-extra
        File extraFolder = new File(abilitiesFolder, "habilidades-extra");
        if (extraFolder.exists() && extraFolder.isDirectory()) {
            File[] extraFiles = extraFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (extraFiles != null) {
                for (File file : extraFiles) {
                    loadAbilityFromFile(file);
                }
            }
        }

        plugin.getLogger().info("Se cargaron " + abilities.size() + " habilidades correctamente");
    }

    /**
     * Copia los archivos de habilidades desde resources si no existen en la carpeta de datos
     */
    private void copyDefaultAbilityFiles() {
        // Lista de habilidades principales (en carpeta habilidades/)
        String[] defaultAbilities = {
            // Habilidades de transformación
            "forma-de-lobo.yml", "forma-de-oso.yml", "forma-de-arana.yml",
            "forma-de-zorro.yml", "forma-de-panda.yml", "forma-de-warden.yml",
            // Habilidades de mascotas
            "orden-de-ataque.yml", "cura-animal.yml", "resucitar-mascota.yml",
            "rabia-animal.yml", "potencia-de-la-manada.yml",
            "golpe-sombras-animales.yml", "manada-necrotica.yml",
            "segunda-mascota.yml",
            // Habilidades de arquero
            "flecha-rapida.yml", "flecha-cargada.yml", "flecha-negra.yml",
            "flecha-penetrante.yml", "flecha-rebotante.yml", "lluvia-flechas.yml",
            "multi-disparo.yml", "disparo-al-corazon.yml",
            // Habilidades de druida
            "cura-natural.yml", "fuerza-de-la-naturaleza.yml",
            // Habilidades de evocador
            "llama-de-los-dragones.yml", "llama-interior.yml", "llama-viva.yml",
            "vuelo-del-dragon.yml", "llama-bailarina.yml", "rugido-del-dragon.yml",
            "rayo-dragones-ancestrales.yml", "llamada-ultimo-dragon.yml",
            // Habilidades de guerrero
            "corte-profundo.yml", "embestida.yml", "romper-corazas.yml",
            "atronar.yml", "sed-de-sangre.yml", "torbellino-sangriento.yml",
            "ejecutar.yml", "ira-furibunda.yml"
        };

        // Lista de habilidades de ataques de formas (en subcarpeta habilidades-extra/)
        String[] extraAbilities = {
            "revertir-forma.yml",
            // Lobo
            "zarpazo-lobo.yml", "mordisco-infectado.yml", "aullido-de-manada.yml",
            // Oso
            "zarpazo-oso.yml", "mordisco-oso.yml", "golpe-pesado.yml", "rabia-de-oso.yml",
            // Araña
            "mordisco-arana.yml", "telarana.yml", "veneno-arana.yml",
            // Zorro
            "zarpazo-zorro.yml", "mordisco-zorro.yml", "esquivar-zorro.yml",
            // Panda
            "zarpazo-panda.yml", "mordisco-panda.yml", "golpe-pesado-panda.yml",
            // Warden
            "grito-sonico.yml", "onda-de-choque.yml", "sentido-vibracion.yml"
        };

        // Copiar habilidades principales
        for (String fileName : defaultAbilities) {
            try {
                plugin.saveResource("habilidades/" + fileName, true);
            } catch (Exception e) {
                // Archivo no existe en resources, ignorar
            }
        }

        // Crear carpeta habilidades-extra si no existe
        File extraFolder = new File(abilitiesFolder, "habilidades-extra");
        if (!extraFolder.exists()) {
            extraFolder.mkdirs();
        }

        // Copiar habilidades de formas a la subcarpeta
        for (String fileName : extraAbilities) {
            try {
                plugin.saveResource("habilidades/habilidades-extra/" + fileName, true);
            } catch (Exception e) {
                // Archivo no existe en resources, ignorar
            }
        }
    }

    private void loadAbilityFromFile(File file) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = config.getString("id");
            String name = config.getString("name");
            String description = config.getString("description");
            
            if (id == null || name == null) {
                plugin.getLogger().warning("Archivo de habilidad incompleto: " + file.getName());
                return;
            }
            
            Ability ability = new Ability(id, name, description);
            ability.setRpgClass(config.getString("class", "General"));
            ability.setRequiredLevel(config.getInt("required-level", 1));
            ability.setManaCost(config.getString("mana-cost", "0"));
            ability.setMaterial(config.getString("material", "BLAZE_ROD"));
            ability.setCustomModelData(config.getInt("custom-model-data", 0));
            ability.setParticle(config.getString("particle", ""));
            ability.setParticleCount(config.getInt("particle-count", 10));
            ability.setParticleSpeed(config.getDouble("particle-speed", 0.5));
            ability.setRequiredWeapon(config.getString("required-weapon", ""));
            ability.setRange(config.getDouble("range", 0));
            ability.setAreaOfEffect(config.getDouble("area-of-effect", 0));
            ability.setCastTime(config.getDouble("cast-time", 0));
            ability.setCastMode(config.getString("cast-mode", "mobile"));
            ability.setCooldown(config.getDouble("cooldown", 0));
            ability.setArmorPenetration(config.getDouble("armor-penetration", 0));
            ability.setCritBonus(config.getDouble("crit-bonus", 0));
            ability.setCritDuration(config.getDouble("crit-duration", 0));
            ability.setPassive(config.getBoolean("passive", false));
            ability.setFormOnly(config.getBoolean("form-only", false));

            // Cargar damage-scaling
            if (config.isConfigurationSection("damage-scaling")) {
                Map<String, Double> scaling = new HashMap<>();
                for (String key : config.getConfigurationSection("damage-scaling").getKeys(false)) {
                    scaling.put(key, config.getDouble("damage-scaling." + key, 0.0));
                }
                ability.setDamageScaling(scaling);
            }

            // Cargar propiedades personalizadas para habilidades de mascotas
            if (config.contains("heal-percent")) {
                ability.setCustomProperty("heal-percent", config.getDouble("heal-percent", 30.0));
            }
            if (config.contains("resurrect-percent")) {
                ability.setCustomProperty("resurrect-percent", config.getDouble("resurrect-percent", 50.0));
            }
            if (config.contains("damage-bonus")) {
                ability.setCustomProperty("damage-bonus", config.getDouble("damage-bonus", 50.0));
            }
            if (config.contains("damage-bonus-per-pet")) {
                ability.setCustomProperty("damage-bonus-per-pet", config.getDouble("damage-bonus-per-pet", 10.0));
            }
            if (config.contains("buff-duration")) {
                ability.setCustomProperty("buff-duration", config.getDouble("buff-duration", 30.0));
            }

            // Cargar propiedades de curación
            if (config.contains("base-heal")) {
                ability.setCustomProperty("base-heal", config.getDouble("base-heal", 10.0));
            }
            if (config.contains("intelligence-scaling")) {
                ability.setCustomProperty("intelligence-scaling", config.getDouble("intelligence-scaling", 0.003));
            }
            if (config.contains("nature-scaling")) {
                ability.setCustomProperty("nature-scaling", config.getDouble("nature-scaling", 0.002));
            }

            abilities.put(id, ability);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al cargar habilidad desde " + file.getName() + ": " + e.getMessage());
        }
    }

    private void createDefaultAbilities() {
        // Habilidades de Mago
        createAbilityFile("bola-fuego", "Bola de Fuego", 
            "Lanza una bola de fuego que daña a los enemigos", "mago", 1, "25");
        
        // Habilidades de Arquero
        createAbilityFile("disparo-critico", "Disparo Crítico",
            "Realiza un disparo que tiene alto chance de crítico", "arquero", 1, "15");

        createAbilityFile("lluvia-flechas", "Lluvia de Flechas",
            "Lanza múltiples flechas al cielo", "arquero", 15, "40");
    }

    private void createAbilityFile(String id, String name, String description, 
                                   String rpgClass, int requiredLevel, String manaCost) {
        try {
            File file = new File(abilitiesFolder, id + ".yml");
            if (file.exists()) return;
            
            FileConfiguration config = new YamlConfiguration();
            config.set("id", id);
            config.set("name", name);
            config.set("description", description);
            config.set("class", rpgClass);
            config.set("required-level", requiredLevel);
            config.set("mana-cost", manaCost);
            
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al crear archivo de habilidad: " + e.getMessage());
        }
    }

    public Ability getAbility(String id) {
        return abilities.get(id.toLowerCase());
    }

    public boolean isValidAbility(String id) {
        return abilities.containsKey(id.toLowerCase());
    }

    public Map<String, Ability> getAllAbilities() {
        return new HashMap<>(abilities);
    }

    public Map<String, Ability> getAbilitiesByClass(String rpgClass) {
        Map<String, Ability> classAbilities = new HashMap<>();
        for (Ability ability : abilities.values()) {
            if (ability.getRpgClass().equalsIgnoreCase(rpgClass)) {
                classAbilities.put(ability.getId(), ability);
            }
        }
        return classAbilities;
    }
}
