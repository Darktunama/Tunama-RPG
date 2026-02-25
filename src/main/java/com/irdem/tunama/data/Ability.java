package com.irdem.tunama.data;

import java.util.HashMap;
import java.util.Map;

public class Ability {
    private String id;
    private String name;
    private String description;
    private String rpgClass;
    private int requiredLevel;
    private String manaCost;
    private String material;
    private int customModelData;
    private String particle;
    private int particleCount;
    private double particleSpeed;
    private String requiredWeapon;
    private double range;
    private double areaOfEffect;
    private double castTime;
    private Map<String, Double> damageScaling;
    private String castMode; // "mobile" = puede moverse, "static" = requiere estar quieto
    private double cooldown; // Tiempo de espera entre usos (en segundos)
    private double armorPenetration; // Penetración de armadura física (valor plano)
    private double magicPenetration; // Penetración de armadura mágica (valor plano)
    private String damageType; // "physical", "magical", "poison", "necrotic"
    private int manaCostPercent; // % del maná máximo que se suma al coste base (0 = sin %)
    private double critBonus; // Bonificación de crítico (0.5 = +50% daño)
    private double critDuration; // Duración del buff de crítico en segundos
    private boolean passive; // Habilidad pasiva (no se puede asignar a la barra)
    private boolean formOnly; // Habilidad solo disponible en forma de animal (no mostrar en menú)
    private Map<String, Object> customProperties; // Propiedades personalizadas del YML

    public Ability(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rpgClass = "General";
        this.requiredLevel = 1;
        this.manaCost = "0";
        this.material = "BLAZE_ROD";
        this.customModelData = 0;
        this.particle = "";
        this.particleCount = 10;
        this.particleSpeed = 0.5;
        this.requiredWeapon = "";
        this.range = 0;
        this.areaOfEffect = 0;
        this.castTime = 0;
        this.damageScaling = new HashMap<>();
        this.castMode = "mobile"; // Por defecto permite moverse
        this.cooldown = 0; // Sin cooldown por defecto
        this.armorPenetration = 0;
        this.magicPenetration = 0;
        this.damageType = "physical";
        this.manaCostPercent = 0;
        this.critBonus = 0; // Sin bonus de crítico por defecto
        this.critDuration = 0; // Sin duración por defecto
        this.passive = false; // Por defecto no es pasiva
        this.formOnly = false; // Por defecto no es solo de forma
        this.customProperties = new HashMap<>();
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getRpgClass() { return rpgClass; }
    public int getRequiredLevel() { return requiredLevel; }
    public String getManaCost() { return manaCost; }
    public String getMaterial() { return material; }
    public int getCustomModelData() { return customModelData; }
    public String getParticle() { return particle; }
    public int getParticleCount() { return particleCount; }
    public double getParticleSpeed() { return particleSpeed; }
    public String getRequiredWeapon() { return requiredWeapon; }
    public double getRange() { return range; }
    public double getAreaOfEffect() { return areaOfEffect; }
    public double getCastTime() { return castTime; }
    public Map<String, Double> getDamageScaling() { return damageScaling; }
    public String getCastMode() { return castMode; }
    public double getCooldown() { return cooldown; }
    public double getArmorPenetration() { return armorPenetration; }
    public double getMagicPenetration() { return magicPenetration; }
    public String getDamageType() { return damageType; }
    public int getManaCostPercent() { return manaCostPercent; }
    public double getCritBonus() { return critBonus; }
    public double getCritDuration() { return critDuration; }
    public boolean isPassive() { return passive; }
    public boolean isFormOnly() { return formOnly; }

    public void setRpgClass(String rpgClass) { this.rpgClass = rpgClass; }
    public void setRequiredLevel(int level) { this.requiredLevel = level; }
    public void setManaCost(String cost) { this.manaCost = cost; }
    public void setMaterial(String material) { this.material = material; }
    public void setCustomModelData(int customModelData) { this.customModelData = customModelData; }
    public void setParticle(String particle) { this.particle = particle; }
    public void setParticleCount(int particleCount) { this.particleCount = particleCount; }
    public void setParticleSpeed(double particleSpeed) { this.particleSpeed = particleSpeed; }
    public void setRequiredWeapon(String requiredWeapon) { this.requiredWeapon = requiredWeapon; }
    public void setRange(double range) { this.range = range; }
    public void setAreaOfEffect(double aoe) { this.areaOfEffect = aoe; }
    public void setCastTime(double castTime) { this.castTime = castTime; }
    public void setDamageScaling(Map<String, Double> scaling) { this.damageScaling = scaling; }
    public void setCastMode(String castMode) { this.castMode = castMode; }
    public void setCooldown(double cooldown) { this.cooldown = cooldown; }
    public void setArmorPenetration(double armorPenetration) { this.armorPenetration = armorPenetration; }
    public void setMagicPenetration(double magicPenetration) { this.magicPenetration = magicPenetration; }
    public void setDamageType(String damageType) { this.damageType = damageType != null ? damageType : "physical"; }
    public void setManaCostPercent(int manaCostPercent) { this.manaCostPercent = manaCostPercent; }
    public void setCritBonus(double critBonus) { this.critBonus = critBonus; }
    public void setCritDuration(double critDuration) { this.critDuration = critDuration; }
    public void setPassive(boolean passive) { this.passive = passive; }
    public void setFormOnly(boolean formOnly) { this.formOnly = formOnly; }

    // Custom properties
    public Map<String, Object> getCustomProperties() { return customProperties; }
    public void setCustomProperties(Map<String, Object> customProperties) { this.customProperties = customProperties; }
    public void setCustomProperty(String key, Object value) { this.customProperties.put(key, value); }

    /**
     * Obtiene una propiedad personalizada como double
     */
    public double getDoubleProperty(String key, double defaultValue) {
        Object value = customProperties.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Obtiene una propiedad personalizada como int
     */
    public int getIntProperty(String key, int defaultValue) {
        Object value = customProperties.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Obtiene una propiedad personalizada como String
     */
    public String getStringProperty(String key, String defaultValue) {
        Object value = customProperties.get(key);
        if (value != null) {
            return value.toString();
        }
        return defaultValue;
    }

    /**
     * Calcula el daño basado en las estadísticas del jugador y los multiplicadores.
     */
    public double calculateDamage(PlayerStats stats) {
        double damage = 0;
        damage += stats.getHealth() * damageScaling.getOrDefault("health", 0.0);
        damage += stats.getStrength() * damageScaling.getOrDefault("strength", 0.0);
        damage += stats.getAgility() * damageScaling.getOrDefault("agility", 0.0);
        damage += stats.getIntelligence() * damageScaling.getOrDefault("intelligence", 0.0);
        damage += stats.getSacredPower() * damageScaling.getOrDefault("sacred-power", 0.0);
        damage += stats.getCorruptPower() * damageScaling.getOrDefault("corrupt-power", 0.0);
        damage += stats.getNaturePower() * damageScaling.getOrDefault("nature-power", 0.0);
        return damage;
    }
}
