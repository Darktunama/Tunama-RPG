package com.irdem.tunama.managers;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.*;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;

/**
 * Manager principal del sistema de mascotas de combate
 */
public class PetManager {

    private final TunamaRPG plugin;
    private final Map<UUID, List<Pet>> activePets;      // Mascotas activas por jugador
    private final Map<String, PetType> petTypes;         // Tipos cargados de YML
    private final Map<UUID, Entity> petEntities;         // Entidades de mascotas -> para rastreo inverso
    private BukkitTask aiTask;
    private File petsFolder;

    public PetManager(TunamaRPG plugin) {
        this.plugin = plugin;
        this.activePets = new HashMap<>();
        this.petTypes = new HashMap<>();
        this.petEntities = new HashMap<>();
        this.petsFolder = new File(plugin.getDataFolder(), "mascotas");

        if (!petsFolder.exists()) {
            petsFolder.mkdirs();
        }

        loadPetTypes();
        startAITask();
    }

    /**
     * Carga los tipos de mascotas desde archivos YML
     */
    public void loadPetTypes() {
        petTypes.clear();

        File[] files = petsFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null || files.length == 0) {
            plugin.getLogger().info("No se encontraron archivos de mascotas. Creando mascotas por defecto...");
            createDefaultPetTypes();
            files = petsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        }

        if (files != null) {
            for (File file : files) {
                loadPetTypeFromFile(file);
            }
        }

        plugin.getLogger().info("Se cargaron " + petTypes.size() + " tipos de mascotas");
    }

    private void loadPetTypeFromFile(File file) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = config.getString("id");
            String name = config.getString("name");

            if (id == null || name == null) {
                plugin.getLogger().warning("Archivo de mascota incompleto: " + file.getName());
                return;
            }

            PetType type = new PetType();
            type.setId(id);
            type.setName(name);
            type.setDescription(config.getString("description", ""));
            type.setRole(config.getString("role", "dps"));
            type.setCustomModelData(config.getInt("custom-model-data", 0));

            // Entity type
            String entityTypeStr = config.getString("entity-type", "WOLF");
            try {
                type.setEntityType(EntityType.valueOf(entityTypeStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                type.setEntityType(EntityType.WOLF);
            }

            // Stats - nuevo formato con sección stats
            if (config.isConfigurationSection("stats")) {
                type.setBaseHealth(config.getInt("stats.health", 40));
                type.setBaseDefense(config.getInt("stats.defense", 5));
                type.setBaseAttack(config.getInt("stats.attack", 8));
                type.setBaseAttackSpeed(config.getDouble("stats.attack-speed", 1.0));
                type.setBaseMovementSpeed(config.getDouble("stats.movement-speed", 1.0));
            } else {
                // Compatibilidad con formato anterior
                type.setBaseHealth(config.getInt("base-health", 40));
                type.setBaseDamage(config.getInt("base-damage", 8));
                type.setBaseArmor(config.getInt("base-armor", 5));
            }

            // Growth - crecimiento por nivel
            if (config.isConfigurationSection("growth")) {
                type.setHealthPerLevel(config.getDouble("growth.health-per-level", 8));
                type.setDefensePerLevel(config.getDouble("growth.defense-per-level", 1));
                type.setAttackPerLevel(config.getDouble("growth.attack-per-level", 2));
                type.setAttackSpeedPerLevel(config.getDouble("growth.attack-speed-per-level", 0.02));
                type.setMovementSpeedPerLevel(config.getDouble("growth.movement-speed-per-level", 0.01));
            } else {
                // Compatibilidad con formato anterior
                type.setHealthPerLevel(config.getDouble("health-per-level", 8));
                type.setDamagePerLevel(config.getDouble("damage-per-level", 2));
            }

            // Escalado desde stats del jugador (porcentaje 0.0 - 1.0)
            if (config.isConfigurationSection("player-scaling")) {
                type.setHealthFromPlayer(config.getDouble("player-scaling.health", 0.0));
                type.setAttackFromPlayer(config.getDouble("player-scaling.attack", 0.0));
                type.setDefenseFromPlayer(config.getDouble("player-scaling.defense", 0.0));
                type.setAttackSpeedFromPlayer(config.getDouble("player-scaling.attack-speed", 0.0));
                type.setMovementSpeedFromPlayer(config.getDouble("player-scaling.movement-speed", 0.0));
            }

            type.setRequiredLevel(config.getInt("required-level", 1));

            // Menu icon
            String iconStr = config.getString("menu-icon", "EGG");
            try {
                type.setMenuIcon(Material.valueOf(iconStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                type.setMenuIcon(Material.EGG);
            }

            // Allowed classes
            type.setAllowedClasses(config.getStringList("allowed-classes"));

            // Tienda
            type.setPrice(config.getDouble("price", 500));
            type.setPermission(config.getString("permission", ""));

            // Cargar habilidades
            if (config.isConfigurationSection("abilities")) {
                List<Map<?, ?>> abilitiesList = config.getMapList("abilities");
                for (Map<?, ?> abilityMap : abilitiesList) {
                    PetAbility ability = new PetAbility();
                    ability.setId((String) abilityMap.get("id"));
                    ability.setName((String) abilityMap.get("name"));
                    ability.setDescription((String) abilityMap.get("description"));

                    if (abilityMap.get("cooldown") instanceof Number) {
                        ability.setCooldown(((Number) abilityMap.get("cooldown")).intValue());
                    }
                    if (abilityMap.get("damage-multiplier") instanceof Number) {
                        ability.setDamageMultiplier(((Number) abilityMap.get("damage-multiplier")).doubleValue());
                    }

                    // Cargar propiedades adicionales
                    for (Map.Entry<?, ?> entry : abilityMap.entrySet()) {
                        String key = (String) entry.getKey();
                        if (!key.equals("id") && !key.equals("name") && !key.equals("description")
                            && !key.equals("cooldown") && !key.equals("damage-multiplier")) {
                            ability.setProperty(key, entry.getValue());
                        }
                    }

                    type.addAbility(ability);
                }
            }

            petTypes.put(id, type);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al cargar mascota desde " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createDefaultPetTypes() {
        // Lista de todas las mascotas que deben copiarse desde resources
        String[] defaultPets = {
            "lobo.yml",
            "zombie-sirviente.yml",
            "esqueleto-arquero.yml",
            "pantera-sombria.yml",
            "araña-venenosa.yml",
            "espiritu-del-bosque.yml",
            "golem-de-hierro.yml",
            "oso-guardian.yml",
            "fenix-menor.yml"
        };

        for (String petFileName : defaultPets) {
            File destFile = new File(petsFolder, petFileName);
            if (!destFile.exists()) {
                // Copiar desde resources/mascotas/
                try {
                    plugin.saveResource("mascotas/" + petFileName, false);
                    plugin.getLogger().info("Creado archivo de mascota: " + petFileName);
                } catch (Exception e) {
                    plugin.getLogger().warning("No se pudo crear mascota: " + petFileName + " - " + e.getMessage());
                }
            }
        }
    }

    /**
     * Invoca una mascota para un jugador
     */
    public boolean summonPet(Player player, Pet pet) {
        UUID uuid = player.getUniqueId();
        List<Pet> playerPets = activePets.computeIfAbsent(uuid, k -> new ArrayList<>());

        // Verificar límite
        int maxPets = getMaxPets(player);
        if (playerPets.size() >= maxPets) {
            player.sendMessage("§c✗ Ya tienes el máximo de mascotas activas (" + maxPets + ")");
            return false;
        }

        // Verificar que no esté ya activa
        if (pet.isActive()) {
            player.sendMessage("§c✗ Esta mascota ya está invocada");
            return false;
        }

        // Verificar que no esté muerta
        if (pet.isDead()) {
            player.sendMessage("§c✗ Esta mascota está muerta. Usa §eResucitar Mascota §cpara revivirla");
            return false;
        }

        PetType type = petTypes.get(pet.getTypeId());
        if (type == null) {
            player.sendMessage("§c✗ Tipo de mascota desconocido");
            return false;
        }

        // Obtener stats del jugador para escalar stats de la mascota
        PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        int playerHealth = 100; // Default
        int playerAttack = 10;  // Default
        int playerAgility = 5;  // Default (afecta defensa, vel. ataque y movimiento)

        if (playerData != null && playerData.getStats() != null) {
            playerHealth = playerData.getStats().getHealth();
            playerAttack = playerData.getStats().getStrength();
            playerAgility = playerData.getStats().getAgility();
        }

        // Calcular stats con escalado del jugador
        pet.calculateStats(type, playerHealth, playerAttack, playerAgility);

        // Spawn entity
        Location spawnLoc = player.getLocation().add(
                player.getLocation().getDirection().multiply(-2).setY(0)
        );

        Entity entity = player.getWorld().spawnEntity(spawnLoc, type.getEntityType());

        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;

            // Configurar vida PRIMERO (antes del nombre para que la barra de vida muestre bien)
            org.bukkit.attribute.AttributeInstance healthAttr = living.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
            if (healthAttr != null) {
                healthAttr.setBaseValue(pet.getMaxHealth());

                // Usar la vida máxima REAL de la entidad (puede diferir si hay modificadores)
                double actualMaxHealth = living.getMaxHealth();

                // Asegurar que currentHealth no exceda el máximo real de la entidad
                int targetHealth = Math.min(pet.getCurrentHealth(), (int) actualMaxHealth);
                if (targetHealth <= 0) targetHealth = (int) actualMaxHealth; // Nueva mascota = vida completa

                // Establecer la vida de la entidad
                living.setHealth(Math.min(targetHealth, actualMaxHealth));

                // Sincronizar el objeto Pet con los valores reales
                pet.setCurrentHealth((int) living.getHealth());
            }

            // Configurar entidad con nombre y barra de vida (después de configurar la vida)
            updatePetNameWithHealthBar(living, pet);
            living.setCustomNameVisible(true);

            // Marcar como mascota RPG
            living.setMetadata("rpg-pet", new FixedMetadataValue(plugin, pet.getId()));
            living.setMetadata("rpg-pet-owner", new FixedMetadataValue(plugin, uuid.toString()));

            // Marcar con PersistentDataContainer (persiste entre reinicios)
            org.bukkit.NamespacedKey petPdcKey = new org.bukkit.NamespacedKey(plugin, "rpg-pet");
            living.getPersistentDataContainer().set(petPdcKey,
                org.bukkit.persistence.PersistentDataType.STRING, uuid.toString());

            // Hacer que no despawnee
            living.setPersistent(true);
            living.setRemoveWhenFarAway(false);

            // Configuraciones específicas por tipo
            if (entity instanceof Wolf) {
                Wolf wolf = (Wolf) entity;
                wolf.setTamed(true);
                wolf.setOwner(player);
                wolf.setSitting(false);
            } else if (entity instanceof Zombie) {
                Zombie zombie = (Zombie) entity;
                zombie.setBaby(false);
            }

            pet.setEntity(entity);
            pet.setActive(true);
            pet.setCurrentCommand(PetCommand.FOLLOW);

            playerPets.add(pet);
            petEntities.put(UUID.fromString(pet.getId()), entity);

            player.sendMessage("§a✓ ¡" + pet.getDisplayName() + " ha sido invocado!");
            return true;
        }

        // Si no se pudo crear como LivingEntity, eliminar
        entity.remove();
        return false;
    }

    /**
     * Guarda una mascota (la desinvoca)
     */
    public void dismissPet(Player player, Pet pet) {
        if (!pet.isActive()) return;

        UUID uuid = player.getUniqueId();
        List<Pet> playerPets = activePets.get(uuid);

        if (playerPets != null) {
            playerPets.remove(pet);
        }

        // Guardar vida actual
        if (pet.getEntity() instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) pet.getEntity();
            pet.setCurrentHealth((int) living.getHealth());
        }

        // Eliminar entidad
        if (pet.getEntity() != null && !pet.getEntity().isDead()) {
            pet.getEntity().remove();
        }

        petEntities.remove(UUID.fromString(pet.getId()));
        pet.setEntity(null);
        pet.setActive(false);
        pet.setTarget(null);

        // Guardar en base de datos
        plugin.getDatabaseManager().savePet(pet);

        player.sendMessage("§a✓ " + pet.getDisplayName() + " ha sido guardado");
    }

    /**
     * Guarda todas las mascotas de un jugador
     */
    public void dismissAllPets(Player player) {
        UUID uuid = player.getUniqueId();
        List<Pet> playerPets = activePets.get(uuid);

        if (playerPets == null || playerPets.isEmpty()) return;

        // Copiar la lista para evitar ConcurrentModificationException
        List<Pet> petsToSave = new ArrayList<>(playerPets);
        for (Pet pet : petsToSave) {
            dismissPet(player, pet);
        }

        activePets.remove(uuid);
    }

    /**
     * Obtiene las mascotas activas de un jugador
     */
    public List<Pet> getActivePets(Player player) {
        return activePets.getOrDefault(player.getUniqueId(), new ArrayList<>());
    }

    /**
     * Obtiene las mascotas guardadas de un jugador desde la base de datos
     */
    public List<Pet> getStoredPets(UUID uuid, int characterSlot) {
        return plugin.getDatabaseManager().loadPlayerPets(uuid, characterSlot);
    }

    /**
     * Obtiene el máximo de mascotas que puede tener un jugador
     */
    public int getMaxPets(Player player) {
        PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return 1;

        String playerClass = playerData.getPlayerClass();
        if (playerClass == null || playerClass.isEmpty()) return 1;

        RPGClass rpgClass = plugin.getClassManager().getClass(playerClass.toLowerCase());
        int basePets = 0;

        if (rpgClass != null && rpgClass.hasPets()) {
            basePets = rpgClass.getMaxPets();
        } else if (player.hasPermission("rpg.pets")) {
            basePets = 1;
        }

        // Verificar si tiene la habilidad pasiva "segunda-mascota" desbloqueada
        if (basePets > 0 && hasPassiveAbility(playerData, "segunda-mascota")) {
            basePets += 1;
        }

        return basePets;
    }

    /**
     * Verifica si el jugador tiene una habilidad pasiva desbloqueada
     */
    private boolean hasPassiveAbility(PlayerData playerData, String abilityId) {
        com.irdem.tunama.data.Ability ability = plugin.getAbilityManager().getAbility(abilityId);
        if (ability == null) return false;
        if (!ability.isPassive()) return false;

        // Verificar que la clase del jugador coincida
        String playerClass = playerData.getPlayerClass();
        if (playerClass == null) return false;
        if (!ability.getRpgClass().equalsIgnoreCase(playerClass) &&
            !ability.getRpgClass().equalsIgnoreCase("General")) {
            return false;
        }

        // Verificar nivel requerido
        return playerData.getLevel() >= ability.getRequiredLevel();
    }

    /**
     * Verifica si un jugador puede tener mascotas
     */
    public boolean canHavePets(Player player) {
        // Verificar permiso
        if (player.hasPermission("rpg.pets")) return true;

        // Verificar clase
        PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return false;

        String playerClass = playerData.getPlayerClass();
        if (playerClass == null || playerClass.isEmpty()) return false;

        RPGClass rpgClass = plugin.getClassManager().getClass(playerClass.toLowerCase());
        return rpgClass != null && rpgClass.hasPets();
    }

    /**
     * Envía un comando a una mascota específica
     */
    public void commandPet(Player player, Pet pet, PetCommand cmd) {
        if (!pet.isActive()) return;

        pet.setCurrentCommand(cmd);

        String cmdName = cmd.getDisplayName();
        player.sendMessage("§a✓ " + pet.getDisplayName() + " ahora está en modo: §e" + cmdName);

        // Si es STAY, detener movimiento
        if (cmd == PetCommand.STAY && pet.getEntity() instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) pet.getEntity();
            living.setAI(false);
            // Reactivar después de un tick para que se quede quieto
            Bukkit.getScheduler().runTaskLater(plugin, () -> living.setAI(true), 1L);
        }
    }

    /**
     * Envía un comando a todas las mascotas de un jugador
     */
    public void commandAllPets(Player player, PetCommand cmd) {
        List<Pet> pets = getActivePets(player);
        for (Pet pet : pets) {
            commandPet(player, pet, cmd);
        }
    }

    /**
     * Marca un objetivo para las mascotas del jugador
     */
    public void setTargetForPets(Player player, Entity target) {
        List<Pet> pets = getActivePets(player);
        for (Pet pet : pets) {
            if (pet.getCurrentCommand() != PetCommand.STAY) {
                pet.setTarget(target);

                // Hacer que ataque
                if (pet.getEntity() instanceof Mob && target instanceof LivingEntity) {
                    Mob mob = (Mob) pet.getEntity();
                    mob.setTarget((LivingEntity) target);
                }
            }
        }
    }

    /**
     * Obtiene la mascota por su entidad
     */
    public Pet getPetByEntity(Entity entity) {
        if (!entity.hasMetadata("rpg-pet")) return null;

        String petId = entity.getMetadata("rpg-pet").get(0).asString();

        for (List<Pet> pets : activePets.values()) {
            for (Pet pet : pets) {
                if (pet.getId().equals(petId)) {
                    return pet;
                }
            }
        }
        return null;
    }

    /**
     * Obtiene el dueño de una mascota por su entidad
     */
    public Player getOwnerByPetEntity(Entity entity) {
        if (!entity.hasMetadata("rpg-pet-owner")) return null;

        String ownerUuid = entity.getMetadata("rpg-pet-owner").get(0).asString();
        return Bukkit.getPlayer(UUID.fromString(ownerUuid));
    }

    /**
     * Verifica si una entidad es una mascota RPG
     */
    public boolean isRpgPet(Entity entity) {
        return entity.hasMetadata("rpg-pet");
    }

    /**
     * Obtiene un tipo de mascota por ID
     */
    public PetType getPetType(String id) {
        return petTypes.get(id);
    }

    /**
     * Obtiene todos los tipos de mascotas
     */
    public Map<String, PetType> getAllPetTypes() {
        return new HashMap<>(petTypes);
    }

    /**
     * Obtiene los tipos de mascotas disponibles para un jugador
     */
    public List<PetType> getAvailablePetTypes(Player player) {
        List<PetType> available = new ArrayList<>();
        PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return available;

        String playerClass = playerData.getPlayerClass();
        int playerLevel = playerData.getLevel();

        for (PetType type : petTypes.values()) {
            // Verificar nivel
            if (playerLevel < type.getRequiredLevel()) continue;

            // Verificar clase
            if (!type.isClassAllowed(playerClass)) continue;

            available.add(type);
        }

        return available;
    }

    /**
     * Inicia el task de IA de mascotas
     */
    public void startAITask() {
        if (aiTask != null) {
            aiTask.cancel();
        }

        aiTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, List<Pet>> entry : activePets.entrySet()) {
                Player owner = Bukkit.getPlayer(entry.getKey());
                if (owner == null || !owner.isOnline()) continue;

                for (Pet pet : entry.getValue()) {
                    updatePetAI(owner, pet);
                }
            }
        }, 20L, 10L); // Cada 0.5 segundos
    }

    private void updatePetAI(Player owner, Pet pet) {
        if (!pet.isActive() || pet.getEntity() == null || pet.getEntity().isDead()) return;

        LivingEntity entity = pet.getLivingEntity();
        if (entity == null) return;

        // Sincronizar vida de la entidad con el Pet y actualizar barra de vida
        int entityHealth = (int) entity.getHealth();
        if (entityHealth != pet.getCurrentHealth()) {
            pet.setCurrentHealth(entityHealth);
            updatePetNameWithHealthBar(entity, pet);
        }

        Location petLoc = entity.getLocation();
        Location ownerLoc = owner.getLocation();

        // Teletransportar si está muy lejos
        if (petLoc.getWorld() != ownerLoc.getWorld() || petLoc.distance(ownerLoc) > 30) {
            entity.teleport(ownerLoc.add(owner.getLocation().getDirection().multiply(-2).setY(0)));
            return;
        }

        // Comportamiento según comando
        switch (pet.getCurrentCommand()) {
            case FOLLOW:
                if (petLoc.distance(ownerLoc) > 5 && entity instanceof Mob) {
                    Mob mob = (Mob) entity;
                    // Limpiar objetivo y moverse hacia el dueño
                    if (mob.getTarget() == null || mob.getTarget().isDead()) {
                        // El pathfinding nativo del mob lo hará seguir si es tameable
                        // Para otros mobs, usamos setTarget null y confiamos en el tick
                    }
                }
                break;

            case STAY:
                // No hacer nada, la mascota se queda quieta
                if (entity instanceof Mob) {
                    ((Mob) entity).setTarget(null);
                }
                break;

            case ATTACK:
                // Atacar objetivo marcado
                if (pet.getTarget() != null && !pet.getTarget().isDead() && entity instanceof Mob) {
                    Mob mob = (Mob) entity;
                    if (pet.getTarget() instanceof LivingEntity) {
                        mob.setTarget((LivingEntity) pet.getTarget());
                    }
                }
                break;

            case DEFEND:
                // Quedarse cerca del dueño, atacar si el dueño es atacado
                if (petLoc.distance(ownerLoc) > 8 && entity instanceof Mob) {
                    ((Mob) entity).setTarget(null);
                }
                // El listener de daño se encarga de poner el target cuando el dueño es atacado
                break;

            case AGGRESSIVE:
                // Atacar mobs hostiles cercanos
                if (entity instanceof Mob) {
                    Mob mob = (Mob) entity;
                    if (mob.getTarget() == null || mob.getTarget().isDead()) {
                        // Buscar mob hostil cercano
                        for (Entity nearby : entity.getNearbyEntities(10, 10, 10)) {
                            if (nearby instanceof Monster && !isRpgPet(nearby)) {
                                mob.setTarget((LivingEntity) nearby);
                                break;
                            }
                        }
                    }
                }
                break;
        }
    }

    /**
     * Detiene el task de IA
     */
    public void stopAITask() {
        if (aiTask != null) {
            aiTask.cancel();
            aiTask = null;
        }
    }

    /**
     * Guarda y elimina todas las mascotas activas de todos los jugadores.
     * Usado al cerrar el servidor para evitar duplicación.
     */
    public void dismissAllPetsOnShutdown() {
        plugin.getLogger().info("Guardando todas las mascotas activas...");
        int savedCount = 0;

        // Iterar sobre todas las mascotas activas
        for (Map.Entry<UUID, List<Pet>> entry : new HashMap<>(activePets).entrySet()) {
            UUID ownerUuid = entry.getKey();
            List<Pet> pets = entry.getValue();

            for (Pet pet : new ArrayList<>(pets)) {
                if (pet.isActive()) {
                    // Guardar vida actual antes de eliminar
                    if (pet.getEntity() instanceof LivingEntity) {
                        LivingEntity living = (LivingEntity) pet.getEntity();
                        if (!living.isDead()) {
                            pet.setCurrentHealth((int) living.getHealth());
                        }
                    }

                    // Eliminar la entidad del mundo
                    if (pet.getEntity() != null && !pet.getEntity().isDead()) {
                        pet.getEntity().remove();
                    }

                    // Marcar como inactiva
                    pet.setEntity(null);
                    pet.setActive(false);
                    pet.setTarget(null);

                    // Guardar en base de datos
                    plugin.getDatabaseManager().savePet(pet);
                    savedCount++;
                }
            }
        }

        // Limpiar mapas
        activePets.clear();
        petEntities.clear();

        plugin.getLogger().info("Se guardaron " + savedCount + " mascotas correctamente.");
    }

    /**
     * Crea una nueva mascota para un jugador
     */
    public Pet createPet(UUID ownerUuid, int characterSlot, String typeId) {
        PetType type = petTypes.get(typeId);
        if (type == null) return null;

        Pet pet = new Pet(typeId, ownerUuid);
        pet.setCharacterSlot(characterSlot);
        pet.calculateStats(type);
        pet.setCurrentHealth(pet.getMaxHealth());

        // Guardar en base de datos
        plugin.getDatabaseManager().savePet(pet);

        return pet;
    }

    /**
     * Actualiza el nombre de la mascota con la barra de vida
     * Formato: "Tipo de NombreJugador [Nv.X]" + barra de vida
     */
    public void updatePetNameWithHealthBar(LivingEntity entity, Pet pet) {
        if (entity == null || pet == null) return;

        // Obtener el tipo de mascota con primera letra mayúscula
        PetType type = getPetType(pet.getTypeId());
        String typeName = type != null ? type.getName() : capitalize(pet.getTypeId());

        // Obtener el nombre del dueño
        String ownerName = "???";
        Player owner = Bukkit.getPlayer(pet.getOwnerUuid());
        if (owner != null) {
            ownerName = owner.getName();
        } else {
            // Intentar obtener de cache offline
            org.bukkit.OfflinePlayer offlineOwner = Bukkit.getOfflinePlayer(pet.getOwnerUuid());
            if (offlineOwner.getName() != null) {
                ownerName = offlineOwner.getName();
            }
        }

        // Usar los valores REALES de la entidad, no del objeto Pet
        // Esto evita discrepancias entre lo que muestra y lo que realmente tiene
        int currentHealth = (int) entity.getHealth();
        int maxHealth = (int) entity.getMaxHealth();

        // Sincronizar el objeto Pet con los valores reales de la entidad
        pet.setCurrentHealth(currentHealth);

        String healthBar = createHealthBar(currentHealth, maxHealth);
        String petNameFormatted = "§a" + typeName + " §7de §f" + ownerName + " §7[Nv." + pet.getLevel() + "]\n" + healthBar;
        entity.customName(LegacyComponentSerializer.legacySection().deserialize(petNameFormatted));
    }

    /**
     * Capitaliza la primera letra de una cadena
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Crea una barra de vida visual para la mascota
     */
    private String createHealthBar(int currentHealth, int maxHealth) {
        int barLength = 10;
        int healthPercent = maxHealth > 0 ? (currentHealth * 100 / maxHealth) : 0;
        int filledBars = (healthPercent * barLength) / 100;

        StringBuilder bar = new StringBuilder();

        // Color basado en porcentaje de vida
        String healthColor;
        if (healthPercent > 50) {
            healthColor = "§a"; // Verde
        } else if (healthPercent > 25) {
            healthColor = "§e"; // Amarillo
        } else {
            healthColor = "§c"; // Rojo
        }

        bar.append("§8[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                bar.append(healthColor).append("█");
            } else {
                bar.append("§7░");
            }
        }
        bar.append("§8] ").append(healthColor).append(currentHealth).append("§7/§f").append(maxHealth);

        return bar.toString();
    }
}
