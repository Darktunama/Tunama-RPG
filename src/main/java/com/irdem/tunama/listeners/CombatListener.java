package com.irdem.tunama.listeners;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.Clan;
import com.irdem.tunama.data.ExperienceManager;
import com.irdem.tunama.data.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Listener para manejar eventos de combate y muertes
 */
public class CombatListener implements Listener {

    private final TunamaRPG plugin;

    public CombatListener(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    /**
     * Detecta combate general para ocultar el scoreboard.
     * Se activa cuando un jugador ataca o es atacado.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (plugin.getScoreboardManager() == null) return;

        // Obtener el atacante real (si es un proyectil, obtener quien lo disparó)
        Entity damager = event.getDamager();
        Player attacker = null;

        if (damager instanceof Player) {
            attacker = (Player) damager;
        } else if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }

        // Si el atacante es un jugador, entrar en combate
        if (attacker != null) {
            plugin.getScoreboardManager().enterCombat(attacker);

            // Guardar el objetivo de ataque para los summons del jugador
            if (event.getEntity() instanceof LivingEntity && !(event.getEntity() instanceof Player)) {
                attacker.setMetadata("rpg-summon-attack-target",
                    new FixedMetadataValue(plugin, event.getEntity().getUniqueId().toString()));
            }
        }

        // Si la víctima es un jugador, entrar en combate
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            plugin.getScoreboardManager().enterCombat(victim);

            // Guardar quien atacó al jugador para que sus summons lo defiendan
            Entity realDamager = damager;
            if (damager instanceof Projectile) {
                Projectile proj = (Projectile) damager;
                if (proj.getShooter() instanceof Entity) {
                    realDamager = (Entity) proj.getShooter();
                }
            }
            if (realDamager instanceof LivingEntity && !(realDamager instanceof Player)) {
                victim.setMetadata("rpg-summon-last-attacker",
                    new FixedMetadataValue(plugin, realDamager.getUniqueId().toString()));
            }
        }
    }

    /**
     * Prevenir que los summons targeteen a su dueño
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSummonTarget(EntityTargetEvent event) {
        Entity entity = event.getEntity();
        Entity target = event.getTarget();

        if (target == null) return;

        // Si el summon intenta targetear a su dueño, cancelar
        if (entity.hasMetadata("rpg-summon-owner") && target instanceof Player) {
            String ownerUUID = entity.getMetadata("rpg-summon-owner").get(0).asString();
            Player player = (Player) target;
            if (player.getUniqueId().toString().equals(ownerUUID)) {
                event.setCancelled(true);
                event.setTarget(null);
                return;
            }
        }

        // Si el summon intenta targetear a otro summon del mismo dueño, cancelar
        if (entity.hasMetadata("rpg-summon-owner") && target.hasMetadata("rpg-summon-owner")) {
            String ownerUUID = entity.getMetadata("rpg-summon-owner").get(0).asString();
            String targetOwnerUUID = target.getMetadata("rpg-summon-owner").get(0).asString();
            if (ownerUUID.equals(targetOwnerUUID)) {
                event.setCancelled(true);
                event.setTarget(null);
            }
        }
    }

    /**
     * Prevenir que las invocaciones ataquen a su dueño (triple protección)
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onSummonDamageOwner(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();
        Entity realDamager = damager;

        // Resolver el atacante real desde proyectiles (flechas, fireballs, etc.)
        if (damager instanceof Projectile) {
            Projectile proj = (Projectile) damager;
            if (proj.getShooter() instanceof Entity) {
                realDamager = (Entity) proj.getShooter();
            }
        }

        // Si el atacante real es un summon
        if (realDamager.hasMetadata("rpg-summon-owner")) {
            String ownerUUID = realDamager.getMetadata("rpg-summon-owner").get(0).asString();

            // Si la víctima es el dueño, cancelar el daño
            if (victim instanceof Player) {
                Player player = (Player) victim;
                if (player.getUniqueId().toString().equals(ownerUUID)) {
                    event.setCancelled(true);
                    // Apagar fuego causado por el summon
                    player.setFireTicks(0);
                    return;
                }
            }

            // Si la víctima es otro summon del mismo dueño, cancelar
            if (victim.hasMetadata("rpg-summon-owner")) {
                String victimOwnerUUID = victim.getMetadata("rpg-summon-owner").get(0).asString();
                if (ownerUUID.equals(victimOwnerUUID)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Si el dueño golpea a su propio summon, cancelar y limpiar target
        if (victim.hasMetadata("rpg-summon-owner") && realDamager instanceof Player) {
            String ownerUUID = victim.getMetadata("rpg-summon-owner").get(0).asString();
            Player player = (Player) realDamager;
            if (player.getUniqueId().toString().equals(ownerUUID)) {
                event.setCancelled(true);

                if (victim instanceof org.bukkit.entity.Mob) {
                    org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) victim;
                    mob.setTarget(null);
                }
            }
        }
    }

    /**
     * Aplicar lifesteal de Sed de Sangre cuando el jugador hace daño
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDealDamage(EntityDamageByEntityEvent event) {
        Entity damagerEntity = event.getDamager();
        Player attacker = null;

        if (damagerEntity instanceof Player) {
            attacker = (Player) damagerEntity;
        } else if (damagerEntity instanceof Projectile) {
            Projectile proj = (Projectile) damagerEntity;
            if (proj.getShooter() instanceof Player) {
                attacker = (Player) proj.getShooter();
            }
        }

        if (attacker == null) return;

        // Verificar si tiene Sed de Sangre activa
        if (attacker.hasMetadata("rpg-blood-thirst")) {
            long expiration = attacker.getMetadata("rpg-blood-thirst").get(0).asLong();
            if (System.currentTimeMillis() < expiration && attacker.hasMetadata("rpg-blood-thirst-lifesteal")) {
                double lifesteal = attacker.getMetadata("rpg-blood-thirst-lifesteal").get(0).asDouble();
                double healAmount = event.getFinalDamage() * lifesteal;

                if (healAmount > 0) {
                    double maxHealth = attacker.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                    double newHealth = Math.min(attacker.getHealth() + healAmount, maxHealth);
                    attacker.setHealth(newHealth);

                    // Efecto visual de curación
                    attacker.getWorld().spawnParticle(org.bukkit.Particle.HEART, attacker.getLocation().add(0, 2, 0), 2, 0.3, 0.2, 0.3, 0);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Si es un summon o mascota, no dropear nada
        org.bukkit.NamespacedKey summonKey = new org.bukkit.NamespacedKey(plugin, "rpg-summon");
        org.bukkit.NamespacedKey petKey = new org.bukkit.NamespacedKey(plugin, "rpg-pet");
        boolean isSummon = entity.hasMetadata("rpg-summon-owner")
            || entity.hasMetadata("rpg-pet-owner")
            || entity.getPersistentDataContainer().has(summonKey, org.bukkit.persistence.PersistentDataType.STRING)
            || entity.getPersistentDataContainer().has(petKey, org.bukkit.persistence.PersistentDataType.STRING);

        if (isSummon) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }

        Player killer = entity.getKiller();

        // Si no hay killer (jugador), no contar
        if (killer == null) return;

        // Si el que muere es un jugador
        if (entity instanceof Player) {
            Player victim = (Player) entity;

            // Incrementar kills de jugadores del asesino
            incrementPlayerKill(killer);

            // Incrementar kills de PvP del clan del asesino
            incrementClanPvpKill(killer);

        } else {
            // Contar kills de mobs (solo Monster para el contador)
            if (entity instanceof Monster) {
                incrementMobKill(killer);
            }

            // Otorgar experiencia por cualquier mob configurado en experiencia.yml
            awardMobExperience(killer, entity);
        }
    }

    private void awardMobExperience(Player player, LivingEntity entity) {
        try {
            ExperienceManager expManager = plugin.getExperienceManager();
            int xpAmount = expManager.getMobExperience(entity.getType());

            if (xpAmount <= 0) return;

            // Cargar datos del jugador
            PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
            if (playerData == null) return;

            int oldLevel = playerData.getLevel();

            // Añadir experiencia
            playerData.addExperience(xpAmount);

            // Calcular nuevo nivel
            int newLevel = expManager.calculateLevel(playerData.getExperience());

            if (newLevel > oldLevel) {
                // Subió de nivel
                playerData.setLevel(newLevel);

                // Otorgar puntos de estadística por cada nivel subido
                int levelsGained = newLevel - oldLevel;
                playerData.addStatPoints(levelsGained * 3); // 3 puntos por nivel

                player.sendMessage("§6§l✦ ¡SUBISTE DE NIVEL! §eNivel " + oldLevel + " → " + newLevel);
                player.sendMessage("§a+" + (levelsGained * 3) + " puntos de estadística disponibles");
            }

            // Guardar cambios
            plugin.getDatabaseManager().updatePlayerData(playerData);

            // Mostrar XP ganada en action bar
            long nextLevelXp = expManager.getExperienceForNextLevel(playerData.getLevel());
            String progressMsg = "§a+" + xpAmount + " XP §7(§f" + playerData.getExperience() + (nextLevelXp > 0 ? "/" + nextLevelXp : "") + "§7)";
            player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(progressMsg));

        } catch (Exception e) {
            plugin.getLogger().warning("Error al otorgar experiencia a " + player.getName() + ": " + e.getMessage());
        }
    }

    private void incrementMobKill(Player player) {
        try {
            int activeSlot = plugin.getDatabaseManager().getCharacterManager().getActiveSlot(player.getUniqueId());
            plugin.getDatabaseManager().incrementMobKills(player.getUniqueId(), activeSlot);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al incrementar mob kills para " + player.getName() + ": " + e.getMessage());
        }
    }

    private void incrementPlayerKill(Player player) {
        try {
            int activeSlot = plugin.getDatabaseManager().getCharacterManager().getActiveSlot(player.getUniqueId());
            plugin.getDatabaseManager().incrementPlayerKills(player.getUniqueId(), activeSlot);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al incrementar player kills para " + player.getName() + ": " + e.getMessage());
        }
    }

    private void incrementClanPvpKill(Player player) {
        try {
            Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (clan != null) {
                clan.addPvpKill();
                // Actualizar en base de datos
                plugin.getClanManager().updateClanPvpKills(clan.getId(), clan.getPvpKills());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error al incrementar clan pvp kills para " + player.getName() + ": " + e.getMessage());
        }
    }
}
