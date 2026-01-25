package com.irdem.tunama.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassManager {

    private Map<String, RPGClass> classes;
    private JavaPlugin plugin;
    private File classesFolder;

    public ClassManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.classes = new HashMap<>();
        this.classesFolder = new File(plugin.getDataFolder(), "clases");
        
        // Crear carpeta si no existe
        if (!classesFolder.exists()) {
            classesFolder.mkdirs();
        }
        
        loadClasses();
    }

    public void loadClasses() {
        classes.clear();
        
        File[] files = classesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No se encontraron archivos de clases. Creando clases por defecto...");
            createDefaultClasses();
            return;
        }
        
        for (File file : files) {
            loadClassFromFile(file);
        }
        
        plugin.getLogger().info("Se cargaron " + classes.size() + " clases correctamente");
    }

    private void loadClassFromFile(File file) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = config.getString("id");
            String name = config.getString("name");
            String description = config.getString("description");
            
            if (id == null || name == null) {
                plugin.getLogger().warning("Archivo de clase incompleto: " + file.getName());
                return;
            }
            
            RPGClass rpgClass = new RPGClass(id, name, description);
            rpgClass.setAdvantages(config.getString("advantages", "Sin ventajas"));
            rpgClass.setDisadvantages(config.getString("disadvantages", "Sin desventajas"));
            
            List<String> subclasses = config.getStringList("subclasses");
            for (String subclass : subclasses) {
                rpgClass.addSubclass(subclass);
            }
            
            classes.put(id, rpgClass);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al cargar clase desde " + file.getName() + ": " + e.getMessage());
        }
    }

    private void createDefaultClasses() {
        createClassFile("guerrero", "Guerrero", "Un maestro en el arte del combate con alta defensa",
            "Buena defensa y vida, fuerte daño a melé, puede absorber una parte del daño",
            "Movimiento lento, débil en habilidades de rango",
            java.util.Arrays.asList("bersker", "maestro-de-armas"));
        
        createClassFile("monje", "Monje", "Un luchador experimentado con mucha velocidad",
            "Movimiento alto, fuerte daño a melé, puede stunear enemigos",
            "Defensa baja, débil en habilidades de rango",
            java.util.Arrays.asList("shaolin", "maestro-zen"));
        
        createClassFile("mago", "Mago", "Poderoso hechicero con alto daño mágico",
            "Alto daño mágico, variedad de hechizos, ataca a distancia",
            "Baja defensa, débil en combate cuerpo a cuerpo",
            java.util.Arrays.asList("elementalista", "mago-de-combate"));
        
        loadClasses();
    }

    private void createClassFile(String id, String name, String description, 
                                String advantages, String disadvantages, List<String> subclasses) {
        try {
            File file = new File(classesFolder, id + ".yml");
            if (!file.exists()) {
                FileConfiguration config = new YamlConfiguration();
                config.set("id", id);
                config.set("name", name);
                config.set("description", description);
                config.set("advantages", advantages);
                config.set("disadvantages", disadvantages);
                config.set("subclasses", subclasses);
                config.save(file);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error al crear archivo de clase: " + e.getMessage());
        }
    }

    public RPGClass getClass(String id) {
        return classes.get(id.toLowerCase());
    }

    public Map<String, RPGClass> getAllClasses() {
        return new HashMap<>(classes);
    }

    public boolean isValidClass(String id) {
        return classes.containsKey(id.toLowerCase());
    }
}
