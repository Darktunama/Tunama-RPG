package com.irdem.tunama.data;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import com.irdem.tunama.TunamaRPG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Representa un objeto RPG del plugin
 */
public class RPGItem {
    private String id;
    private String name;
    private String type; // ring, necklace, amulet, weapon, armor, etc.
    private String description;
    private Material material;
    private List<String> lore;
    private Map<String, Integer> stats; // health, strength, agility, intelligence, sacred, corrupt, nature

    // Requisitos
    private int requiredLevel;
    private Map<String, Integer> requiredStats; // Estadísticas mínimas necesarias
    private List<String> allowedClasses; // Lista de clases permitidas (vacío = todas)

    // Personalización visual
    private int customModelData; // Para resource packs

    public RPGItem() {
        this.lore = new ArrayList<>();
        this.stats = new HashMap<>();
        this.requiredStats = new HashMap<>();
        this.allowedClasses = new ArrayList<>();
        this.requiredLevel = 0;
        this.customModelData = 0;
    }

    /**
     * Crea un ItemStack a partir de este RPGItem
     * Cada item tiene un UUID único para evitar que se stackeen
     */
    public ItemStack toItemStack(int amount) {
        // Crear items individuales para evitar stacking
        // Solo retornamos 1 item a la vez, el amount se ignora para objetos RPG
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);

            // Construir lore con stats y requisitos
            List<String> fullLore = new ArrayList<>();

            // Descripción
            if (description != null && !description.isEmpty()) {
                fullLore.add("§7" + description);
                fullLore.add("");
            }

            // Stats que otorga el objeto
            if (!stats.isEmpty()) {
                fullLore.add("§a§lBonificaciones:");
                for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                    String statName = getStatDisplayName(entry.getKey());
                    int value = entry.getValue();
                    String sign = value >= 0 ? "+" : "";
                    fullLore.add("§7" + statName + ": §a" + sign + value);
                }
                fullLore.add("");
            }

            // Requisitos
            boolean hasRequirements = requiredLevel > 0 || !requiredStats.isEmpty() || !allowedClasses.isEmpty();
            if (hasRequirements) {
                fullLore.add("§c§lRequisitos:");

                if (requiredLevel > 0) {
                    fullLore.add("§7Nivel: §e" + requiredLevel);
                }

                for (Map.Entry<String, Integer> entry : requiredStats.entrySet()) {
                    String statName = getStatDisplayName(entry.getKey());
                    fullLore.add("§7" + statName + ": §e" + entry.getValue());
                }

                if (!allowedClasses.isEmpty()) {
                    fullLore.add("§7Clases: §e" + String.join(", ", allowedClasses));
                }
                fullLore.add("");
            }

            // Lore personalizado del archivo
            if (lore != null && !lore.isEmpty()) {
                fullLore.addAll(lore);
            }

            // Identificador oculto
            fullLore.add("§8ID: " + id);

            meta.setLore(fullLore);

            // CustomModelData para resource packs
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }

            // Añadir UUID único usando PersistentDataContainer para evitar stacking
            TunamaRPG plugin = TunamaRPG.getInstance();
            if (plugin != null) {
                NamespacedKey uniqueKey = new NamespacedKey(plugin, "rpg_item_uuid");
                meta.getPersistentDataContainer().set(uniqueKey, PersistentDataType.STRING, UUID.randomUUID().toString());
            }

            // Establecer max stack size a 1 (Paper 1.21+)
            meta.setMaxStackSize(1);

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Verifica si un jugador cumple los requisitos para usar este objeto
     */
    public boolean meetsRequirements(PlayerData playerData) {
        // Verificar nivel
        if (requiredLevel > 0 && playerData.getLevel() < requiredLevel) {
            return false;
        }

        // Verificar stats
        for (Map.Entry<String, Integer> entry : requiredStats.entrySet()) {
            int playerStat = getPlayerStat(playerData, entry.getKey());
            if (playerStat < entry.getValue()) {
                return false;
            }
        }

        // Verificar clase
        if (!allowedClasses.isEmpty()) {
            String playerClass = playerData.getPlayerClass();
            if (playerClass == null || playerClass.isEmpty()) {
                return false;
            }

            boolean classAllowed = false;
            for (String allowedClass : allowedClasses) {
                if (allowedClass.equalsIgnoreCase(playerClass)) {
                    classAllowed = true;
                    break;
                }
            }
            if (!classAllowed) {
                return false;
            }
        }

        return true;
    }

    /**
     * Obtiene el mensaje de requisitos no cumplidos
     */
    public List<String> getUnmetRequirements(PlayerData playerData) {
        List<String> unmet = new ArrayList<>();

        if (requiredLevel > 0 && playerData.getLevel() < requiredLevel) {
            unmet.add("§c✗ Nivel insuficiente (necesitas nivel " + requiredLevel + ")");
        }

        for (Map.Entry<String, Integer> entry : requiredStats.entrySet()) {
            int playerStat = getPlayerStat(playerData, entry.getKey());
            if (playerStat < entry.getValue()) {
                unmet.add("§c✗ " + getStatDisplayName(entry.getKey()) + " insuficiente (necesitas " + entry.getValue() + ")");
            }
        }

        if (!allowedClasses.isEmpty()) {
            String playerClass = playerData.getPlayerClass();
            boolean classAllowed = false;
            if (playerClass != null && !playerClass.isEmpty()) {
                for (String allowedClass : allowedClasses) {
                    if (allowedClass.equalsIgnoreCase(playerClass)) {
                        classAllowed = true;
                        break;
                    }
                }
            }
            if (!classAllowed) {
                unmet.add("§c✗ Tu clase no puede usar este objeto (solo: " + String.join(", ", allowedClasses) + ")");
            }
        }

        return unmet;
    }

    private int getPlayerStat(PlayerData playerData, String statKey) {
        if (playerData.getStats() == null) return 0;

        switch (statKey.toLowerCase()) {
            case "health": return playerData.getStats().getHealth();
            case "strength": return playerData.getStats().getStrength();
            case "agility": return playerData.getStats().getAgility();
            case "intelligence": return playerData.getStats().getIntelligence();
            case "sacred": return playerData.getStats().getSacredPower();
            case "corrupt": return playerData.getStats().getCorruptPower();
            case "nature": return playerData.getStats().getNaturePower();
            default: return 0;
        }
    }

    private String getStatDisplayName(String statKey) {
        switch (statKey.toLowerCase()) {
            case "health": return "Vida";
            case "strength": return "Fuerza";
            case "agility": return "Agilidad";
            case "intelligence": return "Inteligencia";
            case "sacred": return "Poder Sagrado";
            case "corrupt": return "Poder Corrupto";
            case "nature": return "Poder Naturaleza";
            default: return statKey;
        }
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }

    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; }

    public Map<String, Integer> getStats() { return stats; }
    public void setStats(Map<String, Integer> stats) { this.stats = stats; }

    public int getRequiredLevel() { return requiredLevel; }
    public void setRequiredLevel(int requiredLevel) { this.requiredLevel = requiredLevel; }

    public Map<String, Integer> getRequiredStats() { return requiredStats; }
    public void setRequiredStats(Map<String, Integer> requiredStats) { this.requiredStats = requiredStats; }

    public List<String> getAllowedClasses() { return allowedClasses; }
    public void setAllowedClasses(List<String> allowedClasses) { this.allowedClasses = allowedClasses; }

    public int getCustomModelData() { return customModelData; }
    public void setCustomModelData(int customModelData) { this.customModelData = customModelData; }
}
