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
        
        File[] files = abilitiesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No se encontraron archivos de habilidades. Creando habilidades por defecto...");
            createDefaultAbilities();
            return;
        }
        
        for (File file : files) {
            loadAbilityFromFile(file);
        }
        
        plugin.getLogger().info("Se cargaron " + abilities.size() + " habilidades correctamente");
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
            
            abilities.put(id, ability);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al cargar habilidad desde " + file.getName() + ": " + e.getMessage());
        }
    }

    private void createDefaultAbilities() {
        // Habilidades de Guerrero
        createAbilityFile("tajazo-brutal", "Tajo Brutal", 
            "Realiza un poderoso tajo que causa gran daño", "guerrero", 1, "20");
        
        createAbilityFile("escudo-de-hierro", "Escudo de Hierro", 
            "Aumenta tu defensa temporalmente", "guerrero", 5, "30");
        
        // Habilidades de Mago
        createAbilityFile("bola-fuego", "Bola de Fuego", 
            "Lanza una bola de fuego que daña a los enemigos", "mago", 1, "25");
        
        createAbilityFile("barrera-magica", "Barrera Mágica", 
            "Crea una barrera que protege contra ataques mágicos", "mago", 10, "35");
        
        // Habilidades de Arquero
        createAbilityFile("disparo-critico", "Disparo Crítico", 
            "Realiza un disparo que tiene alto chance de crítico", "arquero", 1, "15");
        
        createAbilityFile("lluvia-flechas", "Lluvia de Flechas", 
            "Lanza múltiples flechas al cielo", "arquero", 15, "40");
        
        // Habilidades de Pícaro
        createAbilityFile("golpe-sigiloso", "Golpe Sigiloso", 
            "Ataca desde las sombras causando daño extra", "picaro", 1, "20");
        
        createAbilityFile("evasion-total", "Evasión Total", 
            "Evita todos los ataques durante un tiempo", "picaro", 12, "45");
        
        // Habilidades de Sacerdote
        createAbilityFile("curacion-sagrada", "Curación Sagrada", 
            "Cura a ti mismo o a un aliado", "sacerdote", 1, "30");
        
        createAbilityFile("bendicion-divina", "Bendición Divina", 
            "Bendice a tu equipo con poder sagrado", "sacerdote", 20, "50");
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
