package com.irdem.tunama.data;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * Define un tipo de mascota cargado desde archivos YML
 */
public class PetType {
    private String id;
    private String name;
    private String description;
    private String role;                  // tank, dps, support, assassin, debuffer, bruiser
    private int customModelData;          // Modelo personalizado para resource packs
    private EntityType entityType;        // WOLF, ZOMBIE, SKELETON, etc.

    // Estadísticas base
    private int baseHealth;
    private int baseDefense;
    private int baseAttack;
    private double baseAttackSpeed;       // Ataques por segundo
    private double baseMovementSpeed;     // Multiplicador de velocidad

    // Crecimiento por nivel
    private double healthPerLevel;
    private double defensePerLevel;
    private double attackPerLevel;
    private double attackSpeedPerLevel;
    private double movementSpeedPerLevel;

    // Escalado desde estadísticas del jugador (porcentaje 0.0 - 1.0)
    private double healthFromPlayer;        // % de vida del jugador que se añade
    private double attackFromPlayer;        // % de fuerza del jugador que se añade al ataque
    private double defenseFromPlayer;       // % de agilidad del jugador que se añade a defensa
    private double attackSpeedFromPlayer;   // % de agilidad del jugador que se añade a vel. ataque
    private double movementSpeedFromPlayer; // % de agilidad del jugador que se añade a vel. movimiento

    // Configuración
    private Material menuIcon;
    private int requiredLevel;
    private List<String> allowedClasses;  // Vacío = todas con acceso a mascotas

    // Habilidades
    private List<PetAbility> abilities;

    // Tienda
    private double price;
    private String permission;

    public PetType() {
        this.allowedClasses = new ArrayList<>();
        this.abilities = new ArrayList<>();
        this.role = "dps";
        this.customModelData = 0;
        this.baseHealth = 40;
        this.baseDefense = 5;
        this.baseAttack = 8;
        this.baseAttackSpeed = 1.0;
        this.baseMovementSpeed = 1.0;
        this.healthPerLevel = 8;
        this.defensePerLevel = 1;
        this.attackPerLevel = 2;
        this.attackSpeedPerLevel = 0.02;
        this.movementSpeedPerLevel = 0.01;
        this.healthFromPlayer = 0.0;
        this.attackFromPlayer = 0.0;
        this.defenseFromPlayer = 0.0;
        this.attackSpeedFromPlayer = 0.0;
        this.movementSpeedFromPlayer = 0.0;
        this.requiredLevel = 1;
        this.menuIcon = Material.EGG;
        this.price = 500;
        this.permission = "";
    }

    public PetType(String id, String name, EntityType entityType) {
        this();
        this.id = id;
        this.name = name;
        this.entityType = entityType;
    }

    /**
     * Verifica si una clase puede usar este tipo de mascota
     */
    public boolean isClassAllowed(String playerClass) {
        // Si la lista está vacía, todas las clases con acceso a mascotas pueden usar este tipo
        if (allowedClasses == null || allowedClasses.isEmpty()) {
            return true;
        }
        // Verificar si la clase está en la lista
        for (String allowedClass : allowedClasses) {
            if (allowedClass.equalsIgnoreCase(playerClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calcula la vida máxima para un nivel dado
     */
    public int getHealthForLevel(int level) {
        return (int) (baseHealth + (healthPerLevel * (level - 1)));
    }

    /**
     * Calcula la vida máxima para un nivel dado, incluyendo % de stats del jugador
     */
    public int getHealthForLevel(int level, int playerHealth) {
        int base = getHealthForLevel(level);
        int fromPlayer = (int) (playerHealth * healthFromPlayer);
        return base + fromPlayer;
    }

    /**
     * Calcula la defensa para un nivel dado
     */
    public int getDefenseForLevel(int level) {
        return (int) (baseDefense + (defensePerLevel * (level - 1)));
    }

    /**
     * Calcula la defensa para un nivel dado, incluyendo % de stats del jugador
     */
    public int getDefenseForLevel(int level, int playerDefense) {
        int base = getDefenseForLevel(level);
        int fromPlayer = (int) (playerDefense * defenseFromPlayer);
        return base + fromPlayer;
    }

    /**
     * Calcula el ataque para un nivel dado
     */
    public int getAttackForLevel(int level) {
        return (int) (baseAttack + (attackPerLevel * (level - 1)));
    }

    /**
     * Calcula el ataque para un nivel dado, incluyendo % de stats del jugador
     */
    public int getAttackForLevel(int level, int playerAttack) {
        int base = getAttackForLevel(level);
        int fromPlayer = (int) (playerAttack * attackFromPlayer);
        return base + fromPlayer;
    }

    /**
     * Calcula la velocidad de ataque para un nivel dado
     */
    public double getAttackSpeedForLevel(int level) {
        return baseAttackSpeed + (attackSpeedPerLevel * (level - 1));
    }

    /**
     * Calcula la velocidad de ataque para un nivel dado, incluyendo % de agilidad del jugador
     */
    public double getAttackSpeedForLevel(int level, int playerAgility) {
        double base = getAttackSpeedForLevel(level);
        double fromPlayer = playerAgility * attackSpeedFromPlayer;
        return base + fromPlayer;
    }

    /**
     * Calcula la velocidad de movimiento para un nivel dado
     */
    public double getMovementSpeedForLevel(int level) {
        return baseMovementSpeed + (movementSpeedPerLevel * (level - 1));
    }

    /**
     * Calcula la velocidad de movimiento para un nivel dado, incluyendo % de agilidad del jugador
     */
    public double getMovementSpeedForLevel(int level, int playerAgility) {
        double base = getMovementSpeedForLevel(level);
        double fromPlayer = playerAgility * movementSpeedFromPlayer;
        return base + fromPlayer;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getCustomModelData() { return customModelData; }
    public void setCustomModelData(int customModelData) { this.customModelData = customModelData; }

    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }

    public int getBaseHealth() { return baseHealth; }
    public void setBaseHealth(int baseHealth) { this.baseHealth = baseHealth; }

    public int getBaseDefense() { return baseDefense; }
    public void setBaseDefense(int baseDefense) { this.baseDefense = baseDefense; }

    public int getBaseAttack() { return baseAttack; }
    public void setBaseAttack(int baseAttack) { this.baseAttack = baseAttack; }

    public double getBaseAttackSpeed() { return baseAttackSpeed; }
    public void setBaseAttackSpeed(double baseAttackSpeed) { this.baseAttackSpeed = baseAttackSpeed; }

    public double getBaseMovementSpeed() { return baseMovementSpeed; }
    public void setBaseMovementSpeed(double baseMovementSpeed) { this.baseMovementSpeed = baseMovementSpeed; }

    public double getHealthPerLevel() { return healthPerLevel; }
    public void setHealthPerLevel(double healthPerLevel) { this.healthPerLevel = healthPerLevel; }

    public double getDefensePerLevel() { return defensePerLevel; }
    public void setDefensePerLevel(double defensePerLevel) { this.defensePerLevel = defensePerLevel; }

    public double getAttackPerLevel() { return attackPerLevel; }
    public void setAttackPerLevel(double attackPerLevel) { this.attackPerLevel = attackPerLevel; }

    public double getAttackSpeedPerLevel() { return attackSpeedPerLevel; }
    public void setAttackSpeedPerLevel(double attackSpeedPerLevel) { this.attackSpeedPerLevel = attackSpeedPerLevel; }

    public double getMovementSpeedPerLevel() { return movementSpeedPerLevel; }
    public void setMovementSpeedPerLevel(double movementSpeedPerLevel) { this.movementSpeedPerLevel = movementSpeedPerLevel; }

    public double getHealthFromPlayer() { return healthFromPlayer; }
    public void setHealthFromPlayer(double healthFromPlayer) { this.healthFromPlayer = healthFromPlayer; }

    public double getAttackFromPlayer() { return attackFromPlayer; }
    public void setAttackFromPlayer(double attackFromPlayer) { this.attackFromPlayer = attackFromPlayer; }

    public double getDefenseFromPlayer() { return defenseFromPlayer; }
    public void setDefenseFromPlayer(double defenseFromPlayer) { this.defenseFromPlayer = defenseFromPlayer; }

    public double getAttackSpeedFromPlayer() { return attackSpeedFromPlayer; }
    public void setAttackSpeedFromPlayer(double attackSpeedFromPlayer) { this.attackSpeedFromPlayer = attackSpeedFromPlayer; }

    public double getMovementSpeedFromPlayer() { return movementSpeedFromPlayer; }
    public void setMovementSpeedFromPlayer(double movementSpeedFromPlayer) { this.movementSpeedFromPlayer = movementSpeedFromPlayer; }

    public Material getMenuIcon() { return menuIcon; }
    public void setMenuIcon(Material menuIcon) { this.menuIcon = menuIcon; }

    public int getRequiredLevel() { return requiredLevel; }
    public void setRequiredLevel(int requiredLevel) { this.requiredLevel = requiredLevel; }

    public List<String> getAllowedClasses() { return allowedClasses; }
    public void setAllowedClasses(List<String> allowedClasses) { this.allowedClasses = allowedClasses; }

    public List<PetAbility> getAbilities() { return abilities; }
    public void setAbilities(List<PetAbility> abilities) { this.abilities = abilities; }
    public void addAbility(PetAbility ability) { this.abilities.add(ability); }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    // Aliases para compatibilidad con código anterior
    public int getBaseDamage() { return baseAttack; }
    public void setBaseDamage(int baseDamage) { this.baseAttack = baseDamage; }

    public int getBaseArmor() { return baseDefense; }
    public void setBaseArmor(int baseArmor) { this.baseDefense = baseArmor; }

    public double getDamagePerLevel() { return attackPerLevel; }
    public void setDamagePerLevel(double damagePerLevel) { this.attackPerLevel = damagePerLevel; }
}
