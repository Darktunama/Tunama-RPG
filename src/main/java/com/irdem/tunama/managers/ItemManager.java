package com.irdem.tunama.managers;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.RPGItem;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestiona la carga y acceso a los objetos RPG del plugin
 */
public class ItemManager {
    private final TunamaRPG plugin;
    private final Map<String, RPGItem> items;

    public ItemManager(TunamaRPG plugin) {
        this.plugin = plugin;
        this.items = new HashMap<>();
    }

    /**
     * Carga todos los objetos desde la carpeta objetos/
     */
    public void loadItems() {
        items.clear();

        File itemsFolder = new File(plugin.getDataFolder(), "objetos");
        if (!itemsFolder.exists()) {
            plugin.getLogger().warning("Carpeta de objetos no encontrada: " + itemsFolder.getPath());
            return;
        }

        File[] files = itemsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().info("No se encontraron archivos de objetos");
            return;
        }

        int loaded = 0;
        for (File file : files) {
            try {
                RPGItem item = loadItemFromFile(file);
                if (item != null) {
                    items.put(item.getId().toLowerCase(), item);
                    loaded++;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error al cargar objeto " + file.getName() + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Cargados " + loaded + " objetos RPG");
    }

    /**
     * Carga un objeto desde un archivo YAML
     */
    private RPGItem loadItemFromFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        RPGItem item = new RPGItem();

        // Datos básicos
        item.setId(config.getString("id", file.getName().replace(".yml", "")));
        item.setName(config.getString("name", "§fObjeto Desconocido"));
        item.setType(config.getString("type", "misc"));
        item.setDescription(config.getString("description", ""));

        // Material
        String materialName = config.getString("material", "PAPER");
        try {
            item.setMaterial(Material.valueOf(materialName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Material inválido en " + file.getName() + ": " + materialName);
            item.setMaterial(Material.PAPER);
        }

        // Lore personalizado
        if (config.contains("lore")) {
            item.setLore(config.getStringList("lore"));
        }

        // Stats que otorga el objeto
        if (config.contains("stats")) {
            Map<String, Integer> stats = new HashMap<>();
            for (String key : config.getConfigurationSection("stats").getKeys(false)) {
                // Normalizar nombres de stats (español -> inglés)
                String normalizedKey = normalizeStatKey(key.toLowerCase());
                stats.put(normalizedKey, config.getInt("stats." + key, 0));
            }
            item.setStats(stats);
        }

        // Requisitos
        item.setRequiredLevel(config.getInt("requisitos.nivel", 0));

        // Requisitos de estadísticas
        if (config.contains("requisitos.estadisticas")) {
            Map<String, Integer> reqStats = new HashMap<>();
            for (String key : config.getConfigurationSection("requisitos.estadisticas").getKeys(false)) {
                reqStats.put(key.toLowerCase(), config.getInt("requisitos.estadisticas." + key, 0));
            }
            item.setRequiredStats(reqStats);
        }

        // Clases permitidas
        if (config.contains("requisitos.clases")) {
            item.setAllowedClasses(config.getStringList("requisitos.clases"));
        }

        // CustomModelData para resource packs
        item.setCustomModelData(config.getInt("custom_model_data", 0));

        return item;
    }

    /**
     * Obtiene un objeto por su ID
     */
    public RPGItem getItem(String id) {
        if (id == null) return null;
        return items.get(id.toLowerCase());
    }

    /**
     * Crea un ItemStack a partir del ID del objeto
     */
    public ItemStack createItemStack(String id, int amount) {
        RPGItem item = getItem(id);
        if (item == null) {
            return null;
        }
        return item.toItemStack(amount);
    }

    /**
     * Verifica si un objeto existe
     */
    public boolean itemExists(String id) {
        return id != null && items.containsKey(id.toLowerCase());
    }

    /**
     * Obtiene todos los IDs de objetos
     */
    public java.util.Set<String> getAllItemIds() {
        return items.keySet();
    }

    /**
     * Obtiene todos los objetos
     */
    public Map<String, RPGItem> getAllItems() {
        return new HashMap<>(items);
    }

    /**
     * Recarga los objetos desde los archivos
     */
    public void reload() {
        loadItems();
    }

    /**
     * Normaliza los nombres de stats (convierte español a inglés)
     */
    private String normalizeStatKey(String key) {
        switch (key) {
            // Stats básicos
            case "vida": return "health";
            case "fuerza": return "strength";
            case "agilidad": return "agility";
            case "inteligencia": return "intelligence";
            // Poderes
            case "sagrado":
            case "poder_sagrado":
            case "podersagrado":
                return "sacred";
            case "corrupto":
            case "poder_corrupto":
            case "podercorrupto":
                return "corrupt";
            case "naturaleza":
            case "poder_naturaleza":
            case "podernaturaleza":
            case "natura":
                return "nature";
            // Ya está en inglés o no reconocido
            default: return key;
        }
    }
}
