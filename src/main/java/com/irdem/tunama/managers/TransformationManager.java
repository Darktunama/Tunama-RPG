package com.irdem.tunama.managers;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.Ability;
import com.irdem.tunama.data.PlayerData;
import com.irdem.tunama.data.PlayerStats;
import com.irdem.tunama.listeners.AbilityBarListener;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

/**
 * Manager para manejar las transformaciones del Druida.
 * Las transformaciones cambian la apariencia del jugador, sus stats,
 * le aplican efectos y le dan acceso a habilidades específicas de la forma.
 */
public class TransformationManager {

    private final TunamaRPG plugin;

    // Jugadores actualmente transformados: UUID -> TransformationData
    private final Map<UUID, TransformationData> activeTransformations = new HashMap<>();

    // Entidades de transformación (el mob que representa al jugador)
    private final Map<UUID, LivingEntity> transformEntities = new HashMap<>();

    // Tareas de expiración y seguimiento
    private final Map<UUID, BukkitTask> expirationTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> followTasks = new HashMap<>();

    // Stats originales del jugador antes de transformarse
    private final Map<UUID, OriginalPlayerState> originalStates = new HashMap<>();

    // Habilidades originales de la barra antes de transformarse
    private final Map<UUID, Map<Integer, String>> originalAbilitySlots = new HashMap<>();

    // Items originales del hotbar antes de transformarse
    private final Map<UUID, ItemStack[]> originalHotbarItems = new HashMap<>();

    public TransformationManager(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    /**
     * Intenta transformar al jugador usando la habilidad de forma especificada.
     * @return true si la transformación fue exitosa
     */
    public boolean transform(Player player, Ability ability) {
        UUID uuid = player.getUniqueId();

        // Si ya está transformado, verificar si es la misma forma (cancelar) o diferente
        if (isTransformed(uuid)) {
            TransformationData currentTransform = activeTransformations.get(uuid);
            if (currentTransform.getAbilityId().equals(ability.getId())) {
                // Misma forma - cancelar transformación
                revertTransformation(player);
                return true;
            } else {
                // Forma diferente - primero revertir la actual
                revertTransformation(player);
            }
        }

        // Cargar datos de transformación desde el archivo YML de la habilidad
        TransformationData transformData = loadTransformationData(ability);
        if (transformData == null) {
            player.sendMessage("§c✗ Esta habilidad no tiene configuración de transformación.");
            plugin.getLogger().warning("No se encontró configuración de transformación para: " + ability.getId());
            return false;
        }
        plugin.getLogger().info("Transformación cargada: " + transformData.getEntityType().name() + " para " + ability.getId());

        // Guardar estado original del jugador
        PlayerData playerData = plugin.getDatabaseManager().getPlayerData(uuid);
        if (playerData == null) {
            player.sendMessage("§c✗ Error al obtener datos del jugador.");
            return false;
        }

        saveOriginalState(player, playerData);

        // Aplicar transformación visual y stats
        applyTransformation(player, playerData, transformData);

        // Registrar transformación activa
        activeTransformations.put(uuid, transformData);

        // Configurar barra de habilidades de la forma
        setupTransformAbilityBar(player, transformData);

        // Programar expiración si tiene duración limitada
        if (transformData.getDuration() > 0) {
            scheduleExpiration(player, transformData.getDuration());
        }

        // Efectos visuales
        player.getWorld().spawnParticle(
            Particle.CAMPFIRE_COSY_SMOKE,
            player.getLocation().add(0, 1, 0),
            30, 0.5, 1, 0.5, 0.1
        );

        // Mensaje
        String formName = ability.getName().replace("Forma de ", "");
        player.sendMessage("§a🐾 ¡Transformación! §7Ahora eres un §b" + formName + "§7.");
        player.sendMessage("§7Usa §f[9] §7o la habilidad de nuevo para volver a tu forma normal.");
        if (transformData.getDuration() > 0) {
            player.sendMessage("§7Duración: §f" + transformData.getDuration() + " segundos");
        }
        player.sendActionBar(Component.text("§a⚡ FORMA DE " + formName.toUpperCase() + " ACTIVA §7- Slot 9 para revertir"));

        return true;
    }

    /**
     * Revierte la transformación del jugador a su forma normal.
     */
    public void revertTransformation(Player player) {
        UUID uuid = player.getUniqueId();

        if (!isTransformed(uuid)) return;

        TransformationData transformData = activeTransformations.remove(uuid);

        // Cancelar tareas
        BukkitTask expTask = expirationTasks.remove(uuid);
        if (expTask != null) expTask.cancel();

        BukkitTask followTask = followTasks.remove(uuid);
        if (followTask != null) followTask.cancel();

        // Eliminar entidad de transformación
        LivingEntity transformEntity = transformEntities.remove(uuid);
        if (transformEntity != null && !transformEntity.isDead()) {
            transformEntity.remove();
        }

        // Remover del team de no colisión
        removeFromNoCollisionTeam(player);

        // Restaurar estado original
        restoreOriginalState(player);

        // Restaurar barra de habilidades original
        restoreOriginalAbilityBar(player);

        // Quitar efectos de poción de la transformación
        for (TransformationEffect effect : transformData.getEffects()) {
            PotionEffectType effectType = getPotionEffectType(effect.getType());
            if (effectType != null) {
                player.removePotionEffect(effectType);
            }
        }

        // Hacer visible al jugador
        player.removePotionEffect(PotionEffectType.INVISIBILITY);

        // Efectos visuales
        player.getWorld().spawnParticle(
            Particle.CAMPFIRE_COSY_SMOKE,
            player.getLocation().add(0, 1, 0),
            30, 0.5, 1, 0.5, 0.1
        );

        // Mensaje
        player.sendMessage("§e🐾 Has vuelto a tu forma original.");
        player.sendActionBar(Component.text("§7Forma normal restaurada"));
    }

    /**
     * Verifica si el jugador está transformado.
     */
    public boolean isTransformed(UUID uuid) {
        return activeTransformations.containsKey(uuid);
    }

    /**
     * Obtiene los datos de transformación actual del jugador.
     */
    public TransformationData getTransformation(UUID uuid) {
        return activeTransformations.get(uuid);
    }

    /**
     * Obtiene las habilidades disponibles mientras está transformado.
     */
    public List<String> getTransformAbilities(UUID uuid) {
        TransformationData data = activeTransformations.get(uuid);
        return data != null ? data.getAbilities() : Collections.emptyList();
    }

    /**
     * Obtiene el tipo de entidad de la transformación actual.
     */
    public EntityType getTransformEntityType(UUID uuid) {
        TransformationData data = activeTransformations.get(uuid);
        return data != null ? data.getEntityType() : null;
    }

    /**
     * Carga los datos de transformación desde el archivo YML de la habilidad.
     */
    private TransformationData loadTransformationData(Ability ability) {
        File abilityFile = new File(plugin.getDataFolder(), "habilidades/" + ability.getId() + ".yml");
        if (!abilityFile.exists()) {
            return null;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(abilityFile);

        if (!config.isConfigurationSection("transformation")) {
            return null;
        }

        ConfigurationSection transformSection = config.getConfigurationSection("transformation");

        TransformationData data = new TransformationData(ability.getId());

        // Entity type
        String entityTypeStr = transformSection.getString("entity-type", "WOLF");
        try {
            data.setEntityType(EntityType.valueOf(entityTypeStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            data.setEntityType(EntityType.WOLF);
        }

        // Custom model data
        data.setCustomModelData(transformSection.getInt("custom-model-data", 0));

        // Duration
        data.setDuration(transformSection.getInt("duration", 0));

        // Stats bonuses (fijos)
        if (transformSection.isConfigurationSection("stats")) {
            ConfigurationSection statsSection = transformSection.getConfigurationSection("stats");
            data.setHealthBonus(statsSection.getInt("health-bonus", 0));
            data.setStrengthBonus(statsSection.getInt("strength-bonus", 0));
            data.setAgilityBonus(statsSection.getInt("agility-bonus", 0));
            data.setDefenseBonus(statsSection.getInt("defense-bonus", 0));
        }

        // Escalado desde stats del jugador (porcentajes 0.0 - 1.0)
        // Formato nuevo: player-scaling.health.stat y player-scaling.health.multiplier
        // Formato simple: player-scaling.health (usa stat por defecto)
        if (transformSection.isConfigurationSection("player-scaling")) {
            ConfigurationSection scalingSection = transformSection.getConfigurationSection("player-scaling");

            // Health scaling
            if (scalingSection.isConfigurationSection("health")) {
                ConfigurationSection healthSection = scalingSection.getConfigurationSection("health");
                data.setHealthScalingStat(healthSection.getString("stat", "health"));
                data.setHealthScaling(healthSection.getDouble("multiplier", 0.0));
            } else {
                data.setHealthScalingStat("health");
                data.setHealthScaling(scalingSection.getDouble("health", 0.0));
            }

            // Strength scaling
            if (scalingSection.isConfigurationSection("strength")) {
                ConfigurationSection strengthSection = scalingSection.getConfigurationSection("strength");
                data.setStrengthScalingStat(strengthSection.getString("stat", "strength"));
                data.setStrengthScaling(strengthSection.getDouble("multiplier", 0.0));
            } else {
                data.setStrengthScalingStat("strength");
                data.setStrengthScaling(scalingSection.getDouble("strength", 0.0));
            }

            // Agility scaling
            if (scalingSection.isConfigurationSection("agility")) {
                ConfigurationSection agilitySection = scalingSection.getConfigurationSection("agility");
                data.setAgilityScalingStat(agilitySection.getString("stat", "agility"));
                data.setAgilityScaling(agilitySection.getDouble("multiplier", 0.0));
            } else {
                data.setAgilityScalingStat("agility");
                data.setAgilityScaling(scalingSection.getDouble("agility", 0.0));
            }

            // Attack speed scaling
            if (scalingSection.isConfigurationSection("attack-speed")) {
                ConfigurationSection attackSpeedSection = scalingSection.getConfigurationSection("attack-speed");
                data.setAttackSpeedScalingStat(attackSpeedSection.getString("stat", "agility"));
                data.setAttackSpeedScaling(attackSpeedSection.getDouble("multiplier", 0.0));
            } else {
                data.setAttackSpeedScalingStat("agility");
                data.setAttackSpeedScaling(scalingSection.getDouble("attack-speed", 0.0));
            }

            // Movement speed scaling
            if (scalingSection.isConfigurationSection("movement-speed")) {
                ConfigurationSection moveSpeedSection = scalingSection.getConfigurationSection("movement-speed");
                data.setMovementSpeedScalingStat(moveSpeedSection.getString("stat", "agility"));
                data.setMovementSpeedScaling(moveSpeedSection.getDouble("multiplier", 0.0));
            } else {
                data.setMovementSpeedScalingStat("agility");
                data.setMovementSpeedScaling(scalingSection.getDouble("movement-speed", 0.0));
            }

            // Armor scaling
            if (scalingSection.isConfigurationSection("armor")) {
                ConfigurationSection armorSection = scalingSection.getConfigurationSection("armor");
                data.setArmorScalingStat(armorSection.getString("stat", "intelligence"));
                data.setArmorScaling(armorSection.getDouble("multiplier", 0.0));
            } else {
                data.setArmorScalingStat("intelligence");
                data.setArmorScaling(scalingSection.getDouble("armor", 0.0));
            }
        }

        // Effects
        if (transformSection.isList("effects")) {
            List<Map<?, ?>> effectsList = transformSection.getMapList("effects");
            for (Map<?, ?> effectMap : effectsList) {
                String type = (String) effectMap.get("type");
                int amplifier = 0;
                Object ampObj = effectMap.get("amplifier");
                if (ampObj instanceof Number) {
                    amplifier = ((Number) ampObj).intValue();
                }
                data.addEffect(new TransformationEffect(type, amplifier));
            }
        }

        // Abilities
        if (transformSection.isList("abilities")) {
            data.setAbilities(transformSection.getStringList("abilities"));
        }

        return data;
    }

    /**
     * Guarda el estado original del jugador antes de transformarse.
     */
    private void saveOriginalState(Player player, PlayerData playerData) {
        UUID uuid = player.getUniqueId();
        PlayerStats stats = playerData.getStats();

        // Obtener atributos actuales
        double maxHealth = 20.0;
        double attackSpeed = 4.0;
        double movementSpeed = 0.1;
        double armor = 0.0;

        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) maxHealth = healthAttr.getBaseValue();

        AttributeInstance attackSpeedAttr = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attackSpeedAttr != null) attackSpeed = attackSpeedAttr.getBaseValue();

        AttributeInstance moveSpeedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (moveSpeedAttr != null) movementSpeed = moveSpeedAttr.getBaseValue();

        AttributeInstance armorAttr = player.getAttribute(Attribute.GENERIC_ARMOR);
        if (armorAttr != null) armor = armorAttr.getBaseValue();

        // Guardar habilidades actuales de la barra
        Map<Integer, String> currentSlots = new HashMap<>(AbilityBarListener.getAbilitySlots(uuid));
        originalAbilitySlots.put(uuid, currentSlots);

        OriginalPlayerState state = new OriginalPlayerState(
            stats.getHealth(),
            stats.getStrength(),
            stats.getAgility(),
            stats.getIntelligence(),
            maxHealth,
            attackSpeed,
            movementSpeed,
            armor,
            player.isInvisible()
        );
        originalStates.put(uuid, state);
    }

    /**
     * Restaura el estado original del jugador.
     */
    private void restoreOriginalState(Player player) {
        UUID uuid = player.getUniqueId();
        OriginalPlayerState state = originalStates.remove(uuid);
        if (state == null) return;

        PlayerData playerData = plugin.getDatabaseManager().getPlayerData(uuid);
        if (playerData == null) return;

        PlayerStats stats = playerData.getStats();
        stats.setHealth(state.health);
        stats.setStrength(state.strength);
        stats.setAgility(state.agility);
        stats.setIntelligence(state.intelligence);

        // Restaurar atributos
        setAttributeValue(player, Attribute.GENERIC_MAX_HEALTH, state.maxHealth);
        setAttributeValue(player, Attribute.GENERIC_ATTACK_SPEED, state.attackSpeed);
        setAttributeValue(player, Attribute.GENERIC_MOVEMENT_SPEED, state.movementSpeed);
        setAttributeValue(player, Attribute.GENERIC_ARMOR, state.armor);

        // Ajustar vida si excede el máximo
        if (player.getHealth() > state.maxHealth) {
            player.setHealth(state.maxHealth);
        }
    }

    /**
     * Restaura la barra de habilidades original.
     */
    private void restoreOriginalAbilityBar(Player player) {
        UUID uuid = player.getUniqueId();
        Map<Integer, String> originalSlots = originalAbilitySlots.remove(uuid);

        // Limpiar slots actuales
        AbilityBarListener.clearAbilitySlots(uuid);

        // Restaurar slots originales de habilidades
        if (originalSlots != null) {
            for (Map.Entry<Integer, String> entry : originalSlots.entrySet()) {
                AbilityBarListener.setAbilitySlot(uuid, entry.getKey(), entry.getValue());
            }
        }

        // Restaurar items originales del hotbar
        ItemStack[] originalItems = originalHotbarItems.remove(uuid);
        if (originalItems != null) {
            for (int i = 0; i < 9; i++) {
                player.getInventory().setItem(i, originalItems[i]);
            }
        }

        // Desactivar modo habilidades de transformación
        AbilityBarListener.disableTransformAbilityMode(uuid);
    }

    /**
     * Configura la barra de habilidades con las habilidades de la forma.
     */
    private void setupTransformAbilityBar(Player player, TransformationData transformData) {
        UUID uuid = player.getUniqueId();

        // Guardar items originales del hotbar (slots 0-8)
        ItemStack[] originalItems = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            originalItems[i] = item != null ? item.clone() : null;
        }
        originalHotbarItems.put(uuid, originalItems);

        // Limpiar barra actual de habilidades
        AbilityBarListener.clearAbilitySlots(uuid);

        // Añadir habilidades de la forma (slots 0-6, slot 7 reservado para revertir)
        List<String> formAbilities = transformData.getAbilities();
        for (int i = 0; i < Math.min(formAbilities.size(), 7); i++) {
            String abilityId = formAbilities.get(i);
            AbilityBarListener.setAbilitySlot(uuid, i, abilityId);

            // Crear item visual para el slot
            Ability ability = plugin.getAbilityManager().getAbility(abilityId);
            if (ability != null) {
                ItemStack abilityItem = createAbilityItem(ability, i + 1);
                player.getInventory().setItem(i, abilityItem);
            } else {
                // Si no existe la habilidad, poner item placeholder
                ItemStack placeholder = new ItemStack(Material.GRAY_DYE);
                var meta = placeholder.getItemMeta();
                meta.displayName(Component.text("§7Habilidad: §c" + abilityId));
                meta.lore(java.util.Arrays.asList(
                    Component.text("§cHabilidad no encontrada")
                ));
                placeholder.setItemMeta(meta);
                player.getInventory().setItem(i, placeholder);
            }
        }

        // Slots vacíos (sin habilidad asignada) - hasta slot 6 (tecla 7)
        for (int i = formAbilities.size(); i < 7; i++) {
            player.getInventory().setItem(i, null);
        }

        // Slot 7 (tecla 8) siempre es "volver a forma normal" con icono de barrera
        AbilityBarListener.setAbilitySlot(uuid, 7, "revertir-forma");
        ItemStack barrierItem = new ItemStack(Material.BARRIER);
        var barrierMeta = barrierItem.getItemMeta();
        barrierMeta.displayName(Component.text("§c⚡ Volver a Forma Normal"));
        barrierMeta.lore(java.util.Arrays.asList(
            Component.text("§7Pulsa §f[8] §7para volver"),
            Component.text("§7a tu forma de druida")
        ));
        barrierItem.setItemMeta(barrierMeta);
        player.getInventory().setItem(7, barrierItem);

        // Slot 8 (tecla 9) vacío
        player.getInventory().setItem(8, null);

        // Mover al slot 8 (tecla 9) después de configurar todo
        player.getInventory().setHeldItemSlot(8);

        // Activar modo habilidades para que los slots funcionen
        AbilityBarListener.enableTransformAbilityMode(uuid);
    }

    /**
     * Crea un item visual para una habilidad en el hotbar.
     */
    private ItemStack createAbilityItem(Ability ability, int slotNumber) {
        Material material;
        try {
            material = Material.valueOf(ability.getMaterial().toUpperCase());
        } catch (Exception e) {
            material = Material.BLAZE_ROD;
        }

        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();

        // Nombre con número de slot
        meta.displayName(Component.text("§b[" + slotNumber + "] §f" + ability.getName()));

        // Lore con descripción y costo de maná
        java.util.List<Component> lore = new java.util.ArrayList<>();
        lore.add(Component.text("§7" + ability.getDescription()));
        lore.add(Component.text(""));

        String manaCost = ability.getManaCost();
        if (manaCost != null && !manaCost.equals("0")) {
            lore.add(Component.text("§9✦ Maná: §f" + manaCost));
        }

        if (ability.getCooldown() > 0) {
            lore.add(Component.text("§e⏱ Cooldown: §f" + ability.getCooldown() + "s"));
        }

        lore.add(Component.text(""));
        lore.add(Component.text("§aPulsa §f[" + slotNumber + "] §apara usar"));

        meta.lore(lore);

        // Custom model data si está definido
        if (ability.getCustomModelData() > 0) {
            meta.setCustomModelData(ability.getCustomModelData());
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Aplica la transformación visual y de stats.
     */
    private void applyTransformation(Player player, PlayerData playerData, TransformationData transformData) {
        UUID uuid = player.getUniqueId();
        PlayerStats stats = playerData.getStats();

        // Calcular bonificaciones con escalado del jugador (usando el stat configurado)
        int healthBonus = transformData.getHealthBonus() +
            (int)(getStatValue(stats, transformData.getHealthScalingStat()) * transformData.getHealthScaling());
        int strengthBonus = transformData.getStrengthBonus() +
            (int)(getStatValue(stats, transformData.getStrengthScalingStat()) * transformData.getStrengthScaling());
        int agilityBonus = transformData.getAgilityBonus() +
            (int)(getStatValue(stats, transformData.getAgilityScalingStat()) * transformData.getAgilityScaling());

        // Logging detallado de bonus calculados
        plugin.getLogger().info("[Transformación] Bonus calculados - Vida: " + healthBonus +
            " (base: " + transformData.getHealthBonus() + ", scaling: " + transformData.getHealthScaling() + ")");
        plugin.getLogger().info("[Transformación] Bonus calculados - Fuerza: " + strengthBonus +
            " (base: " + transformData.getStrengthBonus() + ", scaling: " + transformData.getStrengthScaling() + ")");
        plugin.getLogger().info("[Transformación] Bonus calculados - Agilidad: " + agilityBonus +
            " (base: " + transformData.getAgilityBonus() + ", scaling: " + transformData.getAgilityScaling() + ")");

        // Aplicar bonificaciones de stats RPG
        stats.setHealth(stats.getHealth() + healthBonus);
        stats.setStrength(stats.getStrength() + strengthBonus);
        stats.setAgility(stats.getAgility() + agilityBonus);
        if (transformData.getDefenseBonus() != 0) {
            stats.setIntelligence(stats.getIntelligence() + transformData.getDefenseBonus());
        }

        // Aplicar modificaciones de atributos - PRIMERO aumentar vida máxima, LUEGO establecer vida
        if (healthBonus > 0) {
            AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (healthAttr != null) {
                double currentMaxHealth = healthAttr.getBaseValue();
                double newMaxHealth = currentMaxHealth + healthBonus;
                // Permitir hasta 1024 (límite de Minecraft) para formas poderosas como Warden
                double cappedMaxHealth = Math.min(newMaxHealth, 1024);
                healthAttr.setBaseValue(cappedMaxHealth);
                // Ahora sí podemos establecer la vida usando el nuevo máximo
                double newHealth = Math.min(player.getHealth() + healthBonus, cappedMaxHealth);
                player.setHealth(newHealth);
                plugin.getLogger().info("[Transformación] Vida máxima: " + currentMaxHealth + " -> " + cappedMaxHealth + ", Vida actual: " + newHealth);
            }
        }

        // Velocidad de ataque (escalado desde stat configurado)
        double attackSpeedStatValue = getStatValue(stats, transformData.getAttackSpeedScalingStat());
        double attackSpeedBonus = attackSpeedStatValue * transformData.getAttackSpeedScaling();
        plugin.getLogger().info("[Transformación] Vel. Ataque - Stat: " + transformData.getAttackSpeedScalingStat() +
            ", Valor: " + attackSpeedStatValue + ", Multiplier: " + transformData.getAttackSpeedScaling() + ", Bonus: " + attackSpeedBonus);
        if (attackSpeedBonus > 0) {
            AttributeInstance attackSpeedAttr = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attackSpeedAttr != null) {
                double newSpeed = attackSpeedAttr.getBaseValue() + attackSpeedBonus;
                attackSpeedAttr.setBaseValue(newSpeed);
                plugin.getLogger().info("[Transformación] Velocidad de ataque aplicada: " + newSpeed);
            }
        }

        // Velocidad de movimiento (escalado desde stat configurado)
        double moveSpeedStatValue = getStatValue(stats, transformData.getMovementSpeedScalingStat());
        double moveSpeedBonus = moveSpeedStatValue * transformData.getMovementSpeedScaling() * 0.01;
        plugin.getLogger().info("[Transformación] Vel. Movimiento - Stat: " + transformData.getMovementSpeedScalingStat() +
            ", Valor: " + moveSpeedStatValue + ", Multiplier: " + transformData.getMovementSpeedScaling() + ", Bonus: " + moveSpeedBonus);
        if (moveSpeedBonus > 0) {
            AttributeInstance moveSpeedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (moveSpeedAttr != null) {
                double newSpeed = moveSpeedAttr.getBaseValue() + moveSpeedBonus;
                moveSpeedAttr.setBaseValue(newSpeed);
                plugin.getLogger().info("[Transformación] Velocidad de movimiento aplicada: " + newSpeed);
            }
        }

        // Armadura (escalado desde stat configurado)
        double armorStatValue = getStatValue(stats, transformData.getArmorScalingStat());
        double armorBonus = armorStatValue * transformData.getArmorScaling();
        plugin.getLogger().info("[Transformación] Armadura - Stat: " + transformData.getArmorScalingStat() +
            ", Valor: " + armorStatValue + ", Multiplier: " + transformData.getArmorScaling() + ", Bonus: " + armorBonus);
        if (armorBonus > 0) {
            AttributeInstance armorAttr = player.getAttribute(Attribute.GENERIC_ARMOR);
            if (armorAttr != null) {
                double newArmor = armorAttr.getBaseValue() + armorBonus;
                armorAttr.setBaseValue(newArmor);
                plugin.getLogger().info("[Transformación] Armadura aplicada: " + newArmor);
            }
        }

        // Aplicar efectos de poción
        for (TransformationEffect effect : transformData.getEffects()) {
            PotionEffectType effectType = getPotionEffectType(effect.getType());
            if (effectType != null) {
                int duration = transformData.getDuration() > 0 ? transformData.getDuration() * 20 : Integer.MAX_VALUE;
                player.addPotionEffect(new PotionEffect(
                    effectType,
                    duration,
                    effect.getAmplifier(),
                    false,
                    true,
                    true
                ));
            }
        }

        // Crear entidad de transformación visual
        spawnTransformEntity(player, transformData);
    }

    /**
     * Genera la entidad visual de la transformación.
     */
    private void spawnTransformEntity(Player player, TransformationData transformData) {
        UUID uuid = player.getUniqueId();
        Location loc = player.getLocation();

        // Hacer invisible al jugador
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.INVISIBILITY,
            transformData.getDuration() > 0 ? transformData.getDuration() * 20 : Integer.MAX_VALUE,
            0,
            false,
            false,
            false
        ));

        // Crear la entidad (ligeramente por debajo para evitar colisión inicial)
        Location spawnLoc = loc.clone().subtract(0, 0.15, 0);
        Entity entity;
        try {
            entity = player.getWorld().spawnEntity(spawnLoc, transformData.getEntityType());
        } catch (Exception e) {
            plugin.getLogger().severe("Error al crear entidad de transformación: " + e.getMessage());
            player.sendMessage("§c✗ Error al crear la forma de " + transformData.getEntityType().name());
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            return;
        }
        if (!(entity instanceof LivingEntity)) {
            entity.remove();
            player.sendMessage("§c✗ No se pudo crear la entidad de transformación");
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            return;
        }

        LivingEntity livingEntity = (LivingEntity) entity;

        // Configurar la entidad
        livingEntity.setCustomName("§b" + player.getName() + " §7[Transformado]");
        livingEntity.setCustomNameVisible(true);
        livingEntity.setAI(false); // Desactivar IA - nosotros la controlamos
        livingEntity.setInvulnerable(true); // El daño lo recibe el jugador
        livingEntity.setSilent(true);
        livingEntity.setCollidable(false); // Evitar que empuje al jugador
        livingEntity.setGravity(false); // Sin gravedad para evitar física de empuje
        livingEntity.setMetadata("rpg-transform-entity", new FixedMetadataValue(plugin, uuid.toString()));

        // Configurar propiedades específicas del mob
        if (livingEntity instanceof Tameable) {
            ((Tameable) livingEntity).setTamed(true);
        }
        if (livingEntity instanceof Ageable) {
            ((Ageable) livingEntity).setAdult();
        }
        if (livingEntity instanceof Wolf) {
            Wolf wolf = (Wolf) livingEntity;
            wolf.setAngry(false);
            wolf.setSitting(false);
            wolf.setCollidable(false);
        }
        if (livingEntity instanceof Spider) {
            livingEntity.setCollidable(false);
        }
        if (livingEntity instanceof org.bukkit.entity.Warden) {
            org.bukkit.entity.Warden warden = (org.bukkit.entity.Warden) livingEntity;
            warden.setCollidable(false);
            warden.setAware(false);
        }
        if (livingEntity instanceof Mob) {
            ((Mob) livingEntity).setTarget(null);
        }

        transformEntities.put(uuid, livingEntity);

        // Agregar jugador y entidad al mismo team para evitar colisiones
        addToNoCollisionTeam(player, livingEntity);

        // Iniciar tarea de seguimiento
        startFollowTask(player, livingEntity);
    }

    /**
     * Agrega al jugador y la entidad a un team sin colisiones.
     */
    private void addToNoCollisionTeam(Player player, LivingEntity entity) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "rpg_transform_" + player.getUniqueId().toString().substring(0, 8);

        // Obtener o crear el team
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        // Configurar el team para no tener colisiones
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

        // Agregar jugador y entidad al team
        team.addEntry(player.getName());
        team.addEntry(entity.getUniqueId().toString());
    }

    /**
     * Remueve al jugador del team de transformación.
     */
    private void removeFromNoCollisionTeam(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "rpg_transform_" + player.getUniqueId().toString().substring(0, 8);

        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.unregister();
        }
    }

    /**
     * Inicia la tarea que hace que la entidad siga al jugador.
     */
    private void startFollowTask(Player player, LivingEntity entity) {
        UUID uuid = player.getUniqueId();

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || entity.isDead() || !isTransformed(uuid)) {
                BukkitTask t = followTasks.remove(uuid);
                if (t != null) t.cancel();
                return;
            }

            // Teleportar la entidad a la posición del jugador (ligeramente por debajo para evitar colisión)
            Location playerLoc = player.getLocation();
            Location entityLoc = playerLoc.clone().subtract(0, 0.15, 0);
            entity.teleport(entityLoc);

            // Sincronizar rotación
            entity.setRotation(playerLoc.getYaw(), playerLoc.getPitch());

            // Resetear velocidad de la entidad para evitar cualquier empuje
            entity.setVelocity(new Vector(0, 0, 0));

        }, 1L, 1L); // Cada tick para movimiento suave

        followTasks.put(uuid, task);
    }

    /**
     * Obtiene el valor de un stat del jugador por nombre.
     */
    private double getStatValue(PlayerStats stats, String statName) {
        if (statName == null) return 0;
        switch (statName.toLowerCase()) {
            case "health": return stats.getHealth();
            case "strength": return stats.getStrength();
            case "agility": return stats.getAgility();
            case "intelligence": return stats.getIntelligence();
            case "sacred-power": return stats.getSacredPower();
            case "corrupt-power": return stats.getCorruptPower();
            case "nature-power": return stats.getNaturePower();
            default: return 0;
        }
    }

    /**
     * Establece el valor de un atributo.
     */
    private void setAttributeValue(Player player, Attribute attribute, double value) {
        AttributeInstance attr = player.getAttribute(attribute);
        if (attr != null) {
            attr.setBaseValue(value);
        }
    }

    /**
     * Obtiene el PotionEffectType por nombre.
     */
    private PotionEffectType getPotionEffectType(String name) {
        if (name == null) return null;

        switch (name.toUpperCase()) {
            case "SPEED": return PotionEffectType.SPEED;
            case "SLOWNESS": return PotionEffectType.SLOWNESS;
            case "HASTE": return PotionEffectType.HASTE;
            case "MINING_FATIGUE": return PotionEffectType.MINING_FATIGUE;
            case "STRENGTH": return PotionEffectType.STRENGTH;
            case "INSTANT_HEALTH": return PotionEffectType.INSTANT_HEALTH;
            case "INSTANT_DAMAGE": return PotionEffectType.INSTANT_DAMAGE;
            case "JUMP_BOOST": return PotionEffectType.JUMP_BOOST;
            case "NAUSEA": return PotionEffectType.NAUSEA;
            case "REGENERATION": return PotionEffectType.REGENERATION;
            case "RESISTANCE": return PotionEffectType.RESISTANCE;
            case "FIRE_RESISTANCE": return PotionEffectType.FIRE_RESISTANCE;
            case "WATER_BREATHING": return PotionEffectType.WATER_BREATHING;
            case "INVISIBILITY": return PotionEffectType.INVISIBILITY;
            case "BLINDNESS": return PotionEffectType.BLINDNESS;
            case "NIGHT_VISION": return PotionEffectType.NIGHT_VISION;
            case "HUNGER": return PotionEffectType.HUNGER;
            case "WEAKNESS": return PotionEffectType.WEAKNESS;
            case "POISON": return PotionEffectType.POISON;
            case "WITHER": return PotionEffectType.WITHER;
            case "SATURATION": return PotionEffectType.SATURATION;
            case "GLOWING": return PotionEffectType.GLOWING;
            case "LEVITATION": return PotionEffectType.LEVITATION;
            case "LUCK": return PotionEffectType.LUCK;
            case "UNLUCK": return PotionEffectType.UNLUCK;
            case "SLOW_FALLING": return PotionEffectType.SLOW_FALLING;
            case "CONDUIT_POWER": return PotionEffectType.CONDUIT_POWER;
            case "DOLPHINS_GRACE": return PotionEffectType.DOLPHINS_GRACE;
            case "BAD_OMEN": return PotionEffectType.BAD_OMEN;
            case "HERO_OF_THE_VILLAGE": return PotionEffectType.HERO_OF_THE_VILLAGE;
            case "DARKNESS": return PotionEffectType.DARKNESS;
            default:
                plugin.getLogger().warning("Efecto de poción desconocido: " + name);
                return null;
        }
    }

    /**
     * Programa la expiración automática de la transformación.
     */
    private void scheduleExpiration(Player player, int durationSeconds) {
        UUID uuid = player.getUniqueId();

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            expirationTasks.remove(uuid);
            if (isTransformed(uuid)) {
                player.sendMessage("§e⏰ Tu transformación ha expirado.");
                revertTransformation(player);
            }
        }, durationSeconds * 20L);

        expirationTasks.put(uuid, task);
    }

    /**
     * Limpia todos los datos de un jugador (al desconectarse).
     */
    public void cleanupPlayer(UUID uuid) {
        // Revertir transformación si está activa
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && isTransformed(uuid)) {
            revertTransformation(player);
        }

        activeTransformations.remove(uuid);
        originalStates.remove(uuid);
        originalAbilitySlots.remove(uuid);

        // Eliminar entidad de transformación
        LivingEntity entity = transformEntities.remove(uuid);
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }

        // Cancelar tareas
        BukkitTask expTask = expirationTasks.remove(uuid);
        if (expTask != null) expTask.cancel();

        BukkitTask followTask = followTasks.remove(uuid);
        if (followTask != null) followTask.cancel();
    }

    /**
     * Revierte todas las transformaciones activas (al apagar el servidor).
     */
    public void revertAllTransformations() {
        for (UUID uuid : new HashSet<>(activeTransformations.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                revertTransformation(player);
            }
        }
    }

    // ==================== CLASES INTERNAS ====================

    /**
     * Datos de una transformación activa.
     */
    public static class TransformationData {
        private final String abilityId;
        private EntityType entityType;
        private int customModelData;
        private int duration;
        private int healthBonus;
        private int strengthBonus;
        private int agilityBonus;
        private int defenseBonus;
        // Escalado desde stats del jugador
        private double healthScaling;
        private double strengthScaling;
        private double agilityScaling;
        private double attackSpeedScaling;
        private double movementSpeedScaling;
        private double armorScaling;
        // Qué stat del jugador afecta cada atributo
        private String healthScalingStat = "health";
        private String strengthScalingStat = "strength";
        private String agilityScalingStat = "agility";
        private String attackSpeedScalingStat = "agility";
        private String movementSpeedScalingStat = "agility";
        private String armorScalingStat = "intelligence";
        private List<TransformationEffect> effects = new ArrayList<>();
        private List<String> abilities = new ArrayList<>();

        public TransformationData(String abilityId) {
            this.abilityId = abilityId;
        }

        // Getters y Setters
        public String getAbilityId() { return abilityId; }
        public EntityType getEntityType() { return entityType; }
        public void setEntityType(EntityType entityType) { this.entityType = entityType; }
        public int getCustomModelData() { return customModelData; }
        public void setCustomModelData(int customModelData) { this.customModelData = customModelData; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
        public int getHealthBonus() { return healthBonus; }
        public void setHealthBonus(int healthBonus) { this.healthBonus = healthBonus; }
        public int getStrengthBonus() { return strengthBonus; }
        public void setStrengthBonus(int strengthBonus) { this.strengthBonus = strengthBonus; }
        public int getAgilityBonus() { return agilityBonus; }
        public void setAgilityBonus(int agilityBonus) { this.agilityBonus = agilityBonus; }
        public int getDefenseBonus() { return defenseBonus; }
        public void setDefenseBonus(int defenseBonus) { this.defenseBonus = defenseBonus; }
        public double getHealthScaling() { return healthScaling; }
        public void setHealthScaling(double healthScaling) { this.healthScaling = healthScaling; }
        public double getStrengthScaling() { return strengthScaling; }
        public void setStrengthScaling(double strengthScaling) { this.strengthScaling = strengthScaling; }
        public double getAgilityScaling() { return agilityScaling; }
        public void setAgilityScaling(double agilityScaling) { this.agilityScaling = agilityScaling; }
        public double getAttackSpeedScaling() { return attackSpeedScaling; }
        public void setAttackSpeedScaling(double attackSpeedScaling) { this.attackSpeedScaling = attackSpeedScaling; }
        public double getMovementSpeedScaling() { return movementSpeedScaling; }
        public void setMovementSpeedScaling(double movementSpeedScaling) { this.movementSpeedScaling = movementSpeedScaling; }
        public double getArmorScaling() { return armorScaling; }
        public void setArmorScaling(double armorScaling) { this.armorScaling = armorScaling; }
        public String getHealthScalingStat() { return healthScalingStat; }
        public void setHealthScalingStat(String healthScalingStat) { this.healthScalingStat = healthScalingStat; }
        public String getStrengthScalingStat() { return strengthScalingStat; }
        public void setStrengthScalingStat(String strengthScalingStat) { this.strengthScalingStat = strengthScalingStat; }
        public String getAgilityScalingStat() { return agilityScalingStat; }
        public void setAgilityScalingStat(String agilityScalingStat) { this.agilityScalingStat = agilityScalingStat; }
        public String getAttackSpeedScalingStat() { return attackSpeedScalingStat; }
        public void setAttackSpeedScalingStat(String attackSpeedScalingStat) { this.attackSpeedScalingStat = attackSpeedScalingStat; }
        public String getMovementSpeedScalingStat() { return movementSpeedScalingStat; }
        public void setMovementSpeedScalingStat(String movementSpeedScalingStat) { this.movementSpeedScalingStat = movementSpeedScalingStat; }
        public String getArmorScalingStat() { return armorScalingStat; }
        public void setArmorScalingStat(String armorScalingStat) { this.armorScalingStat = armorScalingStat; }
        public List<TransformationEffect> getEffects() { return effects; }
        public void addEffect(TransformationEffect effect) { effects.add(effect); }
        public List<String> getAbilities() { return abilities; }
        public void setAbilities(List<String> abilities) { this.abilities = abilities; }
    }

    /**
     * Efecto de poción de una transformación.
     */
    public static class TransformationEffect {
        private final String type;
        private final int amplifier;

        public TransformationEffect(String type, int amplifier) {
            this.type = type;
            this.amplifier = amplifier;
        }

        public String getType() { return type; }
        public int getAmplifier() { return amplifier; }
    }

    /**
     * Estado original del jugador antes de transformarse.
     */
    private static class OriginalPlayerState {
        final int health;
        final int strength;
        final int agility;
        final int intelligence;
        final double maxHealth;
        final double attackSpeed;
        final double movementSpeed;
        final double armor;
        final boolean wasInvisible;

        OriginalPlayerState(int health, int strength, int agility, int intelligence,
                           double maxHealth, double attackSpeed, double movementSpeed,
                           double armor, boolean wasInvisible) {
            this.health = health;
            this.strength = strength;
            this.agility = agility;
            this.intelligence = intelligence;
            this.maxHealth = maxHealth;
            this.attackSpeed = attackSpeed;
            this.movementSpeed = movementSpeed;
            this.armor = armor;
            this.wasInvisible = wasInvisible;
        }
    }
}
