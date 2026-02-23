package com.irdem.tunama.data;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

/**
 * Representa una mascota de combate del sistema RPG
 */
public class Pet {
    private String id;                    // ID único de la mascota (UUID)
    private String typeId;                // Tipo de mascota (lobo, zombie, etc.)
    private String customName;            // Nombre dado por el jugador
    private UUID ownerUuid;               // UUID del dueño
    private int characterSlot;            // Slot del personaje del dueño
    private int level;                    // Nivel de la mascota
    private int experience;               // XP de la mascota
    private int currentHealth;            // Vida actual
    private int maxHealth;                // Vida máxima calculada
    private int damage;                   // Daño calculado
    private int armor;                    // Armadura/Defensa calculada
    private double attackSpeed;           // Velocidad de ataque
    private double movementSpeed;         // Velocidad de movimiento
    private boolean active;               // Si está invocada actualmente
    private boolean dead;                 // Si la mascota está muerta
    private Entity entity;                // Referencia a la entidad de MC (null si guardada)
    private PetCommand currentCommand;    // Comando actual
    private Entity target;                // Objetivo actual de ataque

    public Pet() {
        this.id = UUID.randomUUID().toString();
        this.level = 1;
        this.experience = 0;
        this.active = false;
        this.dead = false;
        this.currentCommand = PetCommand.FOLLOW;
        this.characterSlot = 1;
        this.attackSpeed = 1.0;
        this.movementSpeed = 1.0;
    }

    public Pet(String typeId, UUID ownerUuid) {
        this();
        this.typeId = typeId;
        this.ownerUuid = ownerUuid;
    }

    /**
     * Calcula las estadísticas de la mascota basándose en su tipo y nivel
     */
    public void calculateStats(PetType type) {
        calculateStats(type, 0, 0, 0);
    }

    /**
     * Calcula las estadísticas de la mascota basándose en su tipo, nivel y stats del jugador
     * @param type El tipo de mascota
     * @param playerHealth Vida máxima del jugador
     * @param playerAttack Daño del jugador (fuerza)
     * @param playerAgility Agilidad del jugador (afecta defensa, vel. ataque y movimiento)
     */
    public void calculateStats(PetType type, int playerHealth, int playerAttack, int playerAgility) {
        if (type == null) return;

        this.maxHealth = type.getHealthForLevel(level, playerHealth);
        this.damage = type.getAttackForLevel(level, playerAttack);
        this.armor = type.getDefenseForLevel(level, playerAgility);
        this.attackSpeed = type.getAttackSpeedForLevel(level, playerAgility);
        this.movementSpeed = type.getMovementSpeedForLevel(level, playerAgility);

        // Si la vida actual excede el máximo o está inválida, ajustar (pero no si está muerto)
        if (!dead && (currentHealth > maxHealth || currentHealth <= 0)) {
            currentHealth = maxHealth;
        }
    }

    /**
     * Añade experiencia y verifica si sube de nivel
     * @return true si subió de nivel
     */
    public boolean addExperience(int amount) {
        this.experience += amount;
        int requiredXp = getRequiredExperienceForNextLevel();

        if (experience >= requiredXp && level < 50) { // Max level 50
            experience -= requiredXp;
            level++;
            return true;
        }
        return false;
    }

    /**
     * Calcula la experiencia necesaria para el siguiente nivel
     */
    public int getRequiredExperienceForNextLevel() {
        return 100 + (level * 50); // 150 para nivel 2, 200 para nivel 3, etc.
    }

    /**
     * Aplica daño a la mascota
     * @return true si la mascota murió
     */
    public boolean takeDamage(int amount) {
        int effectiveDamage = Math.max(1, amount - armor);
        currentHealth -= effectiveDamage;

        if (currentHealth <= 0) {
            currentHealth = 0;
            return true; // Murió
        }
        return false;
    }

    /**
     * Cura a la mascota
     */
    public void heal(int amount) {
        currentHealth = Math.min(maxHealth, currentHealth + amount);
    }

    /**
     * Cura completamente a la mascota
     */
    public void fullHeal() {
        currentHealth = maxHealth;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTypeId() { return typeId; }
    public void setTypeId(String typeId) { this.typeId = typeId; }

    public String getCustomName() { return customName; }
    public void setCustomName(String customName) { this.customName = customName; }

    public UUID getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(UUID ownerUuid) { this.ownerUuid = ownerUuid; }

    public int getCharacterSlot() { return characterSlot; }
    public void setCharacterSlot(int characterSlot) { this.characterSlot = characterSlot; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }

    public int getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(int currentHealth) { this.currentHealth = currentHealth; }

    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }

    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }

    public int getArmor() { return armor; }
    public void setArmor(int armor) { this.armor = armor; }

    public double getAttackSpeed() { return attackSpeed; }
    public void setAttackSpeed(double attackSpeed) { this.attackSpeed = attackSpeed; }

    public double getMovementSpeed() { return movementSpeed; }
    public void setMovementSpeed(double movementSpeed) { this.movementSpeed = movementSpeed; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isDead() { return dead; }
    public void setDead(boolean dead) { this.dead = dead; }

    /**
     * Marca la mascota como muerta
     */
    public void kill() {
        this.dead = true;
        this.currentHealth = 0;
        this.active = false;
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
        this.entity = null;
        this.target = null;
    }

    /**
     * Resucita la mascota con un porcentaje de vida
     * @param healthPercent porcentaje de vida con la que resucita (0-100)
     */
    public void resurrect(int healthPercent) {
        if (!dead) return;
        this.dead = false;
        this.currentHealth = Math.max(1, (maxHealth * healthPercent) / 100);
    }

    /**
     * Resucita la mascota con vida completa
     */
    public void resurrectFull() {
        resurrect(100);
    }

    /**
     * Verifica si la mascota puede ser invocada
     */
    public boolean canBeSummoned() {
        return !dead && !active;
    }

    public Entity getEntity() { return entity; }
    public void setEntity(Entity entity) { this.entity = entity; }

    public LivingEntity getLivingEntity() {
        if (entity instanceof LivingEntity) {
            return (LivingEntity) entity;
        }
        return null;
    }

    public PetCommand getCurrentCommand() { return currentCommand; }
    public void setCurrentCommand(PetCommand currentCommand) { this.currentCommand = currentCommand; }

    public Entity getTarget() { return target; }
    public void setTarget(Entity target) { this.target = target; }

    /**
     * Obtiene el nombre para mostrar (custom name o tipo)
     */
    public String getDisplayName() {
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }
        return typeId;
    }
}
