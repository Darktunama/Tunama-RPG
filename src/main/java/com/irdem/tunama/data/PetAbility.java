package com.irdem.tunama.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Representa una habilidad de mascota cargada desde YML
 */
public class PetAbility {
    private String id;
    private String name;
    private String description;
    private int cooldown;                    // En segundos
    private double damageMultiplier;         // Multiplicador de daño
    private Map<String, Object> properties;  // Propiedades adicionales (poison, burn, etc.)

    public PetAbility() {
        this.properties = new HashMap<>();
        this.cooldown = 10;
        this.damageMultiplier = 1.0;
    }

    public PetAbility(String id, String name, int cooldown) {
        this();
        this.id = id;
        this.name = name;
        this.cooldown = cooldown;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getCooldown() { return cooldown; }
    public void setCooldown(int cooldown) { this.cooldown = cooldown; }

    public double getDamageMultiplier() { return damageMultiplier; }
    public void setDamageMultiplier(double damageMultiplier) { this.damageMultiplier = damageMultiplier; }

    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }

    public void setProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public int getIntProperty(String key, int defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    public double getDoubleProperty(String key, double defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}
