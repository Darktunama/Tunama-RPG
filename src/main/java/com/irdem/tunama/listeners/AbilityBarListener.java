package com.irdem.tunama.listeners;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.Ability;
import com.irdem.tunama.data.Pet;
import com.irdem.tunama.util.DamageCalculator;
import com.irdem.tunama.data.PetCommand;
import com.irdem.tunama.data.PetType;
import com.irdem.tunama.data.PlayerData;
import com.irdem.tunama.data.PlayerStats;
import com.irdem.tunama.managers.PetManager;
import com.irdem.tunama.managers.TransformationManager;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sistema de barra de habilidades con tecla F.
 * - Doble-tap F rápido (dentro de 400ms) para activar el modo habilidades
 * - Pulsa F una vez para intercambiar manos normalmente
 * - En modo habilidades, las teclas 1-9 lanzan habilidades directamente
 * - Boss Bar muestra las habilidades asignadas
 */
public class AbilityBarListener implements Listener {

    private final TunamaRPG plugin;

    // Jugadores con modo habilidades activo
    private static final Map<UUID, Boolean> abilityModeEnabled = new HashMap<>();

    // Bindings de habilidades: jugador -> (slot 0-8 -> ability ID)
    private static final Map<UUID, Map<Integer, String>> abilitySlots = new HashMap<>();

    // Boss bars por jugador
    private static final Map<UUID, BossBar> playerBossBars = new HashMap<>();

    // Slot seleccionado antes de entrar en modo habilidades (para restaurar)
    private static final Map<UUID, Integer> previousSlot = new HashMap<>();

    // Items intercambiados temporalmente (para restaurar al salir del modo)
    private static final Map<UUID, ItemStack> swappedSlot8Item = new HashMap<>();

    // Sistema de mantener F: tareas pendientes y timestamps
    private static final Map<UUID, BukkitTask> pendingFTasks = new HashMap<>();
    private static final Map<UUID, Long> fPressTimestamps = new HashMap<>();

    // Cooldowns de habilidades: jugador -> (abilityId -> timestamp de último uso)
    private static final Map<UUID, Map<String, Long>> abilityCooldowns = new HashMap<>();

    // Sistema de cast time: jugadores actualmente casteando
    private static final Map<UUID, BukkitTask> castingTasks = new HashMap<>();
    private static final Map<UUID, org.bukkit.Location> castingStartLocations = new HashMap<>();

    // Traducciones de armas y categorías
    private static final Map<String, String> WEAPON_NAMES = new HashMap<>();
    private static final Map<String, java.util.Set<org.bukkit.Material>> WEAPON_CATEGORIES = new HashMap<>();
    static {
        // Categorías de armas (soportan múltiples materials por categoría)
        WEAPON_CATEGORIES.put("BOW",      java.util.Set.of(org.bukkit.Material.BOW));
        WEAPON_CATEGORIES.put("CROSSBOW", java.util.Set.of(org.bukkit.Material.CROSSBOW));
        WEAPON_CATEGORIES.put("SWORD",    java.util.Set.of(
            org.bukkit.Material.WOODEN_SWORD, org.bukkit.Material.STONE_SWORD,
            org.bukkit.Material.IRON_SWORD,   org.bukkit.Material.GOLDEN_SWORD,
            org.bukkit.Material.DIAMOND_SWORD, org.bukkit.Material.NETHERITE_SWORD));
        WEAPON_CATEGORIES.put("AXE",      java.util.Set.of(
            org.bukkit.Material.WOODEN_AXE, org.bukkit.Material.STONE_AXE,
            org.bukkit.Material.IRON_AXE,   org.bukkit.Material.GOLDEN_AXE,
            org.bukkit.Material.DIAMOND_AXE, org.bukkit.Material.NETHERITE_AXE));
        WEAPON_CATEGORIES.put("STAFF",    java.util.Set.of(org.bukkit.Material.BLAZE_ROD));   // placeholder
        WEAPON_CATEGORIES.put("WAND",     java.util.Set.of(org.bukkit.Material.STICK));        // placeholder
        WEAPON_CATEGORIES.put("GAUNTLET", java.util.Set.of(org.bukkit.Material.IRON_INGOT));   // placeholder

        // Nombres amigables (categorías y materiales individuales)
        WEAPON_NAMES.put("BOW",      "arco");
        WEAPON_NAMES.put("CROSSBOW", "ballesta");
        WEAPON_NAMES.put("SWORD",    "espada");
        WEAPON_NAMES.put("AXE",      "hacha");
        WEAPON_NAMES.put("STAFF",    "bastón");
        WEAPON_NAMES.put("WAND",     "varita");
        WEAPON_NAMES.put("GAUNTLET", "guantelete");
        WEAPON_NAMES.put("DIAMOND_SWORD",   "espada de diamante");
        WEAPON_NAMES.put("NETHERITE_SWORD", "espada de netherita");
        WEAPON_NAMES.put("IRON_SWORD",      "espada de hierro");
        WEAPON_NAMES.put("WOODEN_SWORD",    "espada de madera");
        WEAPON_NAMES.put("STONE_SWORD",     "espada de piedra");
        WEAPON_NAMES.put("GOLDEN_SWORD",    "espada de oro");
        WEAPON_NAMES.put("TRIDENT",  "tridente");
        WEAPON_NAMES.put("SHIELD",   "escudo");
    }

    public AbilityBarListener(TunamaRPG plugin) {
        this.plugin = plugin;
        setPluginInstance(plugin);
    }

    // Tiempo máximo entre pulsaciones para doble-tap (ms)
    private static final long DOUBLE_TAP_THRESHOLD_MS = 400;

    // ==================== TOGGLE CON TECLA F (DOBLE-TAP RÁPIDO) ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Si ya está en modo habilidades, salir con un solo F
        if (abilityModeEnabled.getOrDefault(uuid, false)) {
            event.setCancelled(true);
            disableAbilityMode(player);
            return;
        }

        long now = System.currentTimeMillis();
        Long lastPress = fPressTimestamps.get(uuid);

        // Verificar si es un doble-tap (segunda pulsación dentro del umbral)
        if (lastPress != null && (now - lastPress) < DOUBLE_TAP_THRESHOLD_MS) {
            // ¡Doble-tap detectado! Activar modo habilidades
            event.setCancelled(true);
            fPressTimestamps.remove(uuid);

            // Cancelar la tarea de swap pendiente
            BukkitTask pendingTask = pendingFTasks.remove(uuid);
            if (pendingTask != null) {
                pendingTask.cancel();
            }

            enableAbilityMode(player);
            return;
        }

        // Primera pulsación de F: cancelar swap temporalmente y esperar segunda pulsación
        event.setCancelled(true);
        fPressTimestamps.put(uuid, now);

        // Programar el swap normal si no hay segunda pulsación dentro del umbral
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingFTasks.remove(uuid);
            fPressTimestamps.remove(uuid);

            // Si el jugador sigue online, hacer el swap de manos manualmente
            if (player.isOnline()) {
                ItemStack mainHand = player.getInventory().getItemInMainHand();
                ItemStack offHand = player.getInventory().getItemInOffHand();
                player.getInventory().setItemInMainHand(offHand);
                player.getInventory().setItemInOffHand(mainHand);
            }
        }, 8L); // 400ms = 8 ticks (8 ticks * 50ms = 400ms)

        pendingFTasks.put(uuid, task);
    }

    private void enableAbilityMode(Player player) {
        UUID uuid = player.getUniqueId();

        // Guardar slot actual
        int currentSlot = player.getInventory().getHeldItemSlot();
        previousSlot.put(uuid, currentSlot);

        // Activar modo
        abilityModeEnabled.put(uuid, true);

        // Intercambiar el item del slot actual con el slot 8
        // Así el arma queda en el slot 8 (donde estará el jugador) y puede usarla
        if (currentSlot != 8) {
            ItemStack currentItem = player.getInventory().getItem(currentSlot);
            ItemStack slot8Item = player.getInventory().getItem(8);

            // Guardar lo que había en slot 8 para restaurar después
            swappedSlot8Item.put(uuid, slot8Item != null ? slot8Item.clone() : null);

            // Intercambiar
            player.getInventory().setItem(8, currentItem);
            player.getInventory().setItem(currentSlot, slot8Item);
        } else {
            swappedSlot8Item.remove(uuid);
        }

        // Mover al slot 8 (ahora tiene el arma)
        player.getInventory().setHeldItemSlot(8);

        // Crear y mostrar Boss Bar
        showAbilityBar(player);

        // Mensaje
        player.sendMessage("§a⚡ Modo Habilidades §aACTIVADO §7- Pulsa §f1-8 §7para lanzar, §fF §7para salir");
        player.sendActionBar(Component.text("§a⚔ MODO HABILIDADES §7- Pulsa §f1-8 §7para lanzar"));
    }

    private void disableAbilityMode(Player player) {
        UUID uuid = player.getUniqueId();

        // Desactivar modo
        abilityModeEnabled.put(uuid, false);

        // Cancelar casteo en progreso
        BukkitTask castTask = castingTasks.remove(uuid);
        if (castTask != null) {
            castTask.cancel();
            castingStartLocations.remove(uuid);
            player.sendMessage("§c✗ Casteo cancelado");
        }

        // Ocultar Boss Bar
        hideAbilityBar(player);

        // Restaurar items intercambiados
        Integer prevSlot = previousSlot.get(uuid);
        if (prevSlot != null && prevSlot != 8) {
            // Intercambiar de vuelta: el arma vuelve a su slot original
            ItemStack slot8Item = player.getInventory().getItem(8); // El arma
            ItemStack originalSlot8 = swappedSlot8Item.get(uuid);   // Lo que había en slot 8

            player.getInventory().setItem(prevSlot, slot8Item);
            player.getInventory().setItem(8, originalSlot8);

            swappedSlot8Item.remove(uuid);
        }

        // Restaurar slot anterior
        if (prevSlot != null) {
            player.getInventory().setHeldItemSlot(prevSlot);
        }

        // Mensaje
        player.sendMessage("§c⚡ Modo Habilidades §cDESACTIVADO");
        player.sendActionBar(Component.text(""));
    }

    // ==================== BOSS BAR ====================

    private void showAbilityBar(Player player) {
        UUID uuid = player.getUniqueId();

        // Crear boss bar si no existe
        BossBar bossBar = playerBossBars.get(uuid);
        if (bossBar == null) {
            bossBar = BossBar.bossBar(
                buildAbilityBarText(player),
                1.0f,
                BossBar.Color.PURPLE,
                BossBar.Overlay.PROGRESS
            );
            playerBossBars.put(uuid, bossBar);
        } else {
            bossBar.name(buildAbilityBarText(player));
        }

        player.showBossBar(bossBar);
    }

    private void hideAbilityBar(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bossBar = playerBossBars.get(uuid);
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    private Component buildAbilityBarText(Player player) {
        UUID uuid = player.getUniqueId();
        Map<Integer, String> slots = abilitySlots.getOrDefault(uuid, new HashMap<>());

        StringBuilder sb = new StringBuilder();
        sb.append("§d⚔ HABILIDADES §7| ");

        for (int i = 0; i < 9; i++) {
            String abilityId = slots.get(i);
            if (abilityId != null) {
                Ability ability = plugin.getAbilityManager().getAbility(abilityId);
                if (ability != null) {
                    sb.append("§f[").append(i + 1).append(":§e").append(getShortName(ability.getName())).append("§f] ");
                } else {
                    sb.append("§8[").append(i + 1).append(":-] ");
                }
            } else {
                sb.append("§8[").append(i + 1).append(":-] ");
            }
        }

        return Component.text(sb.toString());
    }

    private String getShortName(String name) {
        // Acortar nombres largos
        if (name.length() > 8) {
            return name.substring(0, 7) + "…";
        }
        return name;
    }

    public void updateAbilityBar(Player player) {
        UUID uuid = player.getUniqueId();
        if (!abilityModeEnabled.getOrDefault(uuid, false)) return;

        BossBar bossBar = playerBossBars.get(uuid);
        if (bossBar != null) {
            bossBar.name(buildAbilityBarText(player));
        }
    }

    // ==================== NÚMERO KEYS PARA LANZAR ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Solo interceptar si está en modo habilidades
        if (!abilityModeEnabled.getOrDefault(uuid, false)) return;

        // Cancelar cambio de slot
        event.setCancelled(true);

        // El nuevo slot es la tecla presionada (0-8 = teclas 1-9)
        int abilitySlot = event.getNewSlot();

        // Intentar lanzar la habilidad de ese slot
        castAbilityFromSlot(player, abilitySlot);
    }

    private void castAbilityFromSlot(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        Map<Integer, String> slots = abilitySlots.get(uuid);

        if (slots == null || !slots.containsKey(slot)) {
            player.sendMessage("§7Slot " + (slot + 1) + " vacío. Usa §f/habilidades §7para asignar.");
            return;
        }

        String abilityId = slots.get(slot);
        Ability ability = plugin.getAbilityManager().getAbility(abilityId);

        if (ability == null) {
            player.sendMessage("§c✗ Habilidad no encontrada.");
            return;
        }

        // Verificar cooldown
        if (ability.getCooldown() > 0) {
            Map<String, Long> playerCooldowns = abilityCooldowns.get(uuid);
            if (playerCooldowns != null && playerCooldowns.containsKey(abilityId)) {
                long lastUse = playerCooldowns.get(abilityId);
                long cooldownMs = (long) (ability.getCooldown() * 1000);
                long remaining = (lastUse + cooldownMs) - System.currentTimeMillis();
                if (remaining > 0) {
                    double remainingSecs = remaining / 1000.0;
                    player.sendMessage("§c✗ " + ability.getName() + " §cen cooldown: §f" + String.format("%.1f", remainingSecs) + "s");
                    return;
                }
            }
        }

        // Verificar cast-mode static
        if ("static".equalsIgnoreCase(ability.getCastMode())) {
            double speed = player.getVelocity().length();
            if (speed > 0.1) {
                player.sendMessage("§c✗ Debes estar quieto para usar " + ability.getName() + ".");
                return;
            }
        }

        PlayerData playerData = plugin.getDatabaseManager().getPlayerData(uuid);
        if (playerData == null) {
            player.sendMessage("§c✗ Error al obtener datos del jugador.");
            return;
        }

        // Verificar nivel
        if (playerData.getLevel() < ability.getRequiredLevel()) {
            player.sendMessage("§c✗ Necesitas nivel " + ability.getRequiredLevel() + " para " + ability.getName() + ".");
            return;
        }

        // Verificar maná (coste base + % del maná máximo)
        int manaCostBase = 0;
        try { manaCostBase = Integer.parseInt(ability.getManaCost()); } catch (NumberFormatException ignored) {}
        int manaCost = manaCostBase + (ability.getManaCostPercent() * playerData.getMaxMana() / 100);

        if (manaCost > 0 && !playerData.useMana(manaCost)) {
            player.sendMessage("§c✗ Maná insuficiente. Necesitas §9" + manaCost + "§c, tienes §9" + playerData.getCurrentMana() + "§c.");
            return;
        }

        // Verificar arma requerida — soporta lista separada por comas y categorías de arma
        if (!ability.getRequiredWeapon().isEmpty()) {
            org.bukkit.Material heldMat = player.getInventory().getItemInMainHand().getType();
            boolean weaponValid = false;
            for (String req : ability.getRequiredWeapon().split(",")) {
                req = req.trim().toUpperCase();
                java.util.Set<org.bukkit.Material> cat = WEAPON_CATEGORIES.get(req);
                if (cat != null) {
                    if (cat.contains(heldMat)) { weaponValid = true; break; }
                } else {
                    try { if (org.bukkit.Material.valueOf(req) == heldMat) { weaponValid = true; break; } }
                    catch (IllegalArgumentException ignored) {}
                }
            }
            if (!weaponValid) {
                playerData.regenMana(manaCost);
                String first = ability.getRequiredWeapon().split(",")[0].trim().toUpperCase();
                String wName = WEAPON_NAMES.getOrDefault(first, first.toLowerCase().replace("_", " "));
                player.sendMessage("§c✗ Necesitas un §f" + wName + "§c (u otra arma válida) en el slot §f1§c.");
                return;
            }
        }

        // Verificar si ya está casteando otra habilidad
        if (castingTasks.containsKey(uuid)) {
            player.sendMessage("§c✗ Ya estás casteando otra habilidad.");
            playerData.regenMana(manaCost); // Devolver maná
            return;
        }

        // Si tiene cast time, programar la ejecución
        double castTime = ability.getCastTime();
        if (castTime > 0) {
            // Guardar posición inicial para verificar movimiento en modo static
            castingStartLocations.put(uuid, player.getLocation().clone());

            // Mostrar mensaje de casteo
            player.sendMessage("§e⏳ Casteando " + ability.getName() + "... §7(" + castTime + "s)");
            player.sendActionBar(Component.text("§e⏳ Casteando " + ability.getName() + "..."));

            // Variables finales para el lambda
            final int finalManaCost = manaCost;
            final String finalAbilityId = abilityId;
            final Ability finalAbility = ability;
            final PlayerData finalPlayerData = playerData;

            // Programar ejecución después del cast time
            long castTicks = (long) (castTime * 20); // Convertir segundos a ticks
            BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                castingTasks.remove(uuid);
                org.bukkit.Location startLoc = castingStartLocations.remove(uuid);

                // Verificar si el jugador sigue online
                if (!player.isOnline()) return;

                // Verificar movimiento si es modo static
                if ("static".equalsIgnoreCase(finalAbility.getCastMode())) {
                    org.bukkit.Location currentLoc = player.getLocation();
                    if (startLoc != null && (currentLoc.getX() != startLoc.getX() ||
                        currentLoc.getY() != startLoc.getY() || currentLoc.getZ() != startLoc.getZ())) {
                        player.sendMessage("§c✗ Casteo cancelado - ¡Te has movido!");
                        finalPlayerData.regenMana(finalManaCost);
                        return;
                    }
                }

                // Ejecutar habilidad
                executeAbility(player, finalAbility, finalPlayerData);

                // Registrar cooldown
                if (finalAbility.getCooldown() > 0) {
                    abilityCooldowns.computeIfAbsent(uuid, k -> new HashMap<>()).put(finalAbilityId, System.currentTimeMillis());
                }

                // Mantenerse en slot 8
                if (player.getInventory().getHeldItemSlot() != 8) {
                    player.getInventory().setHeldItemSlot(8);
                }
            }, castTicks);

            castingTasks.put(uuid, task);
        } else {
            // Sin cast time - ejecutar inmediatamente
            executeAbility(player, ability, playerData);

            // Registrar cooldown
            if (ability.getCooldown() > 0) {
                abilityCooldowns.computeIfAbsent(uuid, k -> new HashMap<>()).put(abilityId, System.currentTimeMillis());
            }

            // Mantenerse en slot 8 para tener el arma en mano
            if (player.getInventory().getHeldItemSlot() != 8) {
                player.getInventory().setHeldItemSlot(8);
            }
        }
    }

    // ==================== EJECUCIÓN DE HABILIDADES ====================

    private void executeAbility(Player player, Ability ability, PlayerData playerData) {
        String id = ability.getId();
        PlayerStats stats = playerData.getStats();
        double damage = ability.calculateDamage(stats);
        if (damage < 1.0) damage = 1.0;

        // Aplicar pasiva Elemento Antiguo (+30% daño a hechizos de mago)
        if ("mago".equalsIgnoreCase(ability.getRpgClass())) {
            Ability elementoAntiguo = plugin.getAbilityManager().getAbility("elemento-antiguo");
            if (elementoAntiguo != null && playerData.getLevel() >= elementoAntiguo.getRequiredLevel()) {
                String playerClass = playerData.getPlayerClass();
                if (playerClass != null && playerClass.equalsIgnoreCase("mago")) {
                    double bonus = elementoAntiguo.getDoubleProperty("damage-bonus", 0.3);
                    damage *= (1.0 + bonus);
                }
            }
        }

        switch (id) {
            case "bola-fuego":
                executeFireball(player, ability, damage, playerData);
                break;

            case "flecha-rapida":
                launchArrowAbility(player, ability, damage, 3.0);
                player.sendMessage("§a🏹 ¡Flecha Rápida! §7(Daño: §e" + String.format("%.1f", damage) + "§7) (-§9" + ability.getManaCost() + " maná§7)");
                break;

            case "flecha-cargada":
                launchArrowAbility(player, ability, damage * 1.5, 2.5);
                player.sendMessage("§a🏹 ¡Flecha Cargada! §7(Daño: §e" + String.format("%.1f", damage * 1.5) + "§7) (-§9" + ability.getManaCost() + " maná§7)");
                break;

            case "flecha-negra":
                Arrow blackArrow = launchArrowAbility(player, ability, damage, 2.8);
                blackArrow.setMetadata("rpg-poison", new FixedMetadataValue(plugin, true));
                player.sendMessage("§5🏹 ¡Flecha Negra! §7(Daño: §e" + String.format("%.1f", damage) + "§7 + veneno) (-§9" + ability.getManaCost() + " maná§7)");
                break;

            case "flecha-penetrante":
                Arrow penetratingArrow = launchArrowAbility(player, ability, damage, 3.5);
                penetratingArrow.setMetadata("rpg-pierce", new FixedMetadataValue(plugin, true));
                player.sendMessage("§6🏹 ¡Flecha Penetrante! §7(Daño: §e" + String.format("%.1f", damage) + "§7 ignora armadura) (-§9" + ability.getManaCost() + " maná§7)");
                break;

            case "flecha-rebotante":
                Arrow bouncingArrow = launchArrowAbility(player, ability, damage, 2.5);
                bouncingArrow.setMetadata("rpg-bounce", new FixedMetadataValue(plugin, 3)); // 3 rebotes
                player.sendMessage("§b🏹 ¡Flecha Rebotante! §7(Daño: §e" + String.format("%.1f", damage) + "§7 x3 objetivos) (-§9" + ability.getManaCost() + " maná§7)");
                break;

            case "lluvia-flechas":
                launchArrowRain(player, ability, damage);
                player.sendMessage("§e🏹 ¡Lluvia de Flechas! §7(Daño: §e" + String.format("%.1f", damage) + "§7 por flecha) (-§9" + ability.getManaCost() + " maná§7)");
                break;

            case "multi-disparo":
                launchMultiShot(player, ability, damage, 5);
                player.sendMessage("§a🏹 ¡MultiDisparo! §7(5 flechas, Daño: §e" + String.format("%.1f", damage) + "§7 c/u) (-§9" + ability.getManaCost() + " maná§7)");
                break;

            case "disparo-al-corazon":
                // Buff de daño crítico con valores configurables
                double critBonus = ability.getCritBonus() > 0 ? ability.getCritBonus() : 0.5; // 50% por defecto
                double critDuration = ability.getCritDuration() > 0 ? ability.getCritDuration() : 30.0; // 30s por defecto
                long critExpiration = System.currentTimeMillis() + (long)(critDuration * 1000);
                player.setMetadata("rpg-crit-buff", new FixedMetadataValue(plugin, critExpiration));
                player.setMetadata("rpg-crit-bonus", new FixedMetadataValue(plugin, critBonus));
                spawnAbilityParticles(player, ability);
                int critPercent = (int)(critBonus * 100);
                player.sendMessage("§c❤ ¡Disparo al Corazón! §7(+" + critPercent + "% crítico durante " + (int)critDuration + "s) (-§9" + ability.getManaCost() + " maná§7)");
                break;

            // ==================== HABILIDADES DE MASCOTAS ====================

            case "orden-de-ataque":
                executePetAttackOrder(player, ability, damage);
                break;

            case "cura-animal":
                executePetHeal(player, ability, damage);
                break;

            case "resucitar-mascota":
                executePetResurrect(player, ability);
                break;

            case "rabia-animal":
                executePetRage(player, ability);
                break;

            case "potencia-de-la-manada":
                executePackPower(player, ability, damage);
                break;

            case "golpe-sombras-animales":
                executeShadowStrike(player, ability, damage);
                break;

            case "manada-necrotica":
                executeNecroticPack(player, ability, damage);
                break;

            // ==================== TRANSFORMACIONES DEL DRUIDA ====================

            case "forma-de-lobo":
            case "forma-de-oso":
            case "forma-de-arana":
            case "forma-de-zorro":
            case "forma-de-panda":
            case "forma-de-warden":
                executeTransformation(player, ability);
                break;

            case "revertir-forma":
                // Revertir transformación del druida
                TransformationManager transformManager = plugin.getTransformationManager();
                if (transformManager.isTransformed(player.getUniqueId())) {
                    transformManager.revertTransformation(player);
                } else {
                    player.sendMessage("§c✗ No estás transformado");
                }
                break;

            // ==================== HABILIDADES DE FORMA (DRUIDA) ====================

            case "zarpazo-lobo":
            case "zarpazo-oso":
            case "zarpazo-zorro":
            case "zarpazo-panda":
                executeFormMeleeAttack(player, ability, damage, "§a🐾 ¡" + ability.getName() + "!");
                break;

            case "mordisco-infectado":
            case "mordisco-oso":
            case "mordisco-arana":
            case "mordisco-zorro":
            case "mordisco-panda":
                executeFormMeleeAttack(player, ability, damage, "§c🦷 ¡" + ability.getName() + "!");
                break;

            case "aullido-de-manada":
                executePackHowl(player, ability);
                break;

            case "golpe-pesado":
            case "golpe-pesado-panda":
                executeHeavyStrike(player, ability, damage);
                break;

            case "rabia-de-oso":
                executeBearRage(player, ability);
                break;

            case "telarana":
                executeWebShot(player, ability);
                break;

            case "veneno-arana":
                executePoisonCloud(player, ability, damage);
                break;

            case "esquivar-zorro":
                executeFoxDodge(player, ability);
                break;

            case "grito-sonico":
                executeSonicScream(player, ability, damage);
                break;

            case "onda-de-choque":
                executeShockwave(player, ability, damage);
                break;

            case "sentido-vibracion":
                executeVibrationSense(player, ability);
                break;

            // ==================== HABILIDADES DE CURACIÓN ====================

            case "cura-natural":
                executeNaturalHeal(player, ability, playerData);
                break;

            // ==================== HABILIDADES DE DAÑO EN ÁREA ====================

            case "fuerza-de-la-naturaleza":
                executeNatureForce(player, ability, damage, playerData);
                break;

            // ==================== HABILIDADES DEL EVOCADOR ====================

            case "llama-de-los-dragones":
                executeDragonFlame(player, ability, damage, playerData);
                break;

            case "llama-interior":
                executeInnerFlame(player, ability, playerData);
                break;

            case "llama-viva":
                executeLivingFlame(player, ability, damage, playerData);
                break;

            case "vuelo-del-dragon":
                executeDragonFlight(player, ability, damage);
                break;

            case "llama-bailarina":
                executeDancingFlame(player, ability, damage, playerData);
                break;

            case "rugido-del-dragon":
                executeDragonRoar(player, ability);
                break;

            case "rayo-dragones-ancestrales":
                executeAncestralRay(player, ability, damage, playerData);
                break;

            case "llamada-ultimo-dragon":
                executeLastDragonCall(player, ability, damage);
                break;

            // ==================== HABILIDADES DEL GUERRERO ====================
            case "corte-profundo":
                executeDeepCut(player, ability, damage);
                break;

            case "embestida":
                executeCharge(player, ability, damage);
                break;

            case "romper-corazas":
                executeArmorBreak(player, ability, damage);
                break;

            case "atronar":
                executeThunderStrike(player, ability, damage);
                break;

            case "sed-de-sangre":
                executeBloodThirst(player, ability);
                break;

            case "torbellino-sangriento":
                executeBloodyWhirlwind(player, ability, damage);
                break;

            case "ejecutar":
                executeExecute(player, ability, playerData);
                break;

            case "ira-furibunda":
                executeFuriousRage(player, ability);
                break;

            // ==================== HABILIDADES DEL INVOCADOR ====================
            case "erupcion-de-fuego":
                executeFireEruption(player, ability, damage);
                break;

            case "elemental-de-fuego":
                executeSummonElemental(player, ability, playerData, "BLAZE", "Fuego");
                break;

            case "trueno-primigenio":
                executePrimordialThunder(player, ability, damage);
                break;

            case "elemental-de-aire":
                executeSummonElemental(player, ability, playerData, "STRAY", "Aire");
                break;

            case "maremoto":
                executeTidalWave(player, ability, damage);
                break;

            case "elemental-de-agua":
                executeSummonElemental(player, ability, playerData, "DROWNED", "Agua");
                break;

            case "vulcano":
                executeVolcano(player, ability, damage);
                break;

            case "elemental-de-tierra":
                executeSummonElemental(player, ability, playerData, "IRON_GOLEM", "Tierra");
                break;

            // ==================== HABILIDADES DEL MAGO ====================
            case "pica-de-hielo":
                executeIceSpike(player, ability, damage);
                break;

            case "implosion-arcana":
                executeArcaneImplosion(player, ability, damage);
                break;

            case "llamarada":
                executeFlare(player, ability, damage);
                break;

            case "ventisca":
                executeBlizzard(player, ability, damage);
                break;

            case "sifon-de-mana":
                executeManaSiphon(player, ability, playerData);
                break;

            case "salto-dimensional":
                executeDimensionalLeap(player, ability);
                break;

            // ── MONJE ──
            case "golpe-de-chi":
                executeChiStrike(player, ability, damage);
                break;

            case "flujo-de-chi":
                executeChiFlow(player, ability, damage);
                break;

            case "meditacion":
                executeMeditation(player, ability, playerData);
                break;

            case "carrera-zen":
                executeZenRun(player, ability);
                break;

            case "golpe-ocho-trigramas":
                executeEightTrigrams(player, ability, damage);
                break;

            case "ocho-puertas-meditacion":
                executeEightGates(player, ability);
                break;

            case "palma-de-buda":
                executeBuddhaPalm(player, ability, playerData);
                break;

            case "descarga-de-karma":
                executeKarmaDischarge(player, ability);
                break;

            default:
                // Habilidad genérica - si requiere arco, lanzar flecha
                if ("BOW".equalsIgnoreCase(ability.getRequiredWeapon())) {
                    launchArrowAbility(player, ability, damage, 2.5);
                    player.sendMessage("§e🏹 " + ability.getName() + "! §7(Daño: §e" + String.format("%.1f", damage) + "§7) (-§9" + ability.getManaCost() + " maná§7)");
                } else {
                    spawnAbilityParticles(player, ability);
                    player.sendMessage("§e⚡ " + ability.getName() + " activada! §7(-§9" + ability.getManaCost() + " maná§7)");
                }
                break;
        }
    }

    // ==================== MÉTODOS DE LANZAMIENTO DE PROYECTILES ====================

    private Arrow launchArrowAbility(Player player, Ability ability, double damage, double speed) {
        // Aplicar crítico si el jugador tiene el buff activo (se calcula al lanzar)
        double finalDamage = damage;
        boolean isCrit = false;
        if (player.hasMetadata("rpg-crit-buff")) {
            long expiration = player.getMetadata("rpg-crit-buff").get(0).asLong();
            if (System.currentTimeMillis() < expiration) {
                // Buff activo - aplicar crítico
                double critBonus = 0.5; // Por defecto 50%
                if (player.hasMetadata("rpg-crit-bonus")) {
                    critBonus = player.getMetadata("rpg-crit-bonus").get(0).asDouble();
                }
                finalDamage = damage * (1.0 + critBonus);
                isCrit = true;
            } else {
                // Buff expirado - limpiar metadatos
                player.removeMetadata("rpg-crit-buff", plugin);
                player.removeMetadata("rpg-crit-bonus", plugin);
            }
        }

        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setVelocity(player.getLocation().getDirection().multiply(speed));
        arrow.setMetadata("rpg-damage", new FixedMetadataValue(plugin, finalDamage));
        arrow.setMetadata("rpg-ability", new FixedMetadataValue(plugin, ability.getId()));
        if (isCrit) {
            arrow.setMetadata("rpg-crit", new FixedMetadataValue(plugin, true));
        }
        spawnAbilityParticles(player, ability);
        return arrow;
    }

    private void launchMultiShot(Player player, Ability ability, double damage, int arrowCount) {
        org.bukkit.util.Vector direction = player.getLocation().getDirection();

        // Verificar crit buff al momento del lanzamiento
        double finalDamage = damage;
        boolean isCrit = false;
        if (player.hasMetadata("rpg-crit-buff")) {
            long expiration = player.getMetadata("rpg-crit-buff").get(0).asLong();
            if (System.currentTimeMillis() < expiration) {
                double critBonus = 0.5;
                if (player.hasMetadata("rpg-crit-bonus")) {
                    critBonus = player.getMetadata("rpg-crit-bonus").get(0).asDouble();
                }
                finalDamage = damage * (1.0 + critBonus);
                isCrit = true;
            }
        }

        final double fDamage = finalDamage;
        final boolean fIsCrit = isCrit;
        for (int i = 0; i < arrowCount; i++) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Arrow arrow = player.launchProjectile(Arrow.class);
                double spread = 0.05;
                org.bukkit.util.Vector vel = direction.clone().add(
                    new org.bukkit.util.Vector(
                        (Math.random() - 0.5) * spread,
                        (Math.random() - 0.5) * spread,
                        (Math.random() - 0.5) * spread
                    )
                ).multiply(3.0);
                arrow.setVelocity(vel);
                arrow.setMetadata("rpg-damage", new FixedMetadataValue(plugin, fDamage));
                arrow.setMetadata("rpg-ability", new FixedMetadataValue(plugin, ability.getId()));
                if (fIsCrit) {
                    arrow.setMetadata("rpg-crit", new FixedMetadataValue(plugin, true));
                }
            }, i * 3L);
        }
        spawnAbilityParticles(player, ability);
    }

    private void launchArrowRain(Player player, Ability ability, double damage) {
        org.bukkit.Location target = player.getLocation().add(player.getLocation().getDirection().multiply(10));
        target.setY(target.getY() + 15); // Lanzar desde arriba

        // Verificar crit buff al momento del lanzamiento
        double finalDamage = damage;
        boolean isCrit = false;
        if (player.hasMetadata("rpg-crit-buff")) {
            long expiration = player.getMetadata("rpg-crit-buff").get(0).asLong();
            if (System.currentTimeMillis() < expiration) {
                double critBonus = 0.5;
                if (player.hasMetadata("rpg-crit-bonus")) {
                    critBonus = player.getMetadata("rpg-crit-bonus").get(0).asDouble();
                }
                finalDamage = damage * (1.0 + critBonus);
                isCrit = true;
            }
        }

        final double fDamage = finalDamage;
        final boolean fIsCrit = isCrit;
        for (int i = 0; i < 8; i++) {
            final double offsetX = (Math.random() - 0.5) * 6;
            final double offsetZ = (Math.random() - 0.5) * 6;

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                org.bukkit.Location spawnLoc = target.clone().add(offsetX, 0, offsetZ);
                Arrow arrow = player.getWorld().spawnArrow(
                    spawnLoc,
                    new org.bukkit.util.Vector(0, -1, 0),
                    2.0f,
                    0.5f
                );
                arrow.setShooter(player);
                arrow.setMetadata("rpg-damage", new FixedMetadataValue(plugin, fDamage));
                arrow.setMetadata("rpg-ability", new FixedMetadataValue(plugin, ability.getId()));
                if (fIsCrit) {
                    arrow.setMetadata("rpg-crit", new FixedMetadataValue(plugin, true));
                }
            }, i * 2L);
        }
        spawnAbilityParticles(player, ability);
    }

    private void spawnAbilityParticles(Player player, Ability ability) {
        if (ability == null) return;
        String particleName = ability.getParticle();
        if (particleName == null || particleName.isEmpty()) return;

        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            player.getWorld().spawnParticle(
                particle,
                player.getLocation().add(0, 1, 0),
                ability.getParticleCount(),
                0.5, 0.5, 0.5,
                ability.getParticleSpeed()
            );
        } catch (IllegalArgumentException e) {
            // Partícula no válida, ignorar
        }
    }

    // ==================== HABILIDADES DE MASCOTAS ====================

    /**
     * Orden de Ataque: Ordena a las mascotas atacar al objetivo que el jugador mira
     */
    private void executePetAttackOrder(Player player, Ability ability, double damage) {
        PetManager petManager = plugin.getPetManager();
        java.util.List<Pet> activePets = petManager.getActivePets(player);

        if (activePets.isEmpty()) {
            player.sendMessage("§c✗ No tienes mascotas activas");
            return;
        }

        // Buscar entidad objetivo (raycast)
        org.bukkit.entity.Entity target = getTargetEntity(player, (int) ability.getRange());

        if (target == null || !(target instanceof LivingEntity)) {
            player.sendMessage("§c✗ No hay objetivo válido");
            return;
        }

        LivingEntity targetEntity = (LivingEntity) target;

        // Ordenar a todas las mascotas atacar
        for (Pet pet : activePets) {
            pet.setCurrentCommand(PetCommand.ATTACK);
            pet.setTarget(targetEntity);
        }

        spawnAbilityParticles(player, ability);
        player.sendMessage("§c⚔ ¡Orden de Ataque! §7Tus mascotas atacan a " + targetEntity.getName() + " (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Cura Animal: Cura a las mascotas activas
     */
    private void executePetHeal(Player player, Ability ability, double healAmount) {
        PetManager petManager = plugin.getPetManager();
        java.util.List<Pet> activePets = petManager.getActivePets(player);

        if (activePets.isEmpty()) {
            player.sendMessage("§c✗ No tienes mascotas activas para curar");
            return;
        }

        // Obtener porcentaje de curación del YML (default 30%)
        double healPercent = ability.getDoubleProperty("heal-percent", 30.0);

        int totalHealed = 0;
        for (Pet pet : activePets) {
            if (pet.isDead()) continue;

            // Calcular curación basada en porcentaje de vida máxima
            int healValue = (int) (pet.getMaxHealth() * healPercent / 100.0);
            healValue = Math.max(1, healValue);

            int oldHealth = pet.getCurrentHealth();
            pet.heal(healValue);
            int healed = pet.getCurrentHealth() - oldHealth;
            totalHealed += healed;

            // Actualizar la vida de la entidad también
            if (pet.getEntity() instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) pet.getEntity();
                if (!living.isDead()) {
                    living.setHealth(Math.min(pet.getCurrentHealth(), living.getMaxHealth()));
                    // Actualizar barra de vida
                    petManager.updatePetNameWithHealthBar(living, pet);
                }
            }

            // Guardar cambios
            plugin.getDatabaseManager().savePet(pet);

            // Partículas en la mascota
            if (pet.getEntity() != null && !pet.getEntity().isDead()) {
                pet.getEntity().getWorld().spawnParticle(
                    Particle.HEART,
                    pet.getEntity().getLocation().add(0, 1, 0),
                    5, 0.3, 0.3, 0.3, 0.1
                );
            }
        }

        spawnAbilityParticles(player, ability);
        player.sendMessage("§a❤ ¡Cura Animal! §7Curaste §a" + totalHealed + " §7puntos de vida (" + (int)healPercent + "%) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Resucitar Mascota: Revive a una mascota muerta y la re-invoca
     */
    private void executePetResurrect(Player player, Ability ability) {
        PetManager petManager = plugin.getPetManager();
        int characterSlot = plugin.getDatabaseManager().getCharacterManager().getActiveSlot(player.getUniqueId());
        java.util.List<Pet> storedPets = petManager.getStoredPets(player.getUniqueId(), characterSlot);

        // Buscar primera mascota muerta
        Pet deadPet = null;
        for (Pet pet : storedPets) {
            if (pet.isDead()) {
                deadPet = pet;
                break;
            }
        }

        if (deadPet == null) {
            player.sendMessage("§c✗ No tienes mascotas muertas para resucitar");
            return;
        }

        // Obtener porcentaje de resurrección del YML (default 50%)
        int resurrectPercent = ability.getIntProperty("resurrect-percent", 50);
        deadPet.resurrect(resurrectPercent);
        plugin.getDatabaseManager().savePet(deadPet);

        // Obtener nombre para el mensaje
        PetType type = petManager.getPetType(deadPet.getTypeId());
        String petName = deadPet.getDisplayName();
        if (type != null && (petName == null || petName.equals(deadPet.getTypeId()))) {
            petName = type.getName();
        }

        spawnAbilityParticles(player, ability);
        player.sendMessage("§a✝ ¡Resucitar Mascota! §7" + petName + " ha vuelto a la vida con " + resurrectPercent + "% HP (-§9" + ability.getManaCost() + " maná§7)");

        // Re-invocar la mascota automáticamente
        boolean summoned = petManager.summonPet(player, deadPet);
        if (!summoned) {
            player.sendMessage("§7Tu mascota está lista para ser invocada desde el menú.");
        }
    }

    /**
     * Rabia Animal: Aumenta el daño de las mascotas temporalmente
     */
    private void executePetRage(Player player, Ability ability) {
        PetManager petManager = plugin.getPetManager();
        java.util.List<Pet> activePets = petManager.getActivePets(player);

        if (activePets.isEmpty()) {
            player.sendMessage("§c✗ No tienes mascotas activas");
            return;
        }

        // Obtener valores del YML
        double damageBonus = ability.getDoubleProperty("damage-bonus", 50.0);
        double buffDuration = ability.getDoubleProperty("buff-duration", 30.0);

        // Aplicar buff de daño a las mascotas
        long expiration = System.currentTimeMillis() + (long)(buffDuration * 1000);
        for (Pet pet : activePets) {
            if (pet.getEntity() != null) {
                pet.getEntity().setMetadata("rpg-rage-buff", new FixedMetadataValue(plugin, expiration));
                pet.getEntity().setMetadata("rpg-rage-bonus", new FixedMetadataValue(plugin, damageBonus / 100.0));

                // Partículas de rabia
                pet.getEntity().getWorld().spawnParticle(
                    Particle.ANGRY_VILLAGER,
                    pet.getEntity().getLocation().add(0, 1, 0),
                    10, 0.5, 0.5, 0.5, 0.1
                );
            }
        }

        spawnAbilityParticles(player, ability);
        player.sendMessage("§c🔥 ¡Rabia Animal! §7Tus mascotas tienen +" + (int)damageBonus + "% daño durante " + (int)buffDuration + "s (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Potencia de la Manada: Buff grupal basado en número de mascotas
     */
    private void executePackPower(Player player, Ability ability, double damage) {
        PetManager petManager = plugin.getPetManager();
        java.util.List<Pet> activePets = petManager.getActivePets(player);

        if (activePets.isEmpty()) {
            player.sendMessage("§c✗ No tienes mascotas activas");
            return;
        }

        // Obtener valores del YML
        double damageBonusPerPet = ability.getDoubleProperty("damage-bonus-per-pet", 10.0);
        double buffDuration = ability.getDoubleProperty("buff-duration", 20.0);

        // Buff escalado por número de mascotas
        int petCount = activePets.size();
        double buffPercent = petCount * damageBonusPerPet;

        // Aplicar buff al jugador y mascotas
        long expiration = System.currentTimeMillis() + (long)(buffDuration * 1000);
        player.setMetadata("rpg-pack-buff", new FixedMetadataValue(plugin, expiration));
        player.setMetadata("rpg-pack-bonus", new FixedMetadataValue(plugin, buffPercent / 100.0));

        for (Pet pet : activePets) {
            if (pet.getEntity() != null) {
                pet.getEntity().setMetadata("rpg-pack-buff", new FixedMetadataValue(plugin, expiration));

                // Partículas
                pet.getEntity().getWorld().spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    pet.getEntity().getLocation().add(0, 1, 0),
                    8, 0.3, 0.3, 0.3, 0.1
                );
            }
        }

        spawnAbilityParticles(player, ability);
        player.sendMessage("§a🐺 ¡Potencia de la Manada! §7+" + (int)buffPercent + "% daño (" + petCount + " mascotas x " + (int)damageBonusPerPet + "%) durante " + (int)buffDuration + "s (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Golpe de las Sombras Animales: Teletransporta la mascota detrás del objetivo y ataca con daño aumentado
     */
    private void executeShadowStrike(Player player, Ability ability, double damage) {
        PetManager petManager = plugin.getPetManager();
        java.util.List<Pet> activePets = petManager.getActivePets(player);

        if (activePets.isEmpty()) {
            player.sendMessage("§c✗ No tienes mascotas activas");
            return;
        }

        // Buscar objetivo que el jugador esté mirando
        org.bukkit.entity.Entity targetEntity = getTargetEntity(player, (int) ability.getRange());
        if (!(targetEntity instanceof LivingEntity)) {
            player.sendMessage("§c✗ No hay objetivo válido");
            return;
        }
        LivingEntity target = (LivingEntity) targetEntity;

        // No atacar al dueño ni otras mascotas propias
        if (target.equals(player) || petManager.isRpgPet(target)) {
            Player targetOwner = petManager.getOwnerByPetEntity(target);
            if (targetOwner != null && targetOwner.equals(player)) {
                player.sendMessage("§c✗ No puedes atacar a tus propias mascotas");
                return;
            }
        }

        // Usar la primera mascota activa
        Pet pet = activePets.get(0);
        if (pet.getEntity() == null || pet.getEntity().isDead()) {
            player.sendMessage("§c✗ Tu mascota no está disponible");
            return;
        }

        LivingEntity petEntity = pet.getLivingEntity();
        if (petEntity == null) return;

        // Calcular posición detrás del objetivo
        org.bukkit.Location targetLoc = target.getLocation();
        org.bukkit.util.Vector behindVector = targetLoc.getDirection().multiply(-1.5);
        org.bukkit.Location behindTarget = targetLoc.clone().add(behindVector);
        behindTarget.setY(targetLoc.getY());

        // Partículas de humo en posición original
        petEntity.getWorld().spawnParticle(
            Particle.SMOKE,
            petEntity.getLocation().add(0, 1, 0),
            20, 0.5, 0.5, 0.5, 0.1
        );

        // Teletransportar la mascota detrás del objetivo
        petEntity.teleport(behindTarget);

        // Hacer que mire al objetivo
        petEntity.setRotation(targetLoc.getYaw() + 180, 0);

        // Partículas de humo en nueva posición
        petEntity.getWorld().spawnParticle(
            Particle.SMOKE,
            petEntity.getLocation().add(0, 1, 0),
            20, 0.5, 0.5, 0.5, 0.1
        );

        // Calcular daño aumentado (20% extra)
        double petDamage = pet.getDamage() * 1.2;

        // Aplicar daño al objetivo
        target.damage(petDamage, petEntity);

        // Establecer objetivo de la mascota
        if (petEntity instanceof org.bukkit.entity.Mob) {
            ((org.bukkit.entity.Mob) petEntity).setTarget(target);
        }
        pet.setTarget(target);

        spawnAbilityParticles(player, ability);
        player.sendMessage("§8🗡 ¡Golpe de las Sombras! §7Tu mascota apareció detrás del objetivo y asestó §c" + String.format("%.1f", petDamage) + " §7de daño (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Manada Necrótica: Las mascotas hacen daño de veneno durante un tiempo
     */
    private void executeNecroticPack(Player player, Ability ability, double damage) {
        PetManager petManager = plugin.getPetManager();
        java.util.List<Pet> activePets = petManager.getActivePets(player);

        if (activePets.isEmpty()) {
            player.sendMessage("§c✗ No tienes mascotas activas");
            return;
        }

        // Buscar objetivo que el jugador esté mirando
        org.bukkit.entity.Entity targetEntity2 = getTargetEntity(player, (int) ability.getRange());
        if (!(targetEntity2 instanceof LivingEntity)) {
            player.sendMessage("§c✗ No hay objetivo válido");
            return;
        }
        LivingEntity target = (LivingEntity) targetEntity2;

        // No atacar al dueño ni otras mascotas propias
        if (target.equals(player) || petManager.isRpgPet(target)) {
            Player targetOwner = petManager.getOwnerByPetEntity(target);
            if (targetOwner != null && targetOwner.equals(player)) {
                player.sendMessage("§c✗ No puedes atacar a tus propias mascotas");
                return;
            }
        }

        // Aplicar efecto de veneno al objetivo (8 segundos, nivel 1)
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.POISON,
            160, // 8 segundos (20 ticks por segundo)
            0,   // Nivel 1 (1 corazón cada 2.5s)
            false,
            true
        ));

        // Aplicar efecto de wither (5 segundos, nivel 1)
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.WITHER,
            100, // 5 segundos
            0,   // Nivel 1
            false,
            true
        ));

        // Daño inicial basado en las stats (reducido)
        double necroticDamage = damage * 0.3; // 30% del daño calculado
        target.damage(necroticDamage);

        // Marcar a todas las mascotas para que ataquen el objetivo
        for (Pet pet : activePets) {
            if (pet.getEntity() != null && !pet.getEntity().isDead()) {
                pet.setTarget(target);
                if (pet.getEntity() instanceof org.bukkit.entity.Mob) {
                    ((org.bukkit.entity.Mob) pet.getEntity()).setTarget(target);
                }

                // Partículas necróticas en cada mascota
                pet.getEntity().getWorld().spawnParticle(
                    Particle.WITCH,
                    pet.getEntity().getLocation().add(0, 1, 0),
                    15, 0.5, 0.5, 0.5, 0.1
                );
            }
        }

        // Partículas en el objetivo
        target.getWorld().spawnParticle(
            Particle.WITCH,
            target.getLocation().add(0, 1, 0),
            25, 0.5, 1, 0.5, 0.1
        );

        spawnAbilityParticles(player, ability);
        player.sendMessage("§5☠ ¡Manada Necrótica! §7Objetivo envenenado durante 8s y recibió §c" + String.format("%.1f", necroticDamage) + " §7de daño necrótico (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Busca la entidad que el jugador está mirando
     */
    private org.bukkit.entity.Entity getTargetEntity(Player player, int range) {
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity)) continue;
            if (entity == player) continue;

            // Verificar si el jugador está mirando hacia la entidad
            org.bukkit.util.Vector toEntity = entity.getLocation().toVector()
                .subtract(player.getEyeLocation().toVector());
            org.bukkit.util.Vector playerDirection = player.getLocation().getDirection();

            double angle = toEntity.angle(playerDirection);
            if (angle < Math.PI / 6) { // 30 grados de tolerancia
                return entity;
            }
        }
        return null;
    }

    // ==================== HABILIDADES DE CURACIÓN ====================

    /**
     * Ejecuta la habilidad Cura Natural del Druida
     */
    private void executeNaturalHeal(Player player, Ability ability, PlayerData playerData) {
        PlayerStats stats = playerData.getStats();

        // Obtener configuración de curación del ability (con valores por defecto)
        double baseHeal = ability.getDoubleProperty("base-heal", 10.0);
        double intScaling = ability.getDoubleProperty("intelligence-scaling", 0.003);
        double natureScaling = ability.getDoubleProperty("nature-scaling", 0.002);

        // Calcular curación total
        double healAmount = baseHeal;
        healAmount += stats.getIntelligence() * intScaling;
        healAmount += stats.getNaturePower() * natureScaling;

        // Aplicar curación
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        double currentHealth = player.getHealth();
        double newHealth = Math.min(currentHealth + healAmount, maxHealth);
        player.setHealth(newHealth);

        // Efectos visuales
        spawnAbilityParticles(player, ability);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);

        player.sendMessage("§a❤ Cura Natural! §7(+" + String.format("%.1f", healAmount) + " vida) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Ejecuta la habilidad Fuerza de la Naturaleza del Druida
     * Golpe devastador en área que impide transformarse durante 1 minuto
     */
    private void executeNatureForce(Player player, Ability ability, double damage, PlayerData playerData) {
        double range = ability.getRange() > 0 ? ability.getRange() : 5.0;
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 3.0;

        // Buscar enemigos en el área
        int enemiesHit = 0;
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && !(entity instanceof Player)) {
                org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) entity;
                double distance = player.getLocation().distance(target.getLocation());

                if (distance <= aoe) {
                    // Aplicar daño
                    applyAbilityDamage(target, player, damage, ability);
                    enemiesHit++;

                    // Efectos visuales en el objetivo
                    target.getWorld().spawnParticle(
                        org.bukkit.Particle.HAPPY_VILLAGER,
                        target.getLocation().add(0, 1, 0),
                        15, 0.5, 0.5, 0.5, 0.1
                    );
                }
            }
        }

        // Efectos visuales en el jugador
        spawnAbilityParticles(player, ability);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);

        // Impedir transformación durante 1 minuto
        long blockUntil = System.currentTimeMillis() + 60000; // 60 segundos
        player.setMetadata("rpg-transform-blocked", new FixedMetadataValue(plugin, blockUntil));

        if (enemiesHit > 0) {
            player.sendMessage("§a🌿 ¡Fuerza de la Naturaleza! §7(" + enemiesHit + " enemigos, Daño: §e" + String.format("%.1f", damage) + "§7) (-§9" + ability.getManaCost() + " maná§7)");
        } else {
            player.sendMessage("§a🌿 ¡Fuerza de la Naturaleza! §7(Sin enemigos cercanos) (-§9" + ability.getManaCost() + " maná§7)");
        }
        player.sendMessage("§c⚠ No puedes transformarte durante 1 minuto.");
    }

    // ==================== HABILIDADES DEL EVOCADOR ====================

    /**
     * Llama de los Dragones - Daña enemigos y cura aliados en área
     */
    private void executeDragonFlame(Player player, Ability ability, double damage, PlayerData playerData) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 5.0;
        double healAmount = playerData.getStats().getIntelligence() * 0.5 + playerData.getStats().getSacredPower() * 0.3;

        int enemiesHit = 0;
        int alliesHealed = 0;

        // Curar al propio lanzador primero
        double maxHealthSelf = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        player.setHealth(Math.min(player.getHealth() + healAmount, maxHealthSelf));
        player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 2, 0), 5, 0.3, 0.3, 0.3, 0);
        alliesHealed++;

        // Buscar entidades en el área
        for (org.bukkit.entity.Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), aoe, aoe, aoe)) {
            if (entity.equals(player)) continue; // Ya curamos al lanzador

            if (entity instanceof org.bukkit.entity.LivingEntity) {
                org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) entity;

                if (entity instanceof Player) {
                    // Curar aliados (otros jugadores)
                    Player ally = (Player) entity;
                    double maxHealth = ally.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                    ally.setHealth(Math.min(ally.getHealth() + healAmount, maxHealth));
                    ally.getWorld().spawnParticle(org.bukkit.Particle.HEART, ally.getLocation().add(0, 2, 0), 5, 0.3, 0.3, 0.3, 0);
                    alliesHealed++;
                } else {
                    // Dañar enemigos (mobs)
                    applyAbilityDamage(target, player, damage, ability);
                    target.getWorld().spawnParticle(org.bukkit.Particle.FLAME, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                    enemiesHit++;
                }
            }
        }

        spawnAbilityParticles(player, ability);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
        player.sendMessage("§6🔥 ¡Llama de los Dragones! §7(" + enemiesHit + " enemigos, " + alliesHealed + " aliados curados) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Llama Interior - Buff de armadura y vida temporal
     */
    private void executeInnerFlame(Player player, Ability ability, PlayerData playerData) {
        int duration = ability.getIntProperty("buff-duration", 60);
        PlayerStats stats = playerData.getStats();

        // Calcular bonus de armadura basado en agilidad
        double agilityScaling = ability.getDoubleProperty("agility-scaling", 0.5);
        double armorBonus = ability.getIntProperty("armor-bonus", 10) + (stats.getAgility() * agilityScaling);

        // Calcular bonus de vida basado en inteligencia y vida
        double intelligenceScaling = ability.getDoubleProperty("intelligence-scaling", 1.0);
        double healthStatScaling = ability.getDoubleProperty("health-scaling", 0.5);
        double healthBonus = ability.getIntProperty("health-bonus", 20) +
                            (stats.getIntelligence() * intelligenceScaling) +
                            (stats.getHealth() * healthStatScaling);

        // Usar efectos de poción para el buff (más confiable y sincroniza con el cliente)
        // HEALTH_BOOST: cada nivel añade +4 vida máxima (2 corazones)
        int healthBoostLevel = (int) Math.ceil(healthBonus / 4.0);
        // Limitar a nivel máximo razonable
        healthBoostLevel = Math.min(healthBoostLevel, 100);

        // Aplicar efecto de vida extra
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.HEALTH_BOOST,
            duration * 20, // Duración en ticks
            healthBoostLevel - 1, // Nivel (0-indexed)
            false, // Ambient
            true,  // Particles
            true   // Icon
        ));

        // Para la armadura, usamos el atributo directamente con transient modifier
        org.bukkit.attribute.AttributeInstance armorAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR);
        if (armorAttr != null) {
            // Guardar el valor original para restaurar después
            double originalArmor = armorAttr.getBaseValue();
            armorAttr.setBaseValue(originalArmor + armorBonus);

            // Guardar datos para revertir después
            player.setMetadata("rpg-inner-flame", new FixedMetadataValue(plugin, true));
            player.setMetadata("rpg-inner-flame-original-armor", new FixedMetadataValue(plugin, originalArmor));

            // Programar eliminación del buff de armadura
            final double finalOriginalArmor = originalArmor;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && player.hasMetadata("rpg-inner-flame")) {
                    org.bukkit.attribute.AttributeInstance armor = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR);
                    if (armor != null) {
                        armor.setBaseValue(finalOriginalArmor);
                    }
                    player.removeMetadata("rpg-inner-flame", plugin);
                    player.removeMetadata("rpg-inner-flame-original-armor", plugin);
                    player.sendMessage("§c🔥 El efecto de Llama Interior ha terminado.");
                }
            }, duration * 20L);
        }

        // Curar al jugador inmediatamente por la cantidad del bonus
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        player.setHealth(Math.min(player.getHealth() + healthBonus, maxHealth));

        spawnAbilityParticles(player, ability);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.2f);
        player.sendMessage("§6🔥 ¡Llama Interior! §7(+" + String.format("%.1f", armorBonus) + " armadura, +" + String.format("%.1f", healthBonus) + " vida por " + duration + "s) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Llama Viva - Proyectil que cura aliados o daña enemigos
     */
    private void executeLivingFlame(Player player, Ability ability, double damage, PlayerData playerData) {
        double range = ability.getRange() > 0 ? ability.getRange() : 15.0;
        double healAmount = playerData.getStats().getIntelligence() * 0.8 + playerData.getStats().getSacredPower() * 0.4;

        // Buscar objetivo en la línea de visión
        org.bukkit.entity.LivingEntity target = getTargetInSight(player, range);

        if (target != null) {
            // Crear efecto visual de llama viajando
            org.bukkit.Location from = player.getEyeLocation();
            org.bukkit.Location to = target.getLocation().add(0, 1, 0);
            org.bukkit.util.Vector direction = to.toVector().subtract(from.toVector()).normalize();

            for (double d = 0; d < from.distance(to); d += 0.5) {
                org.bukkit.Location point = from.clone().add(direction.clone().multiply(d));
                player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, point, 3, 0.1, 0.1, 0.1, 0);
            }

            if (target instanceof Player) {
                // Curar aliado
                Player ally = (Player) target;
                double maxHealth = ally.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                ally.setHealth(Math.min(ally.getHealth() + healAmount, maxHealth));
                ally.getWorld().spawnParticle(org.bukkit.Particle.HEART, ally.getLocation().add(0, 2, 0), 8, 0.3, 0.3, 0.3, 0);
                player.sendMessage("§a🔥 ¡Llama Viva! §7Curaste a §f" + ally.getName() + " §7(+" + String.format("%.1f", healAmount) + " vida) (-§9" + ability.getManaCost() + " maná§7)");
            } else {
                // Dañar enemigo
                applyAbilityDamage(target, player, damage, ability);
                target.getWorld().spawnParticle(org.bukkit.Particle.FLAME, target.getLocation().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0.1);
                player.sendMessage("§c🔥 ¡Llama Viva! §7(Daño: §e" + String.format("%.1f", damage) + "§7) (-§9" + ability.getManaCost() + " maná§7)");
            }
        } else {
            // Si no hay objetivo, curarse a sí mismo
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            player.setHealth(Math.min(player.getHealth() + healAmount, maxHealth));
            player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 2, 0), 8, 0.3, 0.3, 0.3, 0);
            player.sendMessage("§a🔥 ¡Llama Viva! §7Te curaste a ti mismo §7(+" + String.format("%.1f", healAmount) + " vida) (-§9" + ability.getManaCost() + " maná§7)");
        }

        spawnAbilityParticles(player, ability);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.0f);
    }

    /**
     * Vuelo del Dragón - Blink que daña enemigos atravesados
     */
    private void executeDragonFlight(Player player, Ability ability, double damage) {
        double range = ability.getRange() > 0 ? ability.getRange() : 12.0;
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 2.0;

        org.bukkit.Location start = player.getLocation().clone();
        org.bukkit.util.Vector direction = player.getLocation().getDirection().normalize();
        direction.setY(0).normalize(); // Mantener movimiento horizontal

        org.bukkit.Location end = start.clone().add(direction.clone().multiply(range));
        end.setY(start.getY()); // Mantener la misma altura

        // Ajustar si hay bloque sólido o vacío
        if (end.getBlock().getType().isSolid()) {
            while (end.getBlock().getType().isSolid() && end.getY() < player.getWorld().getMaxHeight()) {
                end.add(0, 1, 0);
            }
        } else if (!end.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
            while (!end.clone().add(0, -1, 0).getBlock().getType().isSolid() && end.getY() > player.getWorld().getMinHeight()) {
                end.add(0, -1, 0);
            }
        }

        // Calcular la distancia real que vamos a recorrer
        double actualDistance = start.distance(end);

        // Dañar enemigos en el camino
        int enemiesHit = 0;
        for (double d = 0; d <= actualDistance; d += 1.0) {
            org.bukkit.Location point = start.clone().add(direction.clone().multiply(d));
            // Usar PORTAL en lugar de DRAGON_BREATH que requiere Float data
            player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, point, 8, 0.2, 0.2, 0.2, 0.5);
            player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, point, 3, 0.1, 0.1, 0.1, 0.02);

            for (org.bukkit.entity.Entity entity : player.getWorld().getNearbyEntities(point, aoe, aoe, aoe)) {
                if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                    org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) entity;
                    if (!target.hasMetadata("rpg-dragon-flight-hit")) {
                        applyAbilityDamage(target, player, damage, ability);
                        target.setMetadata("rpg-dragon-flight-hit", new FixedMetadataValue(plugin, true));
                        target.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.8);
                        enemiesHit++;
                    }
                }
            }
        }

        // Limpiar metadatos
        for (org.bukkit.entity.Entity entity : player.getWorld().getNearbyEntities(end, range * 2, range * 2, range * 2)) {
            entity.removeMetadata("rpg-dragon-flight-hit", plugin);
        }

        // Teletransportar jugador
        player.teleport(end);
        player.getWorld().playSound(end, org.bukkit.Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f);
        player.sendMessage("§5🐉 ¡Vuelo del Dragón! §7(" + enemiesHit + " enemigos atravesados, Daño: §e" + String.format("%.1f", damage) + "§7) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Llama Bailarina - Salta entre entidades curando aliados y dañando enemigos
     */
    private void executeDancingFlame(Player player, Ability ability, double damage, PlayerData playerData) {
        double range = ability.getRange() > 0 ? ability.getRange() : 10.0;
        int bounces = ability.getIntProperty("bounces", 3);
        double healAmount = playerData.getStats().getIntelligence() * 0.6 + playerData.getStats().getSacredPower() * 0.3;

        java.util.List<org.bukkit.entity.LivingEntity> targets = new java.util.ArrayList<>();
        java.util.Set<org.bukkit.entity.LivingEntity> alreadyHit = new java.util.HashSet<>();

        // Encontrar objetivos aleatorios
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof org.bukkit.entity.LivingEntity) {
                targets.add((org.bukkit.entity.LivingEntity) entity);
            }
        }

        if (targets.isEmpty()) {
            player.sendMessage("§c✗ No hay objetivos cercanos.");
            return;
        }

        java.util.Collections.shuffle(targets);
        int enemiesHit = 0;
        int alliesHealed = 0;
        org.bukkit.Location lastPos = player.getEyeLocation();

        for (int i = 0; i < Math.min(bounces, targets.size()); i++) {
            org.bukkit.entity.LivingEntity target = targets.get(i);
            if (alreadyHit.contains(target)) continue;
            alreadyHit.add(target);

            // Efecto visual de llama saltando
            org.bukkit.Location targetLoc = target.getLocation().add(0, 1, 0);
            org.bukkit.util.Vector dir = targetLoc.toVector().subtract(lastPos.toVector()).normalize();
            for (double d = 0; d < lastPos.distance(targetLoc); d += 0.5) {
                org.bukkit.Location point = lastPos.clone().add(dir.clone().multiply(d));
                player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, point, 2, 0.05, 0.05, 0.05, 0);
            }

            if (target instanceof Player) {
                Player ally = (Player) target;
                double maxHealth = ally.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                ally.setHealth(Math.min(ally.getHealth() + healAmount, maxHealth));
                ally.getWorld().spawnParticle(org.bukkit.Particle.HEART, ally.getLocation().add(0, 2, 0), 5, 0.3, 0.3, 0.3, 0);
                alliesHealed++;
            } else {
                applyAbilityDamage(target, player, damage * 0.7, ability); // Daño reducido por rebote
                target.getWorld().spawnParticle(org.bukkit.Particle.FLAME, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                enemiesHit++;
            }

            lastPos = targetLoc;
        }

        spawnAbilityParticles(player, ability);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.3f);
        double actualDamage = damage * 0.7;
        player.sendMessage("§6🔥 ¡Llama Bailarina! §7(" + bounces + " saltos: " + enemiesHit + " enemigos [Daño: §e" + String.format("%.1f", actualDamage) + "§7], " + alliesHealed + " aliados [Curación: §a" + String.format("%.1f", healAmount) + "§7]) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Rugido del Dragón - Buff masivo a aliados cercanos
     */
    private void executeDragonRoar(Player player, Ability ability) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 12.0;
        int duration = ability.getIntProperty("buff-duration", 30);
        double multiplier = ability.getDoubleProperty("buff-multiplier", 2.0);

        // Bonus fijos: +200% significa multiplicar por 2, así que añadimos el 100% de la vida base
        // Mínimo de 20 de vida y 10 de armadura para que siempre funcione
        double baseHealthBonus = 40.0; // Bonus base de vida
        double baseArmorBonus = 10.0;  // Bonus base de armadura

        int alliesBuffed = 0;

        // Aplicar a aliados cercanos
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(aoe, aoe, aoe)) {
            if (entity instanceof Player) {
                Player ally = (Player) entity;
                applyDragonRoarBuff(ally, multiplier, baseHealthBonus, baseArmorBonus, duration);
                alliesBuffed++;
            }
        }

        // Buff al propio jugador también (SIEMPRE)
        applyDragonRoarBuff(player, multiplier, baseHealthBonus, baseArmorBonus, duration);
        alliesBuffed++; // Contarse a sí mismo

        spawnAbilityParticles(player, ability);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.7f);
        player.sendMessage("§6🐉 ¡Rugido del Dragón! §7(" + alliesBuffed + " aliados potenciados por " + duration + "s) (-§9" + ability.getManaCost() + " maná§7)");
    }

    private void applyDragonRoarBuff(Player target, double multiplier, double baseHealthBonus, double baseArmorBonus, int duration) {
        org.bukkit.attribute.AttributeInstance healthAttr = target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        org.bukkit.attribute.AttributeInstance armorAttr = target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR);

        double healthBonus = 0;
        double armorBonus = 0;

        // Calcular bonus de vida
        if (healthAttr != null) {
            double currentMax = healthAttr.getValue();
            healthBonus = Math.max(currentMax * multiplier, baseHealthBonus);
        }

        // Calcular bonus de armadura
        if (armorAttr != null) {
            double currentArmor = armorAttr.getValue();
            armorBonus = Math.max(currentArmor * multiplier, baseArmorBonus);
        }

        // Usar efectos de poción para el buff (más confiable y sincroniza con el cliente)
        // HEALTH_BOOST: cada nivel añade +4 vida máxima (2 corazones)
        int healthBoostLevel = (int) Math.ceil(healthBonus / 4.0);
        healthBoostLevel = Math.min(healthBoostLevel, 100);

        // Aplicar efecto de vida extra
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.HEALTH_BOOST,
            duration * 20,
            healthBoostLevel - 1,
            false, true, true
        ));

        // Para la armadura, guardar el valor original y modificar directamente
        if (armorAttr != null) {
            double originalArmor = armorAttr.getBaseValue();
            armorAttr.setBaseValue(originalArmor + armorBonus);

            // Guardar metadatos para este jugador
            String metaKey = "rpg-dragon-roar-" + target.getUniqueId().toString().substring(0, 8);
            target.setMetadata(metaKey, new FixedMetadataValue(plugin, originalArmor));

            // Programar restauración de armadura
            final double finalOriginalArmor = originalArmor;
            final String finalMetaKey = metaKey;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (target.isOnline() && target.hasMetadata(finalMetaKey)) {
                    org.bukkit.attribute.AttributeInstance armor = target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR);
                    if (armor != null) {
                        armor.setBaseValue(finalOriginalArmor);
                    }
                    target.removeMetadata(finalMetaKey, plugin);
                    target.sendMessage("§c🐉 El Rugido del Dragón ha terminado.");
                }
            }, duration * 20L);
        }

        // Curar al jugador inmediatamente
        double maxHealth = target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        target.setHealth(Math.min(target.getHealth() + healthBonus, maxHealth));

        target.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, target.getLocation().add(0, 1, 0), 20, 0.5, 1, 0.5, 0.1);
        target.sendMessage("§6🐉 ¡Rugido del Dragón! §7(+" + String.format("%.0f", healthBonus) + " vida, +" + String.format("%.0f", armorBonus) + " armadura por " + duration + "s)");
    }

    /**
     * Rayo de los Dragones Ancestrales - Gran AoE que cura aliados y daña enemigos fuertemente
     */
    private void executeAncestralRay(Player player, Ability ability, double damage, PlayerData playerData) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 8.0;
        double healAmount = playerData.getStats().getIntelligence() * 1.0 + playerData.getStats().getSacredPower() * 0.6;

        int enemiesHit = 0;
        int alliesHealed = 0;

        // Efecto visual de rayo masivo - Usar PORTAL y END_ROD en lugar de DRAGON_BREATH
        for (int i = 0; i < 360; i += 15) {
            double radians = Math.toRadians(i);
            for (double r = 0; r <= aoe; r += 0.5) {
                double x = Math.cos(radians) * r;
                double z = Math.sin(radians) * r;
                org.bukkit.Location loc = player.getLocation().add(x, 0.5, z);
                player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, loc, 2, 0, 0, 0, 0.5);
                if (r % 1.0 < 0.5) {
                    player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, loc, 1, 0, 0, 0, 0.01);
                }
            }
        }

        // Curar al propio lanzador primero
        double maxHealthSelf = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        player.setHealth(Math.min(player.getHealth() + healAmount, maxHealthSelf));
        player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5, 0);
        alliesHealed++;

        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(aoe, aoe, aoe)) {
            if (entity instanceof org.bukkit.entity.LivingEntity) {
                org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) entity;

                if (target instanceof Player) {
                    Player ally = (Player) target;
                    double maxHealth = ally.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                    ally.setHealth(Math.min(ally.getHealth() + healAmount, maxHealth));
                    ally.getWorld().spawnParticle(org.bukkit.Particle.HEART, ally.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5, 0);
                    alliesHealed++;
                } else {
                    applyAbilityDamage(target, player, damage * 1.5, ability); // 50% más de daño
                    target.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, target.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 1.0);
                    target.getWorld().spawnParticle(org.bukkit.Particle.WITCH, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0);
                    enemiesHit++;
                }
            }
        }

        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.5f, 0.8f);
        player.sendMessage("§5⚡ ¡Rayo de los Dragones Ancestrales! §7(" + enemiesHit + " enemigos [Daño: §e" + String.format("%.1f", damage * 1.5) + "§7], " + alliesHealed + " aliados [Curación: §a" + String.format("%.1f", healAmount) + "§7]) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * La Llamada del Último Dragón - Invoca un mini dragón que ataca enemigos
     */
    private void executeLastDragonCall(Player player, Ability ability, double damage) {
        int duration = ability.getIntProperty("summon-duration", 30);
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 15.0;

        // Crear el "dragón" usando un Phantom como base visual
        org.bukkit.Location spawnLoc = player.getLocation().add(0, 3, 0);
        org.bukkit.entity.Phantom dragon = (org.bukkit.entity.Phantom) player.getWorld().spawnEntity(spawnLoc, org.bukkit.entity.EntityType.PHANTOM);
        dragon.setSize(3); // Hacerlo más grande
        dragon.setCustomName("§5Dragón Ancestral de " + player.getName());
        dragon.setCustomNameVisible(true);
        dragon.setMetadata("rpg-summoned-dragon", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        dragon.setMetadata("rpg-dragon-damage", new FixedMetadataValue(plugin, damage));

        // Hacer que no ataque al invocador
        dragon.setTarget(null);

        spawnAbilityParticles(player, ability);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.0f);

        // Tarea para hacer que el dragón ataque enemigos cercanos
        org.bukkit.scheduler.BukkitTask attackTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (dragon.isDead()) return;

            // Buscar enemigo más cercano
            org.bukkit.entity.LivingEntity nearestEnemy = null;
            double nearestDist = aoe;

            for (org.bukkit.entity.Entity entity : dragon.getNearbyEntities(aoe, aoe, aoe)) {
                if (entity instanceof org.bukkit.entity.LivingEntity && !(entity instanceof Player) && !(entity instanceof org.bukkit.entity.Phantom)) {
                    double dist = dragon.getLocation().distance(entity.getLocation());
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearestEnemy = (org.bukkit.entity.LivingEntity) entity;
                    }
                }
            }

            if (nearestEnemy != null) {
                dragon.setTarget(nearestEnemy);
                // Atacar si está cerca
                if (nearestDist < 3) {
                    applyAbilityDamage(nearestEnemy, player, damage * 0.5, ability);
                    nearestEnemy.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, nearestEnemy.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.8);
                    nearestEnemy.getWorld().spawnParticle(org.bukkit.Particle.WITCH, nearestEnemy.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0);
                }
            }
        }, 20L, 20L);

        // Programar eliminación del dragón
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            attackTask.cancel();
            if (!dragon.isDead()) {
                dragon.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, dragon.getLocation(), 50, 1, 1, 1, 0.5);
                dragon.remove();
                player.sendMessage("§5🐉 Tu Dragón Ancestral ha regresado a las sombras.");
            }
        }, duration * 20L);

        player.sendMessage("§5🐉 ¡La Llamada del Último Dragón! §7(Dragón invocado por " + duration + "s) (-§9" + ability.getManaCost() + " maná§7)");
    }

    // ==================== HABILIDADES DEL GUERRERO ====================

    /**
     * Corte Profundo - Daño + sangrado
     */
    private void executeDeepCut(Player player, Ability ability, double damage) {
        int bleedDuration = ability.getIntProperty("bleed-duration", 10);
        int bleedLevel = ability.getIntProperty("bleed-level", 1);

        // Buscar objetivo en línea de visión
        org.bukkit.entity.LivingEntity target = getTargetInSight(player, ability.getRange());

        if (target == null) {
            player.sendMessage("§c✗ No hay objetivo en rango.");
            return;
        }

        // Aplicar daño
        applyAbilityDamage(target, player, damage, ability);

        // Aplicar sangrado (usando WITHER como efecto de sangrado)
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.WITHER,
            bleedDuration * 20,
            bleedLevel - 1,
            false, true, true
        ));

        // Efectos visuales
        target.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
        target.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, target.getLocation().add(0, 0.5, 0), 20, 0.3, 0.3, 0.3, 0, org.bukkit.Material.REDSTONE_BLOCK.createBlockData());
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);

        spawnAbilityParticles(player, ability);
        player.sendMessage("§c⚔ ¡Corte Profundo! §7(Daño: §e" + String.format("%.1f", damage) + "§7, Sangrado: " + bleedDuration + "s) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Embestida - Dash hacia adelante, si impacta daña y reduce armadura del enemigo
     */
    private void executeCharge(Player player, Ability ability, double damage) {
        double range = ability.getRange() > 0 ? ability.getRange() : 6.0;
        double armorReduction = ability.getDoubleProperty("armor-reduction", 0.2);
        int debuffDuration = ability.getIntProperty("debuff-duration", 10);

        org.bukkit.Location start = player.getLocation().clone();
        org.bukkit.util.Vector direction = player.getLocation().getDirection().normalize();
        direction.setY(0); // Mantener en el mismo nivel Y

        org.bukkit.entity.LivingEntity hitTarget = null;
        org.bukkit.Location endLoc = start.clone();

        // Buscar enemigo en el camino
        for (double d = 1; d <= range; d += 0.5) {
            org.bukkit.Location point = start.clone().add(direction.clone().multiply(d));

            // Verificar si este punto es válido (no dentro de bloque sólido)
            if (point.getBlock().getType().isSolid()) {
                break; // Paramos si hay un muro
            }

            endLoc = point.clone();

            // Buscar enemigos en este punto (radio más amplio)
            for (org.bukkit.entity.Entity entity : player.getWorld().getNearbyEntities(point, 1.5, 2, 1.5)) {
                if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                    hitTarget = (org.bukkit.entity.LivingEntity) entity;
                    break;
                }
            }

            if (hitTarget != null) break;

            // Efecto visual del dash
            player.getWorld().spawnParticle(org.bukkit.Particle.CRIT, point, 3, 0.1, 0.1, 0.1, 0);
        }

        // Teletransportar al jugador - mantener Y similar pero ajustar si es necesario
        endLoc.setY(start.getY()); // Mantener la misma altura
        // Ajustar si hay suelo diferente
        org.bukkit.block.Block groundCheck = endLoc.clone().add(0, -1, 0).getBlock();
        if (!groundCheck.getType().isSolid()) {
            // No hay suelo, bajar hasta encontrarlo
            while (!endLoc.clone().add(0, -1, 0).getBlock().getType().isSolid() && endLoc.getY() > player.getWorld().getMinHeight()) {
                endLoc.add(0, -1, 0);
            }
        } else if (endLoc.getBlock().getType().isSolid()) {
            // Estamos dentro de un bloque, subir hasta salir
            while (endLoc.getBlock().getType().isSolid() && endLoc.getY() < player.getWorld().getMaxHeight()) {
                endLoc.add(0, 1, 0);
            }
        }
        player.teleport(endLoc);
        player.getWorld().playSound(endLoc, org.bukkit.Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.2f);

        if (hitTarget != null) {
            // Impactó a un enemigo
            applyAbilityDamage(hitTarget, player, damage, ability);

            // Reducir armadura del enemigo
            org.bukkit.attribute.AttributeInstance armorAttr = hitTarget.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR);
            if (armorAttr != null) {
                double currentArmor = armorAttr.getBaseValue();
                double reduction = currentArmor * armorReduction;
                armorAttr.setBaseValue(currentArmor - reduction);

                // Restaurar después del debuff
                final double finalReduction = reduction;
                final org.bukkit.entity.LivingEntity finalTarget = hitTarget;
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!finalTarget.isDead()) {
                        org.bukkit.attribute.AttributeInstance armor = finalTarget.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR);
                        if (armor != null) {
                            armor.setBaseValue(armor.getBaseValue() + finalReduction);
                        }
                    }
                }, debuffDuration * 20L);
            }

            hitTarget.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, hitTarget.getLocation(), 1, 0, 0, 0, 0);
            player.sendMessage("§c⚔ ¡Embestida! §7¡Impacto! (Daño: §e" + String.format("%.1f", damage) + "§7, Armadura -" + (int)(armorReduction * 100) + "%) (-§9" + ability.getManaCost() + " maná§7)");
        } else {
            // Falló - el jugador sufre el daño y reducción de armadura
            player.damage(damage * 0.5);

            org.bukkit.attribute.AttributeInstance armorAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR);
            if (armorAttr != null) {
                double currentArmor = armorAttr.getBaseValue();
                double reduction = currentArmor * armorReduction;
                armorAttr.setBaseValue(currentArmor - reduction);

                final double finalReduction = reduction;
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        org.bukkit.attribute.AttributeInstance armor = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR);
                        if (armor != null) {
                            armor.setBaseValue(armor.getBaseValue() + finalReduction);
                        }
                    }
                }, debuffDuration * 20L);
            }

            player.sendMessage("§c⚔ ¡Embestida fallida! §7Sufres el daño y la penalización de armadura. (-§9" + ability.getManaCost() + " maná§7)");
        }

        spawnAbilityParticles(player, ability);
    }

    /**
     * Romper Corazas - Daño + reducción masiva de armadura
     */
    private void executeArmorBreak(Player player, Ability ability, double damage) {
        double armorReduction = ability.getDoubleProperty("armor-reduction", 0.4);
        int debuffDuration = ability.getIntProperty("debuff-duration", 15);
        double range = ability.getRange() > 0 ? ability.getRange() : 3.0;

        // Buscar objetivo - primero en línea de visión, luego el más cercano
        org.bukkit.entity.LivingEntity target = getTargetInSight(player, range);

        // Si no encontró en línea de visión, buscar el más cercano en rango
        if (target == null) {
            double closestDist = range + 1;
            for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
                if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                    double dist = entity.getLocation().distance(player.getLocation());
                    if (dist < closestDist) {
                        closestDist = dist;
                        target = (org.bukkit.entity.LivingEntity) entity;
                    }
                }
            }
        }

        if (target == null) {
            player.sendMessage("§c✗ No hay objetivo en rango.");
            return;
        }

        // Aplicar daño
        applyAbilityDamage(target, player, damage, ability);

        // Reducir armadura del objetivo
        org.bukkit.attribute.AttributeInstance armorAttr = target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR);
        if (armorAttr != null) {
            double currentArmor = armorAttr.getBaseValue();
            double reduction = currentArmor * armorReduction;
            armorAttr.setBaseValue(currentArmor - reduction);

            final double finalReduction = reduction;
            final org.bukkit.entity.LivingEntity finalTarget = target;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!finalTarget.isDead()) {
                    org.bukkit.attribute.AttributeInstance armor = finalTarget.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR);
                    if (armor != null) {
                        armor.setBaseValue(armor.getBaseValue() + finalReduction);
                    }
                }
            }, debuffDuration * 20L);
        }

        // Efectos visuales
        target.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 25, 0.4, 0.5, 0.4, 0.1);
        target.getWorld().spawnParticle(org.bukkit.Particle.ITEM, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1, new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_CHESTPLATE));
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ITEM_SHIELD_BREAK, 1.0f, 0.6f);

        spawnAbilityParticles(player, ability);
        player.sendMessage("§c⚔ ¡Romper Corazas! §7(Daño: §e" + String.format("%.1f", damage) + "§7, Armadura -" + (int)(armorReduction * 100) + "% por " + debuffDuration + "s) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Atronar - AoE que daña y ralentiza
     */
    private void executeThunderStrike(Player player, Ability ability, double damage) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 3.0;
        int slowPercent = ability.getIntProperty("slow-percent", 20);
        int slowDuration = ability.getIntProperty("slow-duration", 5);

        int enemiesHit = 0;

        // Efecto visual del golpe al suelo
        player.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, player.getLocation(), 3, 0.5, 0.1, 0.5, 0);

        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(aoe, aoe, aoe)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) entity;

                // Aplicar daño
                applyAbilityDamage(target, player, damage, ability);

                // Aplicar ralentización (SLOW)
                int slowLevel = Math.max(0, (slowPercent / 20) - 1); // 20% = nivel 0, 40% = nivel 1, etc.
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS,
                    slowDuration * 20,
                    slowLevel,
                    false, true, true
                ));

                target.getWorld().spawnParticle(org.bukkit.Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
                enemiesHit++;
            }
        }

        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
        spawnAbilityParticles(player, ability);
        player.sendMessage("§c⚔ ¡Atronar! §7(" + enemiesHit + " enemigos, Daño: §e" + String.format("%.1f", damage) + "§7, Ralentización: -" + slowPercent + "%) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Sed de Sangre - Buff de daño y robo de vida
     */
    private void executeBloodThirst(Player player, Ability ability) {
        int duration = ability.getIntProperty("buff-duration", 20);
        double damageBonus = ability.getDoubleProperty("damage-bonus", 0.2);
        double lifesteal = ability.getDoubleProperty("lifesteal", 0.1);

        // Guardar metadatos del buff
        player.setMetadata("rpg-blood-thirst", new FixedMetadataValue(plugin, System.currentTimeMillis() + (duration * 1000L)));
        player.setMetadata("rpg-blood-thirst-damage", new FixedMetadataValue(plugin, damageBonus));
        player.setMetadata("rpg-blood-thirst-lifesteal", new FixedMetadataValue(plugin, lifesteal));

        // Efecto visual de activación
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.STRENGTH,
            duration * 20,
            0,
            false, true, true
        ));

        // Programar eliminación del buff
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.hasMetadata("rpg-blood-thirst")) {
                player.removeMetadata("rpg-blood-thirst", plugin);
                player.removeMetadata("rpg-blood-thirst-damage", plugin);
                player.removeMetadata("rpg-blood-thirst-lifesteal", plugin);
                player.sendMessage("§c⚔ Sed de Sangre ha terminado.");
            }
        }, duration * 20L);

        player.getWorld().spawnParticle(org.bukkit.Particle.ANGRY_VILLAGER, player.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
        spawnAbilityParticles(player, ability);
        player.sendMessage("§c⚔ ¡Sed de Sangre! §7(+" + (int)(damageBonus * 100) + "% daño, +" + (int)(lifesteal * 100) + "% robo de vida por " + duration + "s) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Torbellino Sangriento - AoE durante 3 segundos
     */
    private void executeBloodyWhirlwind(Player player, Ability ability, double damagePerTick) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 4.0;
        int spinDuration = ability.getIntProperty("spin-duration", 3);

        player.setMetadata("rpg-whirlwind", new FixedMetadataValue(plugin, true));

        // Tarea que se ejecuta cada 0.5 segundos durante la duración del spin
        final int[] ticksRemaining = {spinDuration * 2}; // 2 ticks por segundo (cada 10 game ticks)
        org.bukkit.scheduler.BukkitTask spinTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (ticksRemaining[0] <= 0 || !player.isOnline()) return;

            // Efecto visual de giro
            for (double angle = 0; angle < 360; angle += 30) {
                double radians = Math.toRadians(angle);
                double x = Math.cos(radians) * aoe * 0.8;
                double z = Math.sin(radians) * aoe * 0.8;
                org.bukkit.Location particleLoc = player.getLocation().add(x, 0.5, z);
                player.getWorld().spawnParticle(org.bukkit.Particle.SWEEP_ATTACK, particleLoc, 1, 0, 0, 0, 0);
                player.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, particleLoc, 2, 0.2, 0.2, 0.2, 0);
            }

            // Dañar enemigos cercanos
            for (org.bukkit.entity.Entity entity : player.getNearbyEntities(aoe, aoe, aoe)) {
                if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                    org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) entity;
                    applyAbilityDamage(target, player, damagePerTick * 0.5, ability); // Daño por tick (mitad porque son 2 ticks/s)
                }
            }

            ticksRemaining[0]--;
        }, 0L, 10L); // Cada 10 ticks (0.5 segundos)

        // Cancelar la tarea después de la duración
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            spinTask.cancel();
            player.removeMetadata("rpg-whirlwind", plugin);
            player.sendMessage("§c⚔ Torbellino Sangriento finalizado.");
        }, spinDuration * 20L);

        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
        spawnAbilityParticles(player, ability);
        player.sendMessage("§c⚔ ¡Torbellino Sangriento! §7(Daño AoE por " + spinDuration + "s) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Ejecutar - Daño verdadero, resetea cooldown si mata
     */
    private void executeExecute(Player player, Ability ability, PlayerData playerData) {
        int baseDamage = ability.getIntProperty("base-damage", 100);
        double agilityScaling = 0.1;
        double trueDamage = baseDamage + (playerData.getStats().getAgility() * agilityScaling);

        org.bukkit.entity.LivingEntity target = getTargetInSight(player, ability.getRange());

        if (target == null) {
            player.sendMessage("§c✗ No hay objetivo en rango.");
            return;
        }

        // Guardar vida antes del daño para verificar si murió
        double healthBefore = target.getHealth();

        // Aplicar daño verdadero (ignorar armadura)
        double newHealth = target.getHealth() - trueDamage;
        boolean killed = newHealth <= 0;
        if (killed) {
            // Usar damage() para que cuente como kill del jugador
            target.damage(target.getHealth() + 1, player);
        } else {
            target.setHealth(newHealth);
        }

        // Efectos visuales
        target.getWorld().spawnParticle(org.bukkit.Particle.CRIT, target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.5);
        target.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.1);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.6f);

        // Verificar si mató al objetivo
        if (killed) {
            // Resetear cooldown
            java.util.UUID uuid = player.getUniqueId();
            Map<String, Long> playerCooldowns = abilityCooldowns.get(uuid);
            if (playerCooldowns != null) {
                playerCooldowns.remove(ability.getId());
            }

            player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, target.getLocation(), 30, 0.5, 1, 0.5, 0.3);
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.sendMessage("§c⚔ ¡EJECUCIÓN! §a¡Cooldown reiniciado! §7(Daño verdadero: §e" + String.format("%.0f", trueDamage) + "§7) (-§9" + ability.getManaCost() + " maná§7)");
        } else {
            player.sendMessage("§c⚔ ¡Ejecutar! §7(Daño verdadero: §e" + String.format("%.0f", trueDamage) + "§7) (-§9" + ability.getManaCost() + " maná§7)");
        }

        spawnAbilityParticles(player, ability);
    }

    /**
     * Ira Furibunda - Buff masivo temporal
     */
    private void executeFuriousRage(Player player, Ability ability) {
        int duration = ability.getIntProperty("buff-duration", 30);
        double damageBonus = ability.getDoubleProperty("damage-bonus", 0.3);
        double attackSpeedBonus = ability.getDoubleProperty("attack-speed-bonus", 0.2);
        int tempHealth = ability.getIntProperty("temp-health", 100);

        // Guardar metadatos del buff
        player.setMetadata("rpg-furious-rage", new FixedMetadataValue(plugin, System.currentTimeMillis() + (duration * 1000L)));
        player.setMetadata("rpg-furious-rage-damage", new FixedMetadataValue(plugin, damageBonus));

        // Aplicar efectos de poción
        // Velocidad de ataque
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.HASTE,
            duration * 20,
            1,
            false, true, true
        ));

        // Inmunidad a veneno y efectos negativos
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.RESISTANCE,
            duration * 20,
            1,
            false, true, true
        ));

        // Vida temporal con ABSORPTION
        int absorptionLevel = (int) Math.ceil(tempHealth / 4.0) - 1;
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.ABSORPTION,
            duration * 20,
            Math.min(absorptionLevel, 20),
            false, true, true
        ));

        // Fuerza extra
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.STRENGTH,
            duration * 20,
            1,
            false, true, true
        ));

        // Efecto visual de furia
        player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(org.bukkit.Particle.LAVA, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0);

        // Programar fin del buff
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.hasMetadata("rpg-furious-rage")) {
                player.removeMetadata("rpg-furious-rage", plugin);
                player.removeMetadata("rpg-furious-rage-damage", plugin);
                player.sendMessage("§c⚔ Ira Furibunda ha terminado.");
            }
        }, duration * 20L);

        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
        spawnAbilityParticles(player, ability);
        player.sendMessage("§c⚔ ¡IRA FURIBUNDA! §7(+" + (int)(damageBonus * 100) + "% daño, +" + (int)(attackSpeedBonus * 100) + "% vel. ataque, +" + tempHealth + " vida temporal, inmunidad a estados) (-§9" + ability.getManaCost() + " maná§7)");
    }

    // ==================== HABILIDADES DEL INVOCADOR ====================

    /**
     * Erupción de Fuego - DoT AoE de fuego
     */
    private void executeFireEruption(Player player, Ability ability, double damagePerTick) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 3.0;
        int dotDuration = ability.getIntProperty("dot-duration", 5);
        int dotTicks = ability.getIntProperty("dot-ticks", 5);

        // Obtener ubicación a donde mira el jugador (máximo 8 bloques)
        org.bukkit.Location center = getTargetLocation(player, 8);

        // Aplicar DoT cada segundo durante la duración
        for (int tick = 0; tick < dotTicks; tick++) {
            final int currentTick = tick;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Efectos visuales
                center.getWorld().spawnParticle(org.bukkit.Particle.FLAME, center, 40, aoe * 0.5, 0.5, aoe * 0.5, 0.05);
                center.getWorld().spawnParticle(org.bukkit.Particle.LAVA, center, 15, aoe * 0.4, 0.3, aoe * 0.4, 0);

                // Dañar enemigos en el área
                for (org.bukkit.entity.Entity entity : center.getWorld().getNearbyEntities(center, aoe, 2, aoe)) {
                    if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                        org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) entity;
                        applyAbilityDamage(target, player, damagePerTick, ability);
                        target.setFireTicks(40);
                    }
                }

                if (currentTick == 0) {
                    center.getWorld().playSound(center, org.bukkit.Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
                }
            }, tick * 20L);
        }

        spawnAbilityParticles(player, ability);
        player.sendMessage("§6🔥 ¡Erupción de Fuego! §7(Daño: §e" + String.format("%.1f", damagePerTick) + "/s§7 x" + dotDuration + "s, Área: " + (int)aoe + ") (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Trueno Primigenio - AoE de daño instantáneo con rayo
     */
    private void executePrimordialThunder(Player player, Ability ability, double damage) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 5.0;
        // Obtener ubicación a donde mira el jugador (máximo 8 bloques)
        org.bukkit.Location center = getTargetLocation(player, 8);

        // Efectos visuales de tormenta
        center.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK, center.clone().add(0, 2, 0), 60, aoe * 0.5, 1, aoe * 0.5, 0.1);
        center.getWorld().strikeLightningEffect(center);

        int enemiesHit = 0;

        // Dañar enemigos en el área
        for (org.bukkit.entity.Entity entity : center.getWorld().getNearbyEntities(center, aoe, 3, aoe)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) entity;
                applyAbilityDamage(target, player, damage, ability);
                target.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
                enemiesHit++;
            }
        }

        center.getWorld().playSound(center, org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.2f);
        spawnAbilityParticles(player, ability);
        player.sendMessage("§b⚡ ¡Trueno Primigenio! §7(" + enemiesHit + " enemigos, Daño: §e" + String.format("%.1f", damage) + "§7, Área: " + (int)aoe + ") (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Maremoto - DoT AoE de agua
     */
    private void executeTidalWave(Player player, Ability ability, double damagePerTick) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 6.0;
        int dotDuration = ability.getIntProperty("dot-duration", 5);
        int dotTicks = ability.getIntProperty("dot-ticks", 5);

        // Obtener ubicación a donde mira el jugador (máximo 8 bloques)
        org.bukkit.Location center = getTargetLocation(player, 8);

        // Aplicar DoT cada segundo durante la duración
        for (int tick = 0; tick < dotTicks; tick++) {
            final int currentTick = tick;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Efectos visuales de agua
                center.getWorld().spawnParticle(org.bukkit.Particle.FALLING_WATER, center.clone().add(0, 1, 0), 50, aoe * 0.5, 0.5, aoe * 0.5, 0);
                center.getWorld().spawnParticle(org.bukkit.Particle.BUBBLE_POP, center.clone().add(0, 0.5, 0), 30, aoe * 0.4, 0.3, aoe * 0.4, 0.05);

                // Dañar y ralentizar enemigos en el área
                for (org.bukkit.entity.Entity entity : center.getWorld().getNearbyEntities(center, aoe, 2, aoe)) {
                    if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                        org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) entity;
                        applyAbilityDamage(target, player, damagePerTick, ability);
                        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 1, false, true, false
                        ));
                    }
                }

                if (currentTick == 0) {
                    center.getWorld().playSound(center, org.bukkit.Sound.ENTITY_GENERIC_SPLASH, 1.0f, 0.6f);
                }
            }, tick * 20L);
        }

        spawnAbilityParticles(player, ability);
        player.sendMessage("§3🌊 ¡Maremoto! §7(Daño: §e" + String.format("%.1f", damagePerTick) + "/s§7 x" + dotDuration + "s, Área: " + (int)aoe + ") (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Vulcano - DoT AoE de lava (más poderoso)
     */
    private void executeVolcano(Player player, Ability ability, double damagePerTick) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 6.0;
        int dotDuration = ability.getIntProperty("dot-duration", 5);
        int dotTicks = ability.getIntProperty("dot-ticks", 5);

        // Obtener ubicación a donde mira el jugador (máximo 8 bloques)
        org.bukkit.Location center = getTargetLocation(player, 8);

        // Aplicar DoT cada segundo durante la duración
        for (int tick = 0; tick < dotTicks; tick++) {
            final int currentTick = tick;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Efectos visuales de volcán
                center.getWorld().spawnParticle(org.bukkit.Particle.LAVA, center, 30, aoe * 0.5, 0.3, aoe * 0.5, 0);
                center.getWorld().spawnParticle(org.bukkit.Particle.FLAME, center.clone().add(0, 1, 0), 50, aoe * 0.4, 1, aoe * 0.4, 0.1);
                center.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, center.clone().add(0, 2, 0), 20, aoe * 0.3, 0.5, aoe * 0.3, 0.05);

                // Dañar enemigos en el área
                for (org.bukkit.entity.Entity entity : center.getWorld().getNearbyEntities(center, aoe, 3, aoe)) {
                    if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                        org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) entity;
                        applyAbilityDamage(target, player, damagePerTick, ability);
                        target.setFireTicks(60);
                    }
                }

                if (currentTick == 0) {
                    center.getWorld().playSound(center, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                }
            }, tick * 20L);
        }

        spawnAbilityParticles(player, ability);
        player.sendMessage("§4🌋 ¡VULCANO! §7(Daño: §e" + String.format("%.1f", damagePerTick) + "/s§7 x" + dotDuration + "s, Área: " + (int)aoe + ") (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Invoca un elemental que lucha por el jugador
     */
    private void executeSummonElemental(Player player, Ability ability, PlayerData playerData, String entityType, String elementName) {
        int summonDuration = ability.getIntProperty("summon-duration", 60);
        double damageScaling = ability.getDoubleProperty("summon-damage-scaling", 0.6);

        // Calcular daño basado en inteligencia
        int intelligence = playerData.getStats().getIntelligence();
        double summonDamage = intelligence * damageScaling;

        org.bukkit.Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(2));
        spawnLoc.setY(player.getLocation().getY());

        // Crear el elemental
        org.bukkit.entity.EntityType eType;
        try {
            eType = org.bukkit.entity.EntityType.valueOf(entityType);
        } catch (IllegalArgumentException e) {
            eType = org.bukkit.entity.EntityType.BLAZE;
        }

        org.bukkit.entity.LivingEntity elemental = (org.bukkit.entity.LivingEntity) player.getWorld().spawnEntity(spawnLoc, eType);

        // Configurar el elemental
        elemental.setCustomName("§6Elemental de " + elementName + " §7[" + player.getName() + "]");
        elemental.setCustomNameVisible(true);

        // Configuración especial para Iron Golem - marcar como creado por jugador
        if (elemental instanceof org.bukkit.entity.IronGolem) {
            ((org.bukkit.entity.IronGolem) elemental).setPlayerCreated(true);
        }

        // Calcular vida del elemental basada en stats del invocador (0.1 vida + 0.2 inteligencia)
        int playerHealth = playerData.getStats().getHealth();
        double elementalHealth = (playerHealth * 0.1) + (intelligence * 0.2);
        elementalHealth = Math.max(20, elementalHealth); // Mínimo 20 de vida
        org.bukkit.attribute.AttributeInstance healthAttr = elemental.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(elementalHealth);
            elemental.setHealth(elementalHealth);
        }

        // Hacer que no ataque al dueño
        if (elemental instanceof org.bukkit.entity.Mob) {
            org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) elemental;
            mob.setTarget(null);
        }

        // Guardar metadatos para identificar al dueño
        elemental.setMetadata("rpg-summon-owner", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        elemental.setMetadata("rpg-summon-damage", new FixedMetadataValue(plugin, summonDamage));

        // Marcar con PersistentDataContainer (persiste entre reinicios)
        org.bukkit.NamespacedKey summonKey = new org.bukkit.NamespacedKey(plugin, "rpg-summon");
        elemental.getPersistentDataContainer().set(summonKey,
            org.bukkit.persistence.PersistentDataType.STRING, player.getUniqueId().toString());

        // Prevenir que dropee items
        elemental.setRemoveWhenFarAway(false);

        // Efectos visuales según el elemento
        org.bukkit.Particle particle = org.bukkit.Particle.FLAME;
        if (elementName.equals("Aire")) particle = org.bukkit.Particle.CLOUD;
        else if (elementName.equals("Agua")) particle = org.bukkit.Particle.DRIPPING_WATER;
        else if (elementName.equals("Tierra")) particle = org.bukkit.Particle.BLOCK;

        final org.bukkit.Particle finalParticle = particle;
        final org.bukkit.entity.LivingEntity finalElemental = elemental;

        // Tarea de IA del elemental - seguir y atacar
        org.bukkit.scheduler.BukkitTask aiTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (finalElemental.isDead() || !player.isOnline()) {
                return;
            }

            // Partículas de ambiente
            finalElemental.getWorld().spawnParticle(finalParticle, finalElemental.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.02);

            // Buscar enemigos cercanos y atacar
            if (finalElemental instanceof org.bukkit.entity.Mob) {
                org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) finalElemental;

                org.bukkit.entity.LivingEntity priorityTarget = null;

                // PRIORIDAD 1: Defender al dueño - atacar a quien lo atacó
                if (player.hasMetadata("rpg-summon-last-attacker")) {
                    String attackerUUID = player.getMetadata("rpg-summon-last-attacker").get(0).asString();
                    for (org.bukkit.entity.Entity entity : finalElemental.getNearbyEntities(20, 5, 20)) {
                        if (entity.getUniqueId().toString().equals(attackerUUID) && entity instanceof org.bukkit.entity.LivingEntity) {
                            priorityTarget = (org.bukkit.entity.LivingEntity) entity;
                            break;
                        }
                    }
                }

                // PRIORIDAD 2: Atacar al mismo objetivo que el dueño
                if (priorityTarget == null && player.hasMetadata("rpg-summon-attack-target")) {
                    String targetUUID = player.getMetadata("rpg-summon-attack-target").get(0).asString();
                    for (org.bukkit.entity.Entity entity : finalElemental.getNearbyEntities(20, 5, 20)) {
                        if (entity.getUniqueId().toString().equals(targetUUID) && entity instanceof org.bukkit.entity.LivingEntity) {
                            priorityTarget = (org.bukkit.entity.LivingEntity) entity;
                            break;
                        }
                    }
                }

                // Si hay objetivo prioritario, atacarlo
                if (priorityTarget != null && !priorityTarget.isDead()) {
                    mob.setTarget(priorityTarget);
                }
                // Si no tiene objetivo o el actual está muerto, buscar uno cercano
                else if (mob.getTarget() == null || mob.getTarget().isDead()) {
                    org.bukkit.entity.LivingEntity nearestEnemy = null;
                    double nearestDist = 15;

                    for (org.bukkit.entity.Entity entity : finalElemental.getNearbyEntities(15, 5, 15)) {
                        if (entity instanceof org.bukkit.entity.LivingEntity && entity != player && entity != finalElemental) {
                            // No atacar a otros summons del mismo dueño
                            if (entity.hasMetadata("rpg-summon-owner")) {
                                String ownerUUID = entity.getMetadata("rpg-summon-owner").get(0).asString();
                                if (ownerUUID.equals(player.getUniqueId().toString())) {
                                    continue;
                                }
                            }

                            // No atacar jugadores
                            if (entity instanceof Player) {
                                continue;
                            }

                            double dist = entity.getLocation().distance(finalElemental.getLocation());
                            if (dist < nearestDist) {
                                nearestDist = dist;
                                nearestEnemy = (org.bukkit.entity.LivingEntity) entity;
                            }
                        }
                    }

                    if (nearestEnemy != null) {
                        mob.setTarget(nearestEnemy);
                    }
                }

                // Teletransportar si está muy lejos del dueño
                if (finalElemental.getLocation().distance(player.getLocation()) > 25) {
                    finalElemental.teleport(player.getLocation().add(2, 0, 2));
                }
            }
        }, 10L, 10L);

        // Programar eliminación del elemental
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            aiTask.cancel();
            if (!finalElemental.isDead()) {
                finalElemental.getWorld().spawnParticle(finalParticle, finalElemental.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                finalElemental.remove();
                if (player.isOnline()) {
                    player.sendMessage("§6🔮 Tu Elemental de " + elementName + " ha desaparecido.");
                }
            }
        }, summonDuration * 20L);

        spawnAbilityParticles(player, ability);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 1.0f);
        player.sendMessage("§6🔮 ¡Elemental de " + elementName + " invocado! §7(Daño: §e" + String.format("%.1f", summonDamage) + "§7, Duración: " + summonDuration + "s) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Obtiene la ubicación a donde mira el jugador hasta una distancia máxima
     */
    private org.bukkit.Location getTargetLocation(Player player, double maxRange) {
        org.bukkit.block.Block targetBlock = player.getTargetBlockExact((int) maxRange);
        if (targetBlock != null) {
            return targetBlock.getLocation().add(0.5, 1, 0.5);
        }
        // Si no hay bloque, usar la ubicación a maxRange en la dirección de la vista
        org.bukkit.util.Vector direction = player.getEyeLocation().getDirection().normalize();
        return player.getEyeLocation().add(direction.multiply(maxRange));
    }

    /**
     * Busca un objetivo en la línea de visión del jugador
     */
    private org.bukkit.entity.LivingEntity getTargetInSight(Player player, double range) {
        org.bukkit.util.Vector direction = player.getEyeLocation().getDirection().normalize();

        for (double d = 1; d <= range; d += 0.5) {
            org.bukkit.Location check = player.getEyeLocation().add(direction.clone().multiply(d));

            // Radio de detección más amplio (1.5 horizontal, 2 vertical para entidades altas)
            for (org.bukkit.entity.Entity entity : player.getWorld().getNearbyEntities(check, 1.5, 2, 1.5)) {
                if (entity instanceof org.bukkit.entity.LivingEntity && !entity.equals(player)) {
                    return (org.bukkit.entity.LivingEntity) entity;
                }
            }

            // Si hay un bloque sólido, parar
            if (check.getBlock().getType().isSolid()) {
                break;
            }
        }

        return null;
    }

    /**
     * Remueve un AttributeModifier por su NamespacedKey
     */
    private void removeModifierByKey(org.bukkit.attribute.AttributeInstance attr, org.bukkit.NamespacedKey key) {
        for (org.bukkit.attribute.AttributeModifier mod : attr.getModifiers()) {
            if (mod.getKey().equals(key)) {
                attr.removeModifier(mod);
                return;
            }
        }
    }

    /**
     * Encuentra una ubicación segura para teletransportarse
     */
    private org.bukkit.Location findSafeLocation(org.bukkit.Location target, org.bukkit.World world) {
        org.bukkit.Location safe = target.clone();

        // Obtener el bloque más alto en la coordenada X,Z del destino
        int highestY = world.getHighestBlockYAt(safe);

        // Si el destino está bajo el suelo, subir hasta la superficie
        if (safe.getY() < highestY) {
            safe.setY(highestY);
        }

        // Verificar si el destino está dentro de un bloque sólido
        // Si es así, buscar hacia arriba hasta encontrar espacio libre
        int maxIterations = 50; // Prevenir bucle infinito
        int iterations = 0;
        while (iterations < maxIterations) {
            org.bukkit.block.Block feetBlock = safe.getBlock();
            org.bukkit.block.Block headBlock = safe.clone().add(0, 1, 0).getBlock();

            // Verificar si hay espacio para el jugador (2 bloques de aire)
            if (!feetBlock.getType().isSolid() && !headBlock.getType().isSolid()) {
                // Encontramos espacio, ahora asegurar que hay suelo debajo
                org.bukkit.block.Block groundBlock = safe.clone().add(0, -1, 0).getBlock();
                if (groundBlock.getType().isSolid()) {
                    // Perfecto: hay suelo sólido y espacio para el jugador
                    break;
                } else {
                    // No hay suelo, bajar hasta encontrarlo
                    while (!safe.clone().add(0, -1, 0).getBlock().getType().isSolid() && safe.getY() > world.getMinHeight()) {
                        safe.add(0, -1, 0);
                    }
                    break;
                }
            }

            // Si el bloque de los pies es sólido, subir
            safe.add(0, 1, 0);
            iterations++;
        }

        // Asegurar que no estamos demasiado alto o bajo
        if (safe.getY() < world.getMinHeight() + 1) {
            safe.setY(world.getMinHeight() + 1);
        }
        if (safe.getY() > world.getMaxHeight() - 2) {
            safe.setY(world.getMaxHeight() - 2);
        }

        return safe;
    }

    // ==================== TRANSFORMACIONES DEL DRUIDA ====================

    /**
     * Ejecuta una transformación del Druida
     */
    private void executeTransformation(Player player, Ability ability) {
        TransformationManager transformManager = plugin.getTransformationManager();

        if (transformManager.transform(player, ability)) {
            spawnAbilityParticles(player, ability);
        }
    }

    // ==================== HABILIDADES DE FORMA ====================

    /**
     * Ejecuta un ataque cuerpo a cuerpo de forma animal
     */
    private void executeFormMeleeAttack(Player player, Ability ability, double damage, String message) {
        // Buscar entidad más cercana en el rango
        double range = ability.getRange() > 0 ? ability.getRange() : 3.0;
        LivingEntity target = findNearestTarget(player, range);

        if (target != null) {
            applyAbilityDamage(target, player, damage, ability);
            spawnAbilityParticles(player, ability);

            // Partículas en el objetivo
            target.getWorld().spawnParticle(
                Particle.DAMAGE_INDICATOR,
                target.getLocation().add(0, 1, 0),
                10, 0.3, 0.3, 0.3, 0.1
            );

            player.sendMessage(message + " §7(Daño: §e" + String.format("%.1f", damage) + "§7) (-§9" + ability.getManaCost() + " maná§7)");
        } else {
            player.sendMessage("§c✗ No hay enemigos cerca");
            // Devolver el maná (no se consumió)
            PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
            if (playerData != null) {
                try {
                    int manaCost = Integer.parseInt(ability.getManaCost());
                    playerData.setCurrentMana(Math.min(playerData.getCurrentMana() + manaCost, playerData.getMaxMana()));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    /**
     * Ejecuta el aullido de manada (buff de daño a aliados)
     */
    private void executePackHowl(Player player, Ability ability) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 10.0;

        // Buff al jugador
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.STRENGTH, 200, 1, false, true, true
        ));

        // Buff a jugadores cercanos
        int buffCount = 1;
        for (org.bukkit.entity.Entity nearby : player.getNearbyEntities(aoe, aoe, aoe)) {
            if (nearby instanceof Player && nearby != player) {
                Player ally = (Player) nearby;
                ally.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.STRENGTH, 200, 0, false, true, true
                ));
                ally.sendMessage("§a🐺 ¡El aullido de " + player.getName() + " te fortalece!");
                buffCount++;
            }
        }

        spawnAbilityParticles(player, ability);
        player.sendMessage("§a🐺 ¡Aullido de Manada! §7(" + buffCount + " aliados fortalecidos) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Ejecuta golpe pesado (aturde al enemigo)
     */
    private void executeHeavyStrike(Player player, Ability ability, double damage) {
        double range = ability.getRange() > 0 ? ability.getRange() : 3.0;
        LivingEntity target = findNearestTarget(player, range);

        if (target != null) {
            applyAbilityDamage(target, player, damage * 1.5, ability);
            // Efecto de aturdimiento (lentitud extrema)
            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 5, false, true, true
            ));
            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.BLINDNESS, 20, 0, false, true, true
            ));

            spawnAbilityParticles(player, ability);
            player.sendMessage("§6💪 ¡Golpe Pesado! §7(Daño: §e" + String.format("%.1f", damage * 1.5) + "§7 + Aturdimiento) (-§9" + ability.getManaCost() + " maná§7)");
        } else {
            player.sendMessage("§c✗ No hay enemigos cerca");
        }
    }

    /**
     * Ejecuta rabia de oso (buff de daño y resistencia)
     */
    private void executeBearRage(Player player, Ability ability) {
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.STRENGTH, 300, 2, false, true, true
        ));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.RESISTANCE, 300, 1, false, true, true
        ));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SPEED, 300, 0, false, true, true
        ));

        spawnAbilityParticles(player, ability);
        player.sendMessage("§c🐻 ¡RABIA DE OSO! §7(+Fuerza +Resistencia +Velocidad 15s) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Ejecuta disparo de telaraña
     */
    private void executeWebShot(Player player, Ability ability) {
        // Buscar objetivo
        double range = ability.getRange() > 0 ? ability.getRange() : 10.0;
        LivingEntity target = findNearestTarget(player, range);

        if (target != null) {
            // Ralentizar al objetivo
            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOWNESS, 100, 3, false, true, true
            ));

            // Partículas de telaraña
            target.getWorld().spawnParticle(
                Particle.CLOUD,
                target.getLocation().add(0, 1, 0),
                30, 0.5, 0.5, 0.5, 0.05
            );

            spawnAbilityParticles(player, ability);
            player.sendMessage("§f🕸 ¡Telaraña! §7(Enemigo ralentizado 5s) (-§9" + ability.getManaCost() + " maná§7)");
        } else {
            player.sendMessage("§c✗ No hay enemigos en rango");
        }
    }

    /**
     * Ejecuta nube de veneno
     */
    private void executePoisonCloud(Player player, Ability ability, double damage) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 5.0;

        int poisoned = 0;
        for (org.bukkit.entity.Entity nearby : player.getNearbyEntities(aoe, aoe, aoe)) {
            if (nearby instanceof LivingEntity && !(nearby instanceof Player)) {
                LivingEntity target = (LivingEntity) nearby;
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.POISON, 100, 1, false, true, true
                ));
                applyAbilityDamage(target, player, damage * 0.5, ability);
                poisoned++;
            }
        }

        spawnAbilityParticles(player, ability);
        player.sendMessage("§5☠ ¡Nube de Veneno! §7(" + poisoned + " enemigos envenenados) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Ejecuta evasión de zorro
     */
    private void executeFoxDodge(Player player, Ability ability) {
        // Hacer invisible brevemente y aumentar velocidad
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.INVISIBILITY, 40, 0, false, false, false
        ));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SPEED, 60, 3, false, true, true
        ));

        // Teletransportar ligeramente hacia atrás
        org.bukkit.util.Vector back = player.getLocation().getDirection().multiply(-3);
        org.bukkit.Location newLoc = player.getLocation().add(back);
        newLoc.setY(player.getLocation().getY());
        player.teleport(newLoc);

        spawnAbilityParticles(player, ability);
        player.sendMessage("§e🦊 ¡Evasión! §7(Esquivaste hacia atrás) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Ejecuta grito sónico del warden
     */
    private void executeSonicScream(Player player, Ability ability, double damage) {
        double range = ability.getRange() > 0 ? ability.getRange() : 15.0;
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 5.0;

        org.bukkit.util.Vector direction = player.getLocation().getDirection();
        org.bukkit.Location targetLoc = player.getLocation().add(direction.multiply(range / 2));

        int hit = 0;
        for (org.bukkit.entity.Entity nearby : player.getWorld().getNearbyEntities(targetLoc, aoe, aoe, aoe)) {
            if (nearby instanceof LivingEntity && nearby != player) {
                LivingEntity target = (LivingEntity) nearby;
                applyAbilityDamage(target, player, damage * 2, ability);
                // Efecto de knockback
                org.bukkit.util.Vector knockback = target.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize().multiply(2);
                target.setVelocity(knockback);
                hit++;
            }
        }

        // Partículas de sonic boom
        player.getWorld().spawnParticle(
            Particle.SONIC_BOOM,
            player.getLocation().add(0, 1.5, 0),
            1, 0, 0, 0, 0
        );

        spawnAbilityParticles(player, ability);
        player.sendMessage("§5💀 ¡GRITO SÓNICO! §7(" + hit + " enemigos devastados, Daño: §e" + String.format("%.1f", damage * 2) + "§7) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Ejecuta onda de choque
     */
    private void executeShockwave(Player player, Ability ability, double damage) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 8.0;

        int hit = 0;
        for (org.bukkit.entity.Entity nearby : player.getNearbyEntities(aoe, aoe, aoe)) {
            if (nearby instanceof LivingEntity && !(nearby instanceof Player)) {
                LivingEntity target = (LivingEntity) nearby;
                applyAbilityDamage(target, player, damage * 1.5, ability);

                // Knockback desde el jugador
                org.bukkit.util.Vector knockback = target.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize().multiply(1.5).setY(0.5);
                target.setVelocity(knockback);
                hit++;
            }
        }

        // Partículas de explosión en el suelo
        player.getWorld().spawnParticle(
            Particle.EXPLOSION,
            player.getLocation(),
            5, 2, 0.5, 2, 0
        );

        spawnAbilityParticles(player, ability);
        player.sendMessage("§6💥 ¡Onda de Choque! §7(" + hit + " enemigos golpeados) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Ejecuta sentido de vibración (revela enemigos)
     */
    private void executeVibrationSense(Player player, Ability ability) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 20.0;

        int detected = 0;
        for (org.bukkit.entity.Entity nearby : player.getNearbyEntities(aoe, aoe, aoe)) {
            if (nearby instanceof LivingEntity && !(nearby instanceof Player)) {
                LivingEntity target = (LivingEntity) nearby;
                // Hacer brillar al enemigo
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.GLOWING, 200, 0, false, false, true
                ));
                detected++;
            }
        }

        spawnAbilityParticles(player, ability);
        player.sendMessage("§b👁 ¡Sentido de Vibración! §7(" + detected + " enemigos detectados, brillan 10s) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Encuentra el objetivo más cercano en rango
     */
    private LivingEntity findNearestTarget(Player player, double range) {
        LivingEntity nearest = null;
        double nearestDist = range + 1;

        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                LivingEntity living = (LivingEntity) entity;
                // Ignorar entidades de transformación
                if (living.hasMetadata("rpg-transform-entity")) continue;
                // Ignorar mascotas RPG
                if (living.hasMetadata("rpg-pet")) continue;

                double dist = player.getLocation().distance(living.getLocation());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = living;
                }
            }
        }

        return nearest;
    }

    // ==================== EVENTOS DE CONEXIÓN ====================

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Inicializar estado
        abilityModeEnabled.put(uuid, false);

        // Cargar ability slots desde la base de datos
        Map<Integer, String> loadedSlots = plugin.getDatabaseManager().loadAbilitySlots(uuid);
        if (!loadedSlots.isEmpty()) {
            abilitySlots.put(uuid, loadedSlots);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Guardar ability slots antes de desconectar
        Map<Integer, String> slots = abilitySlots.get(uuid);
        if (slots != null && !slots.isEmpty()) {
            plugin.getDatabaseManager().saveAbilitySlots(uuid, slots);
        }

        // Cancelar tareas pendientes
        BukkitTask task = pendingFTasks.remove(uuid);
        if (task != null) task.cancel();
        fPressTimestamps.remove(uuid);

        // Cancelar casteo en progreso
        BukkitTask castTask = castingTasks.remove(uuid);
        if (castTask != null) castTask.cancel();
        castingStartLocations.remove(uuid);

        // Ocultar boss bar
        hideAbilityBar(event.getPlayer());

        // Limpiar datos
        abilityModeEnabled.remove(uuid);
        abilitySlots.remove(uuid);
        playerBossBars.remove(uuid);
        previousSlot.remove(uuid);
        abilityCooldowns.remove(uuid);

        // Limpiar transformaciones activas
        if (plugin.getTransformationManager() != null) {
            plugin.getTransformationManager().cleanupPlayer(uuid);
        }
    }

    // ==================== API PÚBLICA ====================

    // Referencia estática al plugin para poder guardar desde métodos estáticos
    private static TunamaRPG pluginInstance;

    /**
     * Asigna una habilidad a un slot de la barra de habilidades y guarda a DB
     */
    public static void setAbilitySlot(UUID uuid, int slot, String abilityId) {
        if (slot < 0 || slot > 8) return;
        abilitySlots.computeIfAbsent(uuid, k -> new HashMap<>()).put(slot, abilityId);

        // Guardar a base de datos
        if (pluginInstance != null) {
            Map<Integer, String> slots = abilitySlots.get(uuid);
            if (slots != null) {
                pluginInstance.getDatabaseManager().saveAbilitySlots(uuid, slots);
            }
        }
    }

    /**
     * Establece la referencia al plugin (llamar desde el constructor o TunamaRPG)
     */
    public static void setPluginInstance(TunamaRPG plugin) {
        pluginInstance = plugin;
    }

    /**
     * Obtiene la habilidad asignada a un slot
     */
    public static String getAbilitySlot(UUID uuid, int slot) {
        Map<Integer, String> slots = abilitySlots.get(uuid);
        return slots != null ? slots.get(slot) : null;
    }

    /**
     * Obtiene todos los slots de habilidades de un jugador
     */
    public static Map<Integer, String> getAbilitySlots(UUID uuid) {
        return abilitySlots.getOrDefault(uuid, new HashMap<>());
    }

    /**
     * Limpia los slots de habilidades (al cambiar de personaje)
     */
    public static void clearAbilitySlots(UUID uuid) {
        abilitySlots.remove(uuid);
    }

    /**
     * Recarga los slots de habilidades desde la base de datos (al cambiar de personaje)
     */
    public static void reloadAbilitySlots(UUID uuid) {
        abilitySlots.remove(uuid);
        if (pluginInstance != null) {
            Map<Integer, String> loadedSlots = pluginInstance.getDatabaseManager().loadAbilitySlots(uuid);
            if (!loadedSlots.isEmpty()) {
                abilitySlots.put(uuid, loadedSlots);
            }
        }
    }

    /**
     * Verifica si un jugador está en modo habilidades
     */
    public static boolean isInAbilityMode(UUID uuid) {
        return abilityModeEnabled.getOrDefault(uuid, false);
    }

    /**
     * Desactiva el modo habilidades para un jugador (uso externo)
     */
    public static void forceDisableAbilityMode(Player player) {
        UUID uuid = player.getUniqueId();
        if (abilityModeEnabled.getOrDefault(uuid, false)) {
            abilityModeEnabled.put(uuid, false);
            BossBar bossBar = playerBossBars.get(uuid);
            if (bossBar != null) {
                player.hideBossBar(bossBar);
            }
        }
    }

    /**
     * Activa el modo habilidades para un jugador transformado (uso externo)
     * No guarda items ni intercambia slots, solo activa el modo
     */
    public static void enableTransformAbilityMode(UUID uuid) {
        abilityModeEnabled.put(uuid, true);
    }

    // ==================== HABILIDADES DEL MAGO ====================

    /**
     * Bola de Fuego - Unitarget, si mata al objetivo hace erupción AoE
     */
    private void executeFireball(Player player, Ability ability, double damage, PlayerData playerData) {
        org.bukkit.entity.LivingEntity target = getTargetInSight(player, 15);

        if (target == null) {
            player.sendMessage("§c✗ No hay objetivo a la vista");
            playerData.regenMana(Integer.parseInt(ability.getManaCost()));
            return;
        }

        // Efectos visuales del proyectil
        org.bukkit.Location startLoc = player.getEyeLocation();
        org.bukkit.Location targetLoc = target.getEyeLocation();
        org.bukkit.util.Vector direction = targetLoc.toVector().subtract(startLoc.toVector()).normalize();
        double distance = startLoc.distance(targetLoc);

        // Animación de la bola de fuego viajando
        for (int i = 0; i < (int)(distance * 2); i++) {
            final int step = i;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                org.bukkit.Location particleLoc = startLoc.clone().add(direction.clone().multiply(step * 0.5));
                player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, particleLoc, 8, 0.1, 0.1, 0.1, 0.02);
                player.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, particleLoc, 3, 0.05, 0.05, 0.05, 0.01);
            }, (long)(step));
        }

        // Aplicar daño después de que llegue el proyectil
        long travelTicks = (long)(distance * 2);
        final double finalDamage = damage;
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (target.isDead() || !target.isValid()) return;

            applyAbilityDamage(target, player, finalDamage, ability);
            target.setFireTicks(60);
            player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, target.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
            player.getWorld().playSound(target.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.2f);

            // Verificar si el objetivo murió para la erupción
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (target.isDead() || target.getHealth() <= 0) {
                    // Erupción de área al morir
                    double explosionScaling = ability.getDoubleProperty("explosion-damage-scaling", 1.1);
                    int intelligence = playerData.getStats().getIntelligence();
                    double explosionDamage = intelligence * explosionScaling;

                    // Aplicar pasiva Elemento Antiguo
                    Ability elementoAntiguo = plugin.getAbilityManager().getAbility("elemento-antiguo");
                    if (elementoAntiguo != null && playerData.getLevel() >= elementoAntiguo.getRequiredLevel()) {
                        String pClass = playerData.getPlayerClass();
                        if (pClass != null && pClass.equalsIgnoreCase("mago")) {
                            explosionDamage *= 1.3;
                        }
                    }

                    double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 3.0;
                    org.bukkit.Location deathLoc = target.getLocation();

                    player.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, deathLoc, 3, 0.5, 0.5, 0.5, 0);
                    player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, deathLoc, 60, aoe * 0.5, 1, aoe * 0.5, 0.1);
                    player.getWorld().spawnParticle(org.bukkit.Particle.LAVA, deathLoc, 20, aoe * 0.4, 0.5, aoe * 0.4, 0);
                    player.getWorld().playSound(deathLoc, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);

                    final double finalExplosionDamage = explosionDamage;
                    for (org.bukkit.entity.Entity entity : deathLoc.getWorld().getNearbyEntities(deathLoc, aoe, 2, aoe)) {
                        if (entity instanceof org.bukkit.entity.LivingEntity && !entity.equals(player) && !entity.equals(target)) {
                            org.bukkit.entity.LivingEntity victim = (org.bukkit.entity.LivingEntity) entity;
                            applyAbilityDamage(victim, player, finalExplosionDamage, ability);
                            victim.setFireTicks(60);
                        }
                    }

                    player.sendMessage("§6§l✦ ¡ERUPCION! §eLa bola de fuego causó una explosión al matar al objetivo §7(Daño AoE: §e" + String.format("%.1f", finalExplosionDamage) + "§7)");
                }
            }, 1L);
        }, travelTicks);

        player.sendMessage("§c🔥 ¡Bola de Fuego! §7(Daño: §e" + String.format("%.1f", damage) + "§7) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Pica de Hielo - Unitarget, 3 stacks en 30s = congelación 2s
     */
    private void executeIceSpike(Player player, Ability ability, double damage) {
        org.bukkit.entity.LivingEntity target = getTargetInSight(player, 15);

        if (target == null) {
            player.sendMessage("§c✗ No hay objetivo a la vista");
            try {
                int manaCost = Integer.parseInt(ability.getManaCost());
                PlayerData pd = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
                if (pd != null) pd.regenMana(manaCost);
            } catch (Exception ignored) {}
            return;
        }

        // Animación del proyectil de hielo
        org.bukkit.Location startLoc = player.getEyeLocation();
        org.bukkit.Location targetLoc = target.getEyeLocation();
        org.bukkit.util.Vector direction = targetLoc.toVector().subtract(startLoc.toVector()).normalize();
        double distance = startLoc.distance(targetLoc);

        for (int i = 0; i < (int)(distance * 2); i++) {
            final int step = i;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                org.bukkit.Location particleLoc = startLoc.clone().add(direction.clone().multiply(step * 0.5));
                player.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, particleLoc, 6, 0.1, 0.1, 0.1, 0.01);
                player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTED_HIT, particleLoc, 3, 0.05, 0.05, 0.05, 0.01);
            }, (long)(step));
        }

        long travelTicks = (long)(distance * 2);
        final double finalDamage = damage;
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (target.isDead() || !target.isValid()) return;

            applyAbilityDamage(target, player, finalDamage, ability);
            player.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.05);
            player.getWorld().playSound(target.getLocation(), org.bukkit.Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);

            // Sistema de stacks de hielo
            int freezeWindow = ability.getIntProperty("freeze-window", 30);
            int freezeStacks = ability.getIntProperty("freeze-stacks", 3);
            int freezeDuration = ability.getIntProperty("freeze-duration", 2);

            String metaKey = "rpg-ice-stacks";
            String metaTimeKey = "rpg-ice-stacks-time";
            int currentStacks = 0;

            if (target.hasMetadata(metaKey) && target.hasMetadata(metaTimeKey)) {
                long lastTime = target.getMetadata(metaTimeKey).get(0).asLong();
                if (System.currentTimeMillis() - lastTime < freezeWindow * 1000L) {
                    currentStacks = target.getMetadata(metaKey).get(0).asInt();
                }
            }

            currentStacks++;
            target.setMetadata(metaKey, new FixedMetadataValue(plugin, currentStacks));
            target.setMetadata(metaTimeKey, new FixedMetadataValue(plugin, System.currentTimeMillis()));

            if (currentStacks >= freezeStacks) {
                // Congelar al objetivo
                target.setFreezeTicks(freezeDuration * 20);
                if (target instanceof LivingEntity) {
                    ((LivingEntity) target).addPotionEffect(
                        new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, freezeDuration * 20, 127, false, false));
                    ((LivingEntity) target).addPotionEffect(
                        new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.MINING_FATIGUE, freezeDuration * 20, 127, false, false));
                }
                target.removeMetadata(metaKey, plugin);
                target.removeMetadata(metaTimeKey, plugin);

                player.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.05);
                player.getWorld().playSound(target.getLocation(), org.bukkit.Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
                player.sendMessage("§b❄ ¡El objetivo se ha congelado durante " + freezeDuration + " segundos!");
            } else {
                player.sendMessage("§b❄ Pica de Hielo §7[" + currentStacks + "/" + freezeStacks + " cargas]");
            }
        }, travelTicks);

        player.sendMessage("§b❄ ¡Pica de Hielo! §7(Daño: §e" + String.format("%.1f", damage) + "§7) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Implosión Arcana - AoE 2 bloques desde el jugador con knockback
     */
    private void executeArcaneImplosion(Player player, Ability ability, double damage) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 2.0;
        org.bukkit.Location center = player.getLocation();

        // Efectos visuales
        center.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTED_HIT, center.clone().add(0, 1, 0), 60, aoe * 0.5, 1, aoe * 0.5, 0.3);
        center.getWorld().spawnParticle(org.bukkit.Particle.REVERSE_PORTAL, center.clone().add(0, 1, 0), 40, aoe * 0.3, 0.5, aoe * 0.3, 0.1);
        center.getWorld().playSound(center, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);

        int hits = 0;
        for (org.bukkit.entity.Entity entity : center.getWorld().getNearbyEntities(center, aoe, 2, aoe)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && !entity.equals(player)) {
                // No golpear summons propios
                if (entity.hasMetadata("rpg-summon-owner")) {
                    String ownerUUID = entity.getMetadata("rpg-summon-owner").get(0).asString();
                    if (player.getUniqueId().toString().equals(ownerUUID)) continue;
                }
                if (entity.hasMetadata("rpg-pet-owner")) {
                    String ownerUUID = entity.getMetadata("rpg-pet-owner").get(0).asString();
                    if (player.getUniqueId().toString().equals(ownerUUID)) continue;
                }

                org.bukkit.entity.LivingEntity victim = (org.bukkit.entity.LivingEntity) entity;
                applyAbilityDamage(victim, player, damage, ability);

                // Knockback: empujar hacia afuera
                org.bukkit.util.Vector knockback = victim.getLocation().toVector()
                    .subtract(center.toVector()).normalize().multiply(1.2).setY(0.4);
                victim.setVelocity(knockback);
                hits++;
            }
        }

        player.sendMessage("§d⚡ ¡Implosión Arcana! §7(Daño: §e" + String.format("%.1f", damage) + "§7, Golpeados: §f" + hits + "§7) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Llamarada - Cono de fuego 2 bloques delante del jugador
     */
    private void executeFlare(Player player, Ability ability, double damage) {
        org.bukkit.Location origin = player.getEyeLocation();
        org.bukkit.util.Vector direction = origin.getDirection().normalize();
        double maxRange = ability.getRange() > 0 ? ability.getRange() : 2.0;
        double coneAngle = 45.0; // Grados del cono

        // Efectos visuales del cono de fuego
        for (double d = 0.5; d <= maxRange; d += 0.3) {
            double spread = d * 0.5;
            org.bukkit.Location particleLoc = origin.clone().add(direction.clone().multiply(d));
            player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, particleLoc, 15, spread, 0.2, spread, 0.05);
            player.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, particleLoc, 5, spread * 0.5, 0.1, spread * 0.5, 0.02);
        }
        player.getWorld().playSound(origin, org.bukkit.Sound.ITEM_FIRECHARGE_USE, 1.2f, 0.8f);

        int hits = 0;
        for (org.bukkit.entity.Entity entity : player.getWorld().getNearbyEntities(origin, maxRange + 1, 2, maxRange + 1)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && !entity.equals(player)) {
                org.bukkit.entity.LivingEntity victim = (org.bukkit.entity.LivingEntity) entity;

                // No golpear summons propios
                if (entity.hasMetadata("rpg-summon-owner")) {
                    String ownerUUID = entity.getMetadata("rpg-summon-owner").get(0).asString();
                    if (player.getUniqueId().toString().equals(ownerUUID)) continue;
                }
                if (entity.hasMetadata("rpg-pet-owner")) {
                    String ownerUUID = entity.getMetadata("rpg-pet-owner").get(0).asString();
                    if (player.getUniqueId().toString().equals(ownerUUID)) continue;
                }

                // Verificar que está dentro del cono
                org.bukkit.util.Vector toEntity = victim.getLocation().toVector()
                    .subtract(player.getLocation().toVector());
                double dist = toEntity.length();
                if (dist > maxRange + 1) continue;
                if (dist > 0) {
                    double angle = Math.toDegrees(toEntity.normalize().angle(direction));
                    if (angle <= coneAngle) {
                        applyAbilityDamage(victim, player, damage, ability);
                        victim.setFireTicks(60);
                        hits++;
                    }
                }
            }
        }

        player.sendMessage("§6🔥 ¡Llamarada! §7(Daño: §e" + String.format("%.1f", damage) + "§7, Golpeados: §f" + hits + "§7) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Ventisca - AoE 4 bloques, 5 oleadas, congela si golpea 3+ veces
     */
    private void executeBlizzard(Player player, Ability ability, double damage) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 4.0;
        int waveCount = ability.getIntProperty("wave-count", 5);
        int freezeThreshold = ability.getIntProperty("freeze-threshold", 3);
        int freezeDuration = ability.getIntProperty("freeze-duration", 6);

        org.bukkit.Location center = getTargetLocation(player, 10);

        // Map para contar hits por entidad
        java.util.Map<UUID, Integer> hitCounts = new java.util.concurrent.ConcurrentHashMap<>();

        for (int wave = 0; wave < waveCount; wave++) {
            final int currentWave = wave;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Efectos visuales
                center.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, center, 80, aoe * 0.6, 1.5, aoe * 0.6, 0.05);
                center.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, center, 30, aoe * 0.5, 1, aoe * 0.5, 0.02);

                if (currentWave == 0) {
                    center.getWorld().playSound(center, org.bukkit.Sound.WEATHER_RAIN, 1.5f, 0.5f);
                }

                // Dañar enemigos en el área
                for (org.bukkit.entity.Entity entity : center.getWorld().getNearbyEntities(center, aoe, 3, aoe)) {
                    if (entity instanceof org.bukkit.entity.LivingEntity && !entity.equals(player)) {
                        // No golpear summons propios
                        if (entity.hasMetadata("rpg-summon-owner")) {
                            String ownerUUID = entity.getMetadata("rpg-summon-owner").get(0).asString();
                            if (player.getUniqueId().toString().equals(ownerUUID)) continue;
                        }
                        if (entity.hasMetadata("rpg-pet-owner")) {
                            String ownerUUID = entity.getMetadata("rpg-pet-owner").get(0).asString();
                            if (player.getUniqueId().toString().equals(ownerUUID)) continue;
                        }

                        org.bukkit.entity.LivingEntity victim = (org.bukkit.entity.LivingEntity) entity;
                        applyAbilityDamage(victim, player, damage, ability);
                        hitCounts.merge(victim.getUniqueId(), 1, Integer::sum);
                    }
                }

                // En la última oleada, congelar a los que fueron golpeados 3+ veces
                if (currentWave == waveCount - 1) {
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (java.util.Map.Entry<UUID, Integer> entry : hitCounts.entrySet()) {
                            if (entry.getValue() >= freezeThreshold) {
                                org.bukkit.entity.Entity frozenEntity = org.bukkit.Bukkit.getEntity(entry.getKey());
                                if (frozenEntity instanceof org.bukkit.entity.LivingEntity && !frozenEntity.isDead()) {
                                    org.bukkit.entity.LivingEntity frozen = (org.bukkit.entity.LivingEntity) frozenEntity;
                                    frozen.setFreezeTicks(freezeDuration * 20);
                                    frozen.addPotionEffect(
                                        new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, freezeDuration * 20, 127, false, false));
                                    frozen.addPotionEffect(
                                        new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.MINING_FATIGUE, freezeDuration * 20, 127, false, false));

                                    frozen.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, frozen.getLocation().add(0, 1, 0), 40, 0.3, 0.5, 0.3, 0.02);
                                    frozen.getWorld().playSound(frozen.getLocation(), org.bukkit.Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                                }
                            }
                        }
                        int frozenCount = (int) hitCounts.values().stream().filter(c -> c >= freezeThreshold).count();
                        if (frozenCount > 0) {
                            player.sendMessage("§b❄ ¡" + frozenCount + " enemigo(s) congelado(s) por " + freezeDuration + " segundos!");
                        }
                    }, 5L);
                }
            }, wave * 15L); // Cada oleada cada 0.75 segundos
        }

        player.sendMessage("§b❄ ¡Ventisca! §7(Daño: §e" + String.format("%.1f", damage) + "§7/oleada x" + waveCount + ", Área: " + (int)aoe + ") (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Sifón de Maná - Recupera 60% del maná máximo
     */
    private void executeManaSiphon(Player player, Ability ability, PlayerData playerData) {
        int restorePercent = ability.getIntProperty("mana-restore-percent", 60);
        int maxMana = playerData.getMaxMana();
        int restoreAmount = (int)(maxMana * (restorePercent / 100.0));

        playerData.regenMana(restoreAmount);

        // Efectos visuales
        player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANT, player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.5);
        player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.1);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f, 1.2f);

        player.sendMessage("§9✦ ¡Sifón de Maná! §7Recuperaste §9" + restoreAmount + " maná §7(" + restorePercent + "% del máximo)");
        player.sendMessage("§7Maná actual: §9" + playerData.getCurrentMana() + "/" + maxMana);
    }

    /**
     * Salto Dimensional - Blink hacia adelante hasta 10 bloques
     */
    private void executeDimensionalLeap(Player player, Ability ability) {
        double maxRange = ability.getRange() > 0 ? ability.getRange() : 10.0;
        org.bukkit.Location start = player.getLocation();
        org.bukkit.util.Vector direction = start.getDirection().clone();
        direction.setY(0).normalize();

        // Buscar la posición más lejana sin bloque sólido
        org.bukkit.Location destination = start.clone();
        for (double d = 1; d <= maxRange; d += 0.5) {
            org.bukkit.Location check = start.clone().add(direction.clone().multiply(d));
            check.setY(start.getY());

            // Verificar que no hay bloque sólido a la altura del jugador
            org.bukkit.block.Block feetBlock = check.getBlock();
            org.bukkit.block.Block headBlock = check.clone().add(0, 1, 0).getBlock();
            org.bukkit.block.Block groundBlock = check.clone().add(0, -1, 0).getBlock();

            if (feetBlock.getType().isSolid() || headBlock.getType().isSolid()) {
                break;
            }

            // Si hay suelo debajo, es una posición válida
            if (groundBlock.getType().isSolid()) {
                destination = check.clone();
                destination.setYaw(start.getYaw());
                destination.setPitch(start.getPitch());
            }
        }

        // Efectos en posición inicial
        start.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, start.clone().add(0, 1, 0), 50, 0.3, 0.5, 0.3, 0.5);
        start.getWorld().spawnParticle(org.bukkit.Particle.REVERSE_PORTAL, start.clone().add(0, 1, 0), 30, 0.2, 0.3, 0.2, 0.3);
        start.getWorld().playSound(start, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);

        // Teletransportar
        player.teleport(destination);

        // Efectos en posición final
        destination.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, destination.clone().add(0, 1, 0), 50, 0.3, 0.5, 0.3, 0.5);
        destination.getWorld().spawnParticle(org.bukkit.Particle.REVERSE_PORTAL, destination.clone().add(0, 1, 0), 30, 0.2, 0.3, 0.2, 0.3);

        double actualDistance = start.distance(destination);
        player.sendMessage("§5✦ ¡Salto Dimensional! §7(Distancia: §f" + String.format("%.1f", actualDistance) + " bloques§7) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Desactiva el modo habilidades para un jugador transformado (uso externo)
     * Solo desactiva el flag, no restaura items
     */
    public static void disableTransformAbilityMode(UUID uuid) {
        abilityModeEnabled.put(uuid, false);
    }

    // ==================== HABILIDADES DEL MONJE ====================

    /**
     * Golpe de Chi - Golpe melee rápido, daño fuerza x0.2 + agilidad x0.3
     */
    private void executeChiStrike(Player player, Ability ability, double damage) {
        org.bukkit.entity.LivingEntity target = getTargetInSight(player, ability.getRange());

        if (target == null) {
            player.sendMessage("§c✗ No hay objetivo en rango.");
            return;
        }

        // Efectos visuales de chi
        target.getWorld().spawnParticle(org.bukkit.Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.2);
        target.getWorld().spawnParticle(org.bukkit.Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.4f);

        applyAbilityDamage(target, player, damage, ability);
        spawnAbilityParticles(player, ability);
        player.sendMessage("§e👊 ¡Golpe de Chi! §7(Daño: §e" + String.format("%.1f", damage) + "§7) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Flujo de Chi - Rayo de energía a distancia (máx. 4 bloques), estático
     */
    private void executeChiFlow(Player player, Ability ability, double damage) {
        double range = ability.getRange() > 0 ? ability.getRange() : 4.0;
        org.bukkit.Location start = player.getEyeLocation();
        org.bukkit.util.Vector direction = start.getDirection().normalize();

        org.bukkit.entity.LivingEntity hit = null;

        // Raycast bloque a bloque hasta 4 casillas
        for (double d = 0.5; d <= range; d += 0.25) {
            org.bukkit.Location point = start.clone().add(direction.clone().multiply(d));

            // Partícula visual del rayo
            point.getWorld().spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, point, 1, 0.0, 0.0, 0.0, 0.0);

            if (point.getBlock().getType().isSolid()) break;

            for (org.bukkit.entity.Entity entity : point.getWorld().getNearbyEntities(point, 0.6, 0.6, 0.6)) {
                if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                    hit = (org.bukkit.entity.LivingEntity) entity;
                    break;
                }
            }
            if (hit != null) break;
        }

        if (hit == null) {
            player.sendMessage("§c✗ No hay objetivo en rango (máx. 4 casillas).");
            return;
        }

        applyAbilityDamage(hit, player, damage, ability);
        hit.getWorld().spawnParticle(org.bukkit.Particle.FLASH, hit.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.6f);
        spawnAbilityParticles(player, ability);
        player.sendMessage("§e✦ ¡Flujo de Chi! §7(Daño: §e" + String.format("%.1f", damage) + "§7) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Meditación - Canalizada: +5% vida/s, -10% maná/s. Se interrumpe al moverse.
     */
    private void executeMeditation(Player player, Ability ability, PlayerData playerData) {
        // Evitar doble meditación
        if (player.hasMetadata("rpg-meditacion")) {
            // Cancelar meditación activa
            player.removeMetadata("rpg-meditacion", plugin);
            player.sendMessage("§7☯ Meditación interrumpida.");
            return;
        }

        int healPercent = ability.getIntProperty("heal-percent-per-second", 5);
        int manaPercent = ability.getIntProperty("mana-drain-percent-per-second", 10);
        int maxDuration = ability.getIntProperty("max-duration", 30);

        player.setMetadata("rpg-meditacion", new FixedMetadataValue(plugin, true));
        org.bukkit.Location startLoc = player.getLocation().clone();

        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 0.5f);
        player.sendMessage("§e☯ ¡Meditación iniciada! §7(+" + healPercent + "% vida/s, -" + manaPercent + "% maná/s)");

        final int[] ticksLeft = {maxDuration};
        org.bukkit.scheduler.BukkitTask task = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !player.hasMetadata("rpg-meditacion") || ticksLeft[0] <= 0) return;

            // Verificar si el jugador se movió
            if (player.getLocation().distanceSquared(startLoc) > 0.1) {
                player.removeMetadata("rpg-meditacion", plugin);
                player.sendMessage("§c✗ Meditación interrumpida por movimiento.");
                return;
            }

            // Verificar maná disponible
            PlayerData pd = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
            if (pd == null) { player.removeMetadata("rpg-meditacion", plugin); return; }

            int maxMana = pd.getMaxMana();
            int currentMana = pd.getCurrentMana();
            int manaDrain = (int)(maxMana * manaPercent / 100.0);

            if (currentMana < manaDrain) {
                player.removeMetadata("rpg-meditacion", plugin);
                player.sendMessage("§c✗ Maná insuficiente para continuar la meditación.");
                return;
            }

            // Drenar maná
            pd.setCurrentMana(currentMana - manaDrain);
            plugin.getDatabaseManager().updatePlayerData(pd);

            // Curar vida
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            double heal = maxHealth * healPercent / 100.0;
            player.setHealth(Math.min(player.getHealth() + heal, maxHealth));

            // Partículas
            player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, player.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.02);
            player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 2, 0), 2, 0.2, 0.1, 0.2, 0);

            ticksLeft[0]--;
        }, 20L, 20L); // Cada segundo

        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.hasMetadata("rpg-meditacion")) {
                player.removeMetadata("rpg-meditacion", plugin);
                player.sendMessage("§7☯ Meditación finalizada.");
            }
            task.cancel();
        }, maxDuration * 20L + 20L);
    }

    /**
     * Carrera Zen - Burst de velocidad 150% durante 10 segundos
     */
    private void executeZenRun(Player player, Ability ability) {
        int duration = ability.getIntProperty("buff-duration", 10);

        // SPEED II = 40% extra (nivel 4 = amplifier 3 = aprox 160% extra)
        // Para 150% usamos SPEED con amplifier 7 (cada nivel añade ~20%)
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SPEED,
            duration * 20,
            7, // amplifier 7 ≈ +160% velocidad
            false, true, true
        ));

        // Partículas de viento
        player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, player.getLocation().add(0, 0.5, 0), 20, 0.3, 0.3, 0.3, 0.15);
        player.getWorld().spawnParticle(org.bukkit.Particle.CRIT, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.8f);

        spawnAbilityParticles(player, ability);
        player.sendMessage("§b💨 ¡Carrera Zen! §7(+150% velocidad por §e" + duration + "s§7) (-§9" + ability.getManaCost() + " maná§7)");

        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) player.sendMessage("§7💨 Carrera Zen finalizada.");
        }, duration * 20L);
    }

    /**
     * Golpe de los Ocho Trigramas - AoE 4 bloques, daño por segundo durante 4 segundos
     */
    private void executeEightTrigrams(Player player, Ability ability, double damage) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 4.0;
        int spinDuration = ability.getIntProperty("spin-duration", 4);
        double damagePerSecond = damage; // damage ya viene calculado con fuerza*1.1 + agil*0.3

        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);

        final int[] secondsLeft = {spinDuration};
        org.bukkit.scheduler.BukkitTask task = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (secondsLeft[0] <= 0 || !player.isOnline()) return;

            // Dibujar los 8 trigramas
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(i * 45);
                double x = Math.cos(angle) * aoe;
                double z = Math.sin(angle) * aoe;
                org.bukkit.Location particleLoc = player.getLocation().add(x, 0.5, z);
                player.getWorld().spawnParticle(org.bukkit.Particle.SWEEP_ATTACK, particleLoc, 3, 0.1, 0.1, 0.1, 0);
                player.getWorld().spawnParticle(org.bukkit.Particle.CRIT, particleLoc, 5, 0.2, 0.2, 0.2, 0.1);
            }
            // Centro
            player.getWorld().spawnParticle(org.bukkit.Particle.FLASH, player.getLocation().add(0, 0.5, 0), 1, 0, 0, 0, 0);

            // Dañar enemigos en el área
            for (org.bukkit.entity.Entity entity : player.getNearbyEntities(aoe, aoe, aoe)) {
                if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                    org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) entity;
                    if (player.getLocation().distance(target.getLocation()) <= aoe) {
                        applyAbilityDamage(target, player, damagePerSecond, ability);
                    }
                }
            }

            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.0f);
            secondsLeft[0]--;
        }, 0L, 20L); // Cada segundo

        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, task::cancel, spinDuration * 20L + 5L);

        spawnAbilityParticles(player, ability);
        player.sendMessage("§6✦ ¡Golpe de los Ocho Trigramas! §7(Daño: §e" + String.format("%.1f", damage) + "§7/s durante §e" + spinDuration + "s§7) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Ocho Puertas de la Meditación - +50% daño, +20% vel. ataque por 30s
     */
    private void executeEightGates(Player player, Ability ability) {
        int duration = ability.getIntProperty("buff-duration", 30);
        double damageBonus = ability.getDoubleProperty("damage-bonus", 0.5);
        double attackSpeedBonus = ability.getDoubleProperty("attack-speed-bonus", 0.2);

        // Guardar buff en metadata
        player.setMetadata("rpg-ocho-puertas", new FixedMetadataValue(plugin, System.currentTimeMillis() + duration * 1000L));
        player.setMetadata("rpg-ocho-puertas-damage", new FixedMetadataValue(plugin, damageBonus));

        // Haste para velocidad de ataque (+20% = Haste II)
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.HASTE,
            duration * 20,
            1,
            false, true, true
        ));

        // Fuerza extra visual
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.STRENGTH,
            duration * 20,
            0,
            false, true, true
        ));

        // Efectos visuales épicos
        player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 80, 0.5, 1, 0.5, 0.5);
        player.getWorld().spawnParticle(org.bukkit.Particle.CRIT, player.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.3);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.5f);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.8f);

        // Fin del buff
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.removeMetadata("rpg-ocho-puertas", plugin);
                player.removeMetadata("rpg-ocho-puertas-damage", plugin);
                player.sendMessage("§6⛩ Las Ocho Puertas se han cerrado.");
            }
        }, duration * 20L);

        spawnAbilityParticles(player, ability);
        player.sendMessage("§6⛩ ¡OCHO PUERTAS DE LA MEDITACIÓN! §7(+" + (int)(damageBonus * 100) + "% daño, +" + (int)(attackSpeedBonus * 100) + "% vel. ataque por §e" + duration + "s§7) (-§9" + ability.getManaCost() + " maná§7)");
    }

    /**
     * Palma de Buda - AoE 2 bloques, consume todo el maná (mín 20%), cada 10% extra +10% daño
     */
    private void executeBuddhaPalm(Player player, Ability ability, PlayerData playerData) {
        double aoe = ability.getAreaOfEffect() > 0 ? ability.getAreaOfEffect() : 2.0;
        int minManaPercent = ability.getIntProperty("min-mana-percent", 20);
        double bonusPerTen = ability.getDoubleProperty("damage-bonus-per-10-percent", 0.1);

        int maxMana = playerData.getMaxMana();
        int currentMana = playerData.getCurrentMana();

        // Verificar maná mínimo
        if (maxMana <= 0 || currentMana < (maxMana * minManaPercent / 100.0)) {
            player.sendMessage("§c✗ Necesitas al menos §e" + minManaPercent + "%§c de maná para usar Palma de Buda.");
            return;
        }

        // Calcular el daño base (scaling fuerza*0.5 + agil*0.5 ya viene en damage del ability)
        double baseDamage = ability.calculateDamage(playerData.getStats());
        if (baseDamage < 1.0) baseDamage = 1.0;

        // Calcular bonus por maná consumido
        double manaPercent = (double) currentMana / maxMana * 100.0;
        int tensConsumed = (int)(manaPercent / 10); // Cada 10% de maná consumido
        double damageMultiplier = 1.0 + (tensConsumed * bonusPerTen);
        double finalDamage = baseDamage * damageMultiplier;

        // Consumir todo el maná
        playerData.setCurrentMana(0);
        plugin.getDatabaseManager().updatePlayerData(playerData);

        // Efectos visuales
        player.getWorld().spawnParticle(org.bukkit.Particle.FLASH, player.getLocation().add(0, 1, 0), 3, 0, 0, 0, 0);
        player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 60, 0.3, 0.5, 0.3, 0.5);
        player.getWorld().spawnParticle(org.bukkit.Particle.CRIT, player.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.3);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.2f, 0.5f);

        // Dañar enemigos en el área
        int hit = 0;
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(aoe, aoe, aoe)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) entity;
                if (player.getLocation().distance(target.getLocation()) <= aoe) {
                    applyAbilityDamage(target, player, finalDamage, ability);
                    target.getWorld().spawnParticle(org.bukkit.Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.2);
                    hit++;
                }
            }
        }

        spawnAbilityParticles(player, ability);
        String msg = hit > 0
            ? "§e☸ ¡PALMA DE BUDA! §7(" + hit + " enemigos, Daño: §e" + String.format("%.1f", finalDamage) + "§7, Bonus: §e+" + String.format("%.0f", (damageMultiplier - 1) * 100) + "%§7, Maná consumido: §9" + (int)manaPercent + "%§7)"
            : "§e☸ ¡PALMA DE BUDA! §7(Sin enemigos en rango, Maná consumido: §9" + (int)manaPercent + "%§7)";
        player.sendMessage(msg);
    }

    /**
     * Descarga de Karma - Daño = vida actual del lanzador. Si falla, el jugador se daña a sí mismo
     */
    private void executeKarmaDischarge(Player player, Ability ability) {
        double currentHealth = player.getHealth();
        double damage = currentHealth; // El daño es exactamente la vida actual

        org.bukkit.entity.LivingEntity target = getTargetInSight(player, ability.getRange());

        // Efectos visuales en el jugador
        player.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.2);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.6f);

        if (target != null) {
            // Golpea al enemigo
            applyAbilityDamage(target, player, damage, ability);
            target.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.3);
            target.getWorld().spawnParticle(org.bukkit.Particle.FLASH, target.getLocation().add(0, 1, 0), 2, 0, 0, 0, 0);
            target.getWorld().playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.5f);
            player.sendMessage("§4☯ ¡DESCARGA DE KARMA! §7(Daño: §e" + String.format("%.1f", damage) + "§7 = tu vida actual) (-§9" + ability.getManaCost() + " maná§7)");
        } else {
            // Falla - el jugador se daña a sí mismo
            // Asegurar que no muera por esto (dejarlo en 0.5 mínimo)
            double selfDamage = Math.min(damage, player.getHealth() - 0.5);
            if (selfDamage > 0) {
                player.damage(selfDamage);
            }
            player.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 2, 0), 20, 0.3, 0.5, 0.3, 0.3);
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
            player.sendMessage("§c✗ ¡El karma se volvió contra ti! §7(Autodaño: §e" + String.format("%.1f", selfDamage) + "§7) (-§9" + ability.getManaCost() + " maná§7)");
        }

        spawnAbilityParticles(player, ability);
    }

    /**
     * Aplica daño de habilidad RPG al objetivo, calculando la reducción por armadura.
     *
     * Fórmula:
     *   Físico/Veneno/Necrótico: max(1, rawDamage - max(0, targetArmor - armorPen))
     *   Mágico:                  max(1, rawDamage - max(0, targetMagicArmor - magicPen))
     *
     * Se marca el atacante con metadata "rpg-bypass-minecraft-armor" para que
     * CombatListener anule la reducción de armadura de vainilla de Minecraft.
     *
     * @param target    Entidad objetivo
     * @param attacker  Jugador atacante
     * @param rawDamage Daño bruto antes de armadura
     * @param ability   Habilidad usada (contiene tipo de daño y penetración)
     */
    protected void applyAbilityDamage(LivingEntity target, Player attacker, double rawDamage, Ability ability) {
        int targetArmor = 0;
        int targetMagicArmor = 0;

        if (target instanceof Player) {
            PlayerData victimData = plugin.getDatabaseManager().getPlayerData(target.getUniqueId());
            if (victimData != null) {
                targetArmor = victimData.getStats().getArmor();
                targetMagicArmor = victimData.getStats().getMagicArmor();
            }
        }

        double effectiveDamage = DamageCalculator.calculate(
            rawDamage,
            ability.getDamageType(),
            ability.getArmorPenetration(),
            ability.getMagicPenetration(),
            targetArmor,
            targetMagicArmor
        );

        attacker.setMetadata("rpg-bypass-minecraft-armor", new FixedMetadataValue(plugin, true));
        target.damage(effectiveDamage, attacker);
    }
}
