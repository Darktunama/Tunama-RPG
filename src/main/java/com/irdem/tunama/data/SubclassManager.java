package com.irdem.tunama.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class SubclassManager {

    private Map<String, Subclass> subclasses;
    private JavaPlugin plugin;
    private File subclassesFolder;

    public SubclassManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.subclasses = new HashMap<>();
        this.subclassesFolder = new File(plugin.getDataFolder(), "subclases");
        
        // Crear carpeta si no existe
        if (!subclassesFolder.exists()) {
            subclassesFolder.mkdirs();
        }
        
        loadSubclasses();
    }

    public void loadSubclasses() {
        subclasses.clear();
        
        File[] files = subclassesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No se encontraron archivos de subclases. Creando subclases por defecto...");
            createDefaultSubclasses();
            return;
        }
        
        for (File file : files) {
            loadSubclassFromFile(file);
        }
        
        plugin.getLogger().info("Se cargaron " + subclasses.size() + " subclases correctamente");
    }

    private void loadSubclassFromFile(File file) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = config.getString("id");
            String name = config.getString("name");
            String description = config.getString("description");
            String parentClass = config.getString("parent-class");
            
            if (id == null || name == null || parentClass == null) {
                plugin.getLogger().warning("Archivo de subclase incompleto: " + file.getName());
                return;
            }
            
            Subclass subclass = new Subclass(id, name, description, parentClass);
            subclass.setAdvantages(config.getString("advantages", "Sin ventajas"));
            subclass.setDisadvantages(config.getString("disadvantages", "Sin desventajas"));
            subclass.setOrder(config.getInt("order", 999)); // 999 por defecto para que aparezcan al final

            subclasses.put(id, subclass);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al cargar subclase desde " + file.getName() + ": " + e.getMessage());
        }
    }

    private void createDefaultSubclasses() {
        createSubclassFile("bersker", "Bersker", "Un maestro en el arte del combate que decidió abandonar toda armadura a cambio de pegar mas fuerte", "guerrero",
            "Gran cantidad vida, Gran daño a melé, puede absorber una parte del daño (absorbe el 10% del daño recibido como curación)",
            "Tiene 0 de Defensa Fija, débil en habilidades de rango");
        
        createSubclassFile("maestro-de-armas", "Maestro de Armas", "Un maestro en el arte del combate con alta defensa que perdió la capacidad de absorber el daño", "guerrero",
            "Buena defensa y vida, fuerte daño a melé",
            "Movimiento lento, débil en habilidades de rango");
        
        createSubclassFile("shaolin", "Shaolin", "Un luchador experimentado que abandona su movilidad a cambio de ganar mas daño", "monje",
            "Gran daño a melé, puede stunear enemigos (5% Probabilidad)",
            "Defensa baja, débil en habilidades de rango tanto magias, trampas y arcos");
        
        createSubclassFile("maestro-zen", "Maestro Zen", "Un luchador experimentado con mucha velocidad pero perdió la capacidad de stunear a los enemigos a cambio de mas daño", "monje",
            "Movimiento alto, Gran daño a melé",
            "Defensa baja, débil en habilidades de rango tanto magias, trampas y arcos");
        
        createSubclassFile("elementalista", "Elementalista", "Poderoso hechicero con alto daño mágico que se especializo en el dominio de los elementos", "mago",
            "Alto daño mágico, variedad de hechizos de diferentes elementos, ataca a distancia",
            "Baja defensa, débil en combate cuerpo a cuerpo");
        
        createSubclassFile("mago-de-combate", "Mago de Combate", "Poderoso hechicero con alto daño mágico cuerpo a cuerpo", "mago",
            "Alto daño mágico con poderes concentrado en sus armas cuerpo a cuerpo",
            "Nula capacitad de combate a distancia");
        
        createSubclassFile("brujo", "Brujo", "Hechicero que invoca demonios que luchen por el", "invocador",
            "Invoca demonios, las demonios pelean por ti, puede invocar varias demonios a la vez",
            "Débil en habilidades melé, defensa baja, débil en habilidades de distancia");
        
        createSubclassFile("chaman", "Chamán", "Hechicero que invoca elementales para que luchen por el", "invocador",
            "Invoca elementales, las elementales pelean por ti, puede invocar varias elementales a la vez",
            "Débil en habilidades melé, defensa baja, débil en habilidades de distancia");
        
        createSubclassFile("francotirador", "Francotirador", "Un preciso luchador a distancia con alto critico que sacrifico su movilidad para ello", "arquero",
            "Alta cantidad de critico, daño fuerte a distancia, puede golpear a los enemigos a distancia con su arco",
            "Baja defensa y débil en habilidad a melé");
        
        createSubclassFile("guardabosques", "Guardabosques", "Un preciso luchador a distancia con alta movilidad", "arquero",
            "Alta velocidad de movimiento, daño fuerte a distancia, puede golpear a los enemigos a distancia con su arco",
            "Baja defensa y débil en habilidad a melé");
        
        createSubclassFile("asesino", "Asesino", "Luchador hábil con alta posibilidad de hacer golpes críticos y gran movilidad", "picaro",
            "Alta posibilidad de critico, alta movilidad, hace mas daño si golpea por la espalda",
            "Baja defensa y nula habilidad a distancia");
        
        createSubclassFile("asaltante", "Asaltante", "Luchador rápido que prefiere escapar de combate y atacar por sorpresa", "picaro",
            "Gran movilidad, puede escapar de combates, excelente daño a distancia",
            "Baja defensa, débil en combate prolongado");
        
        createSubclassFile("paladin-sagrado", "Paladín Sagrado", "Luchador balanceado en combate que puede curar tanto a si mismo como a aliados", "paladin",
            "Alta defensa y vida, curaciones y habilidades variadas",
            "Débil tanto en habilidades melé como distancia");
        
        createSubclassFile("paladin-del-caos", "Paladín del Caos", "Paladín que usa magia corrupta para potenciar su combate", "paladin",
            "Alto daño mágico, curaciones poderosas, buena defensa",
            "Débil en combate a distancia");
        
        createSubclassFile("lich", "Lich", "Hechicero Oscuro con invocación de muertos y usa habilidades de putrefacción", "nigromante",
            "Invoca no muertos, Alto daño mágico, debilita a los enemigos con su putrefacción",
            "Baja defensa, Débil en habilidades a melé");
        
        createSubclassFile("caballero-de-la-muerte", "Caballero de la Muerte", "Guerrero que usa magia oscura para potenciar su combate", "nigromante",
            "Alto daño a melé con magia, vida temporal robada a enemigos",
            "Débil en defensa");
        
        createSubclassFile("licantropo", "Licantropo", "Usa magia de Naturaleza y transformaciones en animales fuertes", "druida",
            "Se transforma en animales fuertes, se puede curar a si mismo, daño alto en forma animal",
            "Baja defensa en formas débiles de animales");
        
        createSubclassFile("archidruida", "Archidruida", "Maestro en la magia de naturaleza con transformaciones versátiles", "druida",
            "Se transforma en varios animales, magia de naturaleza poderosa, se puede curar",
            "Baja defensa en la forma humana");
        
        createSubclassFile("salvaguarda", "Salvaguarda", "Usa magia su magia apoyar a los aliados y puede hacer algo de daño", "evocador",
            "Magias de soporte y decente daño a distancia",
            "Baja vida y débil daño a melé");
        
        createSubclassFile("destructor", "Destructor", "Usa magia para infligir daño masivo", "evocador",
            "Alto daño mágico en área, hechizos destructivos",
            "Baja defensa, baja vida");
        
        createSubclassFile("maestro-de-la-manada", "Maestro de la Manada", "Pelea junto a varias mascotas compañeras", "cazador",
            "Disponibilidad de múltiples mascotas, buena velocidad de movimiento",
            "Baja defensa y vida");
        
        createSubclassFile("combatiente-primigenio", "Combatiente Primigenio", "Cazador que se une con su mascota en combate", "cazador",
            "Disponibilidad de mascota poderosa, se puede potenciar con ella",
            "Baja defensa y vida");
        
        createSubclassFile("primarca", "Primarca", "Enfocado a curar a tus compañeros con magia de luz", "sacerdote",
            "Alta curación propia a y a los aliados y habilidades de escudo",
            "No tiene daño a melé y tiene a distancia débil");
        
        createSubclassFile("sacerdote-corrupto", "Sacerdote Corrupto", "Sacerdote que usa magia oscura para sus curaciones", "sacerdote",
            "Curaciones con magia corrupta, daño mágico decente",
            "No tiene defensa a melé y tiene a distancia débil");
        
        createSubclassFile("maestro-de-las-trampas", "Maestro de las Trampas", "Pelea basándose en sus trampas explosivas", "trampero",
            "Alto daño en trampas",
            "No puede usar combate cuerpo a cuerpo ni a distancia");
        
        createSubclassFile("ingeniero", "Ingeniero", "Pelea basándose en sus torretas y trampas", "trampero",
            "Alto daño en torretas y trampas",
            "No puede usar combate cuerpo a cuerpo ni a distancia");
        
        loadSubclasses();
    }

    private void createSubclassFile(String id, String name, String description, String parentClass,
                                   String advantages, String disadvantages) {
        try {
            File file = new File(subclassesFolder, id + ".yml");
            if (!file.exists()) {
                // Asignar un orden básico (las subclases se ordenarán por clase padre)
                FileConfiguration config = new YamlConfiguration();
                config.set("id", id);
                config.set("name", name);
                config.set("order", 1); // Por defecto, se puede ajustar manualmente
                config.set("description", description);
                config.set("parent-class", parentClass);
                config.set("advantages", advantages);
                config.set("disadvantages", disadvantages);
                config.save(file);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error al crear archivo de subclase: " + e.getMessage());
        }
    }

    public Subclass getSubclass(String id) {
        return subclasses.get(id.toLowerCase());
    }

    public Map<String, Subclass> getAllSubclasses() {
        // Ordenar las subclases por el campo order
        return subclasses.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(Subclass::getOrder)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public boolean isValidSubclass(String id) {
        return subclasses.containsKey(id.toLowerCase());
    }
}
