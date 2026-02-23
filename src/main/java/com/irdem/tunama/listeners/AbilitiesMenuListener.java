package com.irdem.tunama.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.commands.AbilitiesCommand;
import com.irdem.tunama.data.Ability;
import com.irdem.tunama.data.PlayerData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener para el menú de habilidades.
 * Solo maneja la asignación de habilidades a slots desde el menú /habilidades.
 * El lanzamiento de habilidades se maneja en AbilityBarListener con el sistema de tecla F.
 */
public class AbilitiesMenuListener implements Listener {

    private final TunamaRPG plugin;
    // Almacena bindings: jugador -> (slot hotbar -> ability ID) - para compatibilidad
    private static final Map<UUID, Map<Integer, String>> abilityBindings = new HashMap<>();

    // Traducciones de materiales al español
    private static final Map<String, String> WEAPON_NAMES = new HashMap<>();
    static {
        WEAPON_NAMES.put("BOW", "arco");
        WEAPON_NAMES.put("CROSSBOW", "ballesta");
        WEAPON_NAMES.put("DIAMOND_SWORD", "espada de diamante");
        WEAPON_NAMES.put("NETHERITE_SWORD", "espada de netherita");
        WEAPON_NAMES.put("IRON_SWORD", "espada de hierro");
        WEAPON_NAMES.put("WOODEN_SWORD", "espada de madera");
        WEAPON_NAMES.put("STONE_SWORD", "espada de piedra");
        WEAPON_NAMES.put("GOLDEN_SWORD", "espada de oro");
        WEAPON_NAMES.put("TRIDENT", "tridente");
        WEAPON_NAMES.put("SHIELD", "escudo");
    }

    public AbilitiesMenuListener(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    // --- MENÚ: click en GUI para vincular habilidad al slot actual ---

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getView().getTitle() == null) return;
        if (!event.getView().getTitle().equals(AbilitiesCommand.MENU_TITLE)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        // Items bloqueados (gris)
        if (clicked.getType() == Material.GRAY_DYE) return;

        // Buscar la habilidad por nombre
        String displayName = meta.getDisplayName();
        String abilityName = displayName.replaceAll("§[0-9a-fklmnor]", "").replace("⚔ ", "").trim();
        Ability ability = findAbilityByName(abilityName);
        if (ability == null) return;

        // Verificar nivel
        PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;
        if (playerData.getLevel() < ability.getRequiredLevel()) {
            player.sendMessage("§c✗ Necesitas nivel " + ability.getRequiredLevel() + " para usar esta habilidad.");
            return;
        }

        // Bloquear habilidades pasivas - no se pueden asignar a la barra
        if (ability.isPassive()) {
            player.sendMessage("§e⚡ " + ability.getName() + " §7es una habilidad §bpasiva§7.");
            player.sendMessage("§7Las habilidades pasivas se activan automáticamente cuando cumples los requisitos.");
            return;
        }

        // Vincular al slot de la barra de habilidades (usa el slot actual del hotbar como referencia)
        int slot = player.getInventory().getHeldItemSlot();

        // Bloquear slot 8 (tecla 9) - reservado para el sistema
        if (slot == 8) {
            player.sendMessage("§c✗ El slot 9 está reservado y no se pueden asignar habilidades.");
            return;
        }

        // Usar el nuevo sistema de AbilityBarListener
        AbilityBarListener.setAbilitySlot(player.getUniqueId(), slot, ability.getId());

        // También mantener compatibilidad con el sistema antiguo
        abilityBindings.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(slot, ability.getId());

        player.sendMessage("§a✓ §e" + ability.getName() + " §aasignada al slot §f" + (slot + 1) + " §ade la barra de habilidades.");
        player.sendMessage("§7Doble-tap §fF §7para modo habilidades, luego §f" + (slot + 1) + " §7para lanzar.");
        if ("static".equalsIgnoreCase(ability.getCastMode())) {
            player.sendMessage("§7Esta habilidad requiere estar §fquieto§7.");
        }
        player.closeInventory();

        // Mostrar en action bar
        showActionBar(player, ability);
    }

    // --- Limpieza al desconectar ---

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        abilityBindings.remove(event.getPlayer().getUniqueId());
        PlayerData.clearManaCache(event.getPlayer().getUniqueId());
    }

    // --- Utilidades ---

    private String translateWeaponName(String materialName) {
        String upper = materialName.toUpperCase();
        return WEAPON_NAMES.getOrDefault(upper, materialName.toLowerCase().replace("_", " "));
    }

    private void showActionBar(Player player, Ability ability) {
        String weaponInfo = "";
        if (!ability.getRequiredWeapon().isEmpty()) {
            weaponInfo = " §7[§e" + translateWeaponName(ability.getRequiredWeapon()) + "§7]";
        }
        String castInfo = "static".equalsIgnoreCase(ability.getCastMode()) ? "§7(quieto)" : "";
        player.sendActionBar(Component.text(
            "§a✓ " + ability.getName() + " §7| Maná: §9" + ability.getManaCost() + weaponInfo + " " + castInfo
        ));
    }

    private Ability findAbilityByName(String name) {
        Map<String, Ability> allAbilities = plugin.getAbilityManager().getAllAbilities();
        for (Ability ability : allAbilities.values()) {
            if (ability.getName().equalsIgnoreCase(name)) {
                return ability;
            }
        }
        return null;
    }

    /**
     * Maneja el daño de flechas RPG al impactar.
     * Sistema RPG: daño - armadura = daño real que se resta de la vida RPG.
     * Los corazones de Minecraft representan un % de la vida RPG total.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow)) return;
        Arrow arrow = (Arrow) event.getDamager();

        if (!arrow.hasMetadata("rpg-damage")) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity target = (LivingEntity) event.getEntity();
        double rpgDamage = arrow.getMetadata("rpg-damage").get(0).asDouble();

        // Obtener penetración de armadura de la habilidad
        double armorPenetration = 0.0;
        if (arrow.hasMetadata("rpg-pierce")) {
            armorPenetration = 0.3; // 30% de penetración por defecto para flecha penetrante
        }

        // Si el objetivo es un jugador, usar sistema RPG completo
        if (target instanceof Player) {
            Player playerTarget = (Player) target;
            PlayerData targetData = plugin.getDatabaseManager().getPlayerData(playerTarget.getUniqueId());

            if (targetData != null) {
                // Cancelar el daño de Minecraft - lo manejamos nosotros
                event.setCancelled(true);

                // Calcular armadura total: MC armor + RPG equipment armor
                double mcArmor = 0;
                org.bukkit.attribute.AttributeInstance armorAttr = target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR);
                if (armorAttr != null) {
                    mcArmor = armorAttr.getValue();
                }

                // Obtener armadura de equipo RPG
                int equipArmor = 0;
                try {
                    java.util.Map<String, Integer> equipStats = targetData.getEquipmentStats(plugin.getItemManager());
                    equipArmor = equipStats.getOrDefault("armor", 0);
                } catch (Exception ignored) {}

                double totalArmor = mcArmor + equipArmor;

                // Aplicar penetración de armadura
                double effectiveArmor = totalArmor * (1.0 - armorPenetration);

                // Calcular daño real = daño RPG - armadura efectiva (mínimo 1)
                double actualDamage = Math.max(1.0, rpgDamage - effectiveArmor);

                // Calcular vida RPG máxima
                com.irdem.tunama.data.Race race = targetData.getRace() != null ?
                    plugin.getRaceManager().getRace(targetData.getRace().toLowerCase()) : null;
                double lifeMult = race != null ? race.getLifeMultiplier() : 1.0;

                int baseHealth = targetData.getStats().getHealth();
                int equipHealth = 0;
                try {
                    java.util.Map<String, Integer> equipStats = targetData.getEquipmentStats(plugin.getItemManager());
                    equipHealth = equipStats.getOrDefault("health", 0);
                } catch (Exception ignored) {}
                int rpgMaxHealth = Math.max(20, (int)((baseHealth + equipHealth) * lifeMult));

                // Calcular vida RPG actual desde vida MC (MC health / 20 * RPG max health)
                double mcHealth = Math.min(playerTarget.getHealth(), 20.0);
                double rpgCurrentHealth = (mcHealth / 20.0) * rpgMaxHealth;

                // Restar daño real
                double newRpgHealth = Math.max(0, rpgCurrentHealth - actualDamage);

                // Convertir de vuelta a vida MC (0-20)
                double newMcHealth = (newRpgHealth / rpgMaxHealth) * 20.0;
                newMcHealth = Math.max(0, Math.min(20.0, newMcHealth));

                // Aplicar la nueva vida
                if (newMcHealth <= 0) {
                    // El jugador muere
                    playerTarget.setHealth(0);
                } else {
                    playerTarget.setHealth(newMcHealth);
                    // Efecto visual de daño
                    playerTarget.damage(0.01); // Esto activa la animación de daño sin hacer daño real
                }

                // Mostrar daño al atacante
                if (arrow.getShooter() instanceof Player) {
                    Player shooter = (Player) arrow.getShooter();
                    boolean isCrit = arrow.hasMetadata("rpg-crit");
                    String critText = isCrit ? " §6¡CRÍTICO!" : "";
                    shooter.sendActionBar(Component.text("§c-" + (int)actualDamage + critText + " §7(Armadura: " + (int)effectiveArmor + ")"));

                    // Entrar en combate (atacante)
                    if (plugin.getScoreboardManager() != null) {
                        plugin.getScoreboardManager().enterCombat(shooter);
                    }
                }

                // Entrar en combate (víctima)
                if (plugin.getScoreboardManager() != null) {
                    plugin.getScoreboardManager().enterCombat(playerTarget);
                }
            }
        } else {
            // Para mobs, usar daño normal de Minecraft
            // Calcular armadura del mob (solo MC armor, no tienen equipo RPG)
            double mobArmor = 0;
            org.bukkit.attribute.AttributeInstance mobArmorAttr = target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR);
            if (mobArmorAttr != null) {
                mobArmor = mobArmorAttr.getValue();
            }

            // Aplicar penetración de armadura
            double effectiveMobArmor = mobArmor * (1.0 - armorPenetration);

            // Calcular daño real para mobs
            double mobActualDamage = Math.max(1.0, rpgDamage - effectiveMobArmor);

            // Mobs no tienen vida RPG, así que aplicamos daño directo convertido
            double mcDamage = mobActualDamage / 10.0; // Convertir a escala MC
            mcDamage = Math.max(1.0, Math.min(40.0, mcDamage)); // Min 0.5 corazones, max 20 corazones
            event.setDamage(mcDamage);

            // Entrar en combate (atacante vs mob)
            if (arrow.getShooter() instanceof Player && plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().enterCombat((Player) arrow.getShooter());
            }
        }

        // Aplicar veneno si la flecha lo tiene
        if (arrow.hasMetadata("rpg-poison")) {
            // Veneno nivel 1 por 5 segundos (100 ticks)
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0, false, true));

            // Partículas de veneno
            target.getWorld().spawnParticle(
                Particle.WITCH,
                target.getLocation().add(0, 1, 0),
                15, 0.3, 0.5, 0.3, 0.05
            );
        }

        // Manejar rebote si la flecha es rebotante
        if (arrow.hasMetadata("rpg-bounce")) {
            int remainingBounces = arrow.getMetadata("rpg-bounce").get(0).asInt();
            if (remainingBounces > 0 && arrow.getShooter() instanceof Player) {
                Player shooter = (Player) arrow.getShooter();

                // Buscar siguiente objetivo cercano (diferente al actual)
                LivingEntity nextTarget = findNextBounceTarget(target, 8.0, shooter);

                if (nextTarget != null) {
                    // Programar el rebote para el siguiente tick
                    final int newBounces = remainingBounces - 1;
                    final double savedDamage = rpgDamage;
                    final LivingEntity hitTarget = target;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        // Lanzar nueva flecha hacia el siguiente objetivo
                        Location arrowSpawn = hitTarget.getLocation().add(0, 1, 0);
                        Vector direction = nextTarget.getLocation().add(0, 1, 0)
                            .subtract(arrowSpawn).toVector().normalize();

                        Arrow newArrow = shooter.getWorld().spawnArrow(
                            arrowSpawn, direction, 2.5f, 0
                        );
                        newArrow.setShooter(shooter);
                        newArrow.setMetadata("rpg-damage", new FixedMetadataValue(plugin, savedDamage * 0.8)); // 80% del daño
                        newArrow.setMetadata("rpg-bounce", new FixedMetadataValue(plugin, newBounces));

                        // Partículas de rebote
                        hitTarget.getWorld().spawnParticle(
                            Particle.ENCHANTED_HIT,
                            hitTarget.getLocation().add(0, 1, 0),
                            10, 0.2, 0.2, 0.2, 0.1
                        );
                    }, 1L);
                }
            }
        }

        // Eliminar la flecha después del impacto para evitar daño duplicado
        plugin.getServer().getScheduler().runTaskLater(plugin, arrow::remove, 1L);
    }

    /**
     * Busca el siguiente objetivo para el rebote de flecha
     */
    private LivingEntity findNextBounceTarget(LivingEntity currentTarget, double range, Player shooter) {
        LivingEntity nearest = null;
        double nearestDist = range + 1;

        for (Entity entity : currentTarget.getNearbyEntities(range, range, range)) {
            // Solo entidades vivas, no el jugador que disparó, no el objetivo actual
            if (!(entity instanceof LivingEntity)) continue;
            if (entity.equals(currentTarget)) continue;
            if (entity.equals(shooter)) continue;

            LivingEntity living = (LivingEntity) entity;
            double dist = living.getLocation().distance(currentTarget.getLocation());

            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = living;
            }
        }

        return nearest;
    }

    /**
     * Obtiene los bindings de un jugador (para uso externo)
     */
    public static Map<Integer, String> getBindings(UUID uuid) {
        return abilityBindings.getOrDefault(uuid, new HashMap<>());
    }

    /**
     * Limpia los bindings de un jugador (al cambiar de personaje o desconectar)
     */
    public static void clearBindings(UUID uuid) {
        abilityBindings.remove(uuid);
    }
}
