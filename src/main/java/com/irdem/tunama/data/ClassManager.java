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
        
        createClassFile("invocador", "Invocador", "Hechicero que invoca criaturas para que luchen por él",
            "Invoca criaturas, las criaturas pelean por ti, puede invocar varias criaturas",
            "Débil en habilidades melé, defensa baja, débil en habilidades de distancia",
            java.util.Arrays.asList("brujo", "chaman"));
        
        createClassFile("arquero", "Arquero", "Un preciso luchador a distancia con alta movilidad",
            "Alta velocidad de movimiento, daño fuerte a distancia, golpea a los enemigos a distancia",
            "Baja defensa y débil en habilidad a melé",
            java.util.Arrays.asList("francotirador", "guardabosques"));
        
        createClassFile("picaro", "Pícaro", "Luchador hábil con alta posibilidad de hacer golpes críticos",
            "Alta posibilidad de crítico, alta movilidad, hace más daño si golpea por la espalda",
            "Baja defensa y nula habilidad a distancia",
            java.util.Arrays.asList("asesino", "asaltante"));
        
        createClassFile("paladin", "Paladín", "Luchador balanceado en combate que puede curar tanto a sí mismo como a aliados",
            "Alta defensa y vida, curaciones y habilidades variadas",
            "Débil tanto en habilidades melé como distancia",
            java.util.Arrays.asList("paladin-sagrado", "paladin-del-caos"));
        
        createClassFile("nigromante", "Nigromante", "Hechicero oscuro con invocación de muertos y habilidades de putrefacción",
            "Invoca no muertos, alto daño mágico, debilita a los enemigos",
            "Baja defensa, débil en habilidades a melé",
            java.util.Arrays.asList("lich", "caballero-de-la-muerte"));
        
        createClassFile("druida", "Druida", "Usa magia de naturaleza y transformaciones",
            "Se transforma en animales, se puede curar a sí mismo, magia de naturaleza",
            "Baja defensa en formas de animales, débil en habilidades a melé",
            java.util.Arrays.asList("licantropo", "archidruida"));
        
        createClassFile("evocador", "Evocador", "Usa magia para apoyar a los aliados",
            "Magias de soporte y daño a distancia decente",
            "Baja vida y débil daño a melé",
            java.util.Arrays.asList("salvaguarda", "destructor"));
        
        createClassFile("cazador", "Cazador", "Pelea junto a su mascota compañera",
            "Disponibilidad de mascota y buena velocidad de movimiento",
            "Baja defensa y vida",
            java.util.Arrays.asList("maestro-de-la-manada", "combatiente-primigenio"));
        
        createClassFile("sacerdote", "Sacerdote", "Enfocado a curar a tus compañeros a cambio de tener poco daño",
            "Alta curación propia y de los aliados, habilidades de escudo",
            "No tiene daño a melé y daño a distancia débil",
            java.util.Arrays.asList("primarca", "sacerdote-corrupto"));
        
        createClassFile("trampero", "Trampero", "Pelea basándose en sus trampas",
            "Alto daño en trampas y torretas",
            "No puede usar combate cuerpo a cuerpo ni a distancia",
            java.util.Arrays.asList("maestro-de-las-trampas", "ingeniero"));
        
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
