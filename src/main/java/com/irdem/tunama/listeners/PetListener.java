package com.irdem.tunama.listeners;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.Pet;
import com.irdem.tunama.data.PetCommand;
import com.irdem.tunama.managers.PetManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;

/**
 * Listener para eventos relacionados con las mascotas de combate
 */
public class PetListener implements Listener {

    private final TunamaRPG plugin;

    public PetListener(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    /**
     * Cuando una mascota recibe daño
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPetDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        PetManager petManager = plugin.getPetManager();
        if (!petManager.isRpgPet(event.getEntity())) return;

        Pet pet = petManager.getPetByEntity(event.getEntity());
        if (pet == null) return;

        // Prevenir daño de caída, ahogamiento, etc. para mascotas
        switch (event.getCause()) {
            case FALL:
            case DROWNING:
            case SUFFOCATION:
            case STARVATION:
                event.setCancelled(true);
                return;
            default:
                break;
        }

        // Aplicar armadura del sistema RPG
        double damage = event.getDamage();
        double reducedDamage = Math.max(1, damage - pet.getArmor());
        event.setDamage(reducedDamage);
    }

    /**
     * Cuando una mascota recibe daño de otra entidad
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPetDamageByEntity(EntityDamageByEntityEvent event) {
        PetManager petManager = plugin.getPetManager();

        // Si la víctima es una mascota
        if (petManager.isRpgPet(event.getEntity())) {
            Pet pet = petManager.getPetByEntity(event.getEntity());
            if (pet == null) return;

            // Obtener el atacante real
            Entity damager = event.getDamager();
            if (damager instanceof Projectile) {
                Projectile proj = (Projectile) damager;
                if (proj.getShooter() instanceof Entity) {
                    damager = (Entity) proj.getShooter();
                }
            }

            // Prevenir daño del dueño a su propia mascota
            Player owner = petManager.getOwnerByPetEntity(event.getEntity());
            if (owner != null && damager.equals(owner)) {
                event.setCancelled(true);
                return;
            }

            // Prevenir daño de otras mascotas del mismo dueño
            if (petManager.isRpgPet(damager)) {
                Player otherOwner = petManager.getOwnerByPetEntity(damager);
                if (owner != null && owner.equals(otherOwner)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Si el atacante es una mascota
        if (petManager.isRpgPet(event.getDamager())) {
            Pet pet = petManager.getPetByEntity(event.getDamager());
            if (pet == null) return;

            // Prevenir que la mascota ataque a su dueño
            Player owner = petManager.getOwnerByPetEntity(event.getDamager());
            if (owner != null && event.getEntity().equals(owner)) {
                event.setCancelled(true);
                return;
            }

            // Prevenir que la mascota ataque a otras mascotas del mismo dueño
            if (petManager.isRpgPet(event.getEntity())) {
                Player otherOwner = petManager.getOwnerByPetEntity(event.getEntity());
                if (owner != null && owner.equals(otherOwner)) {
                    event.setCancelled(true);
                    return;
                }
            }

            // Aplicar daño de la mascota
            event.setDamage(pet.getDamage());
        }
    }

    /**
     * Cuando una mascota muere
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPetDeath(EntityDeathEvent event) {
        PetManager petManager = plugin.getPetManager();
        if (!petManager.isRpgPet(event.getEntity())) return;

        Pet pet = petManager.getPetByEntity(event.getEntity());
        if (pet == null) return;

        Player owner = petManager.getOwnerByPetEntity(event.getEntity());

        // La mascota no dropea items
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Marcar como muerta y guardar
        pet.kill(); // Esto marca como dead=true, currentHealth=0, active=false

        // Remover de la lista de mascotas activas
        if (owner != null) {
            List<Pet> activePets = petManager.getActivePets(owner);
            activePets.remove(pet);

            owner.sendMessage("§c✗ " + pet.getDisplayName() + " ha sido derrotado");
            owner.sendMessage("§7Usa una poción o espera para revivirlo.");
        }

        // Guardar en base de datos con vida 0
        plugin.getDatabaseManager().savePet(pet);
    }

    /**
     * Cuando el dueño es atacado - las mascotas en modo DEFEND lo defienden
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player owner = (Player) event.getEntity();
        PetManager petManager = plugin.getPetManager();

        List<Pet> pets = petManager.getActivePets(owner);
        if (pets.isEmpty()) return;

        // Obtener el atacante real
        Entity attacker = event.getDamager();
        if (attacker instanceof Projectile) {
            Projectile proj = (Projectile) attacker;
            if (proj.getShooter() instanceof Entity) {
                attacker = (Entity) proj.getShooter();
            }
        }

        // No hacer que las mascotas ataquen otras mascotas del mismo dueño
        if (petManager.isRpgPet(attacker)) {
            Player attackerOwner = petManager.getOwnerByPetEntity(attacker);
            if (owner.equals(attackerOwner)) return;
        }

        // Hacer que las mascotas en modo DEFEND ataquen al agresor
        for (Pet pet : pets) {
            if (pet.getCurrentCommand() == PetCommand.DEFEND && attacker instanceof LivingEntity) {
                pet.setTarget(attacker);
                if (pet.getEntity() instanceof org.bukkit.entity.Mob) {
                    ((org.bukkit.entity.Mob) pet.getEntity()).setTarget((LivingEntity) attacker);
                }
            }
        }
    }

    /**
     * Cuando el dueño ataca a algo - las mascotas marcan el mismo objetivo
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerAttack(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        // Obtener el jugador atacante
        Player owner = null;
        if (damager instanceof Player) {
            owner = (Player) damager;
        } else if (damager instanceof Projectile) {
            Projectile proj = (Projectile) damager;
            if (proj.getShooter() instanceof Player) {
                owner = (Player) proj.getShooter();
            }
        }

        if (owner == null) return;

        PetManager petManager = plugin.getPetManager();
        Entity target = event.getEntity();

        // No marcar mascotas propias como objetivo
        if (petManager.isRpgPet(target)) {
            Player targetOwner = petManager.getOwnerByPetEntity(target);
            if (owner.equals(targetOwner)) return;
        }

        // Marcar objetivo para mascotas en modo ATTACK o FOLLOW
        if (target instanceof LivingEntity) {
            petManager.setTargetForPets(owner, target);
        }
    }

    /**
     * Prevenir que mascotas marquen a su dueño como objetivo
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityTarget(EntityTargetEvent event) {
        PetManager petManager = plugin.getPetManager();

        if (!petManager.isRpgPet(event.getEntity())) return;

        Entity target = event.getTarget();
        if (target == null) return;

        Player owner = petManager.getOwnerByPetEntity(event.getEntity());

        // Prevenir que ataque a su dueño
        if (owner != null && target.equals(owner)) {
            event.setCancelled(true);
            return;
        }

        // Prevenir que ataque a otras mascotas del mismo dueño
        if (petManager.isRpgPet(target)) {
            Player targetOwner = petManager.getOwnerByPetEntity(target);
            if (owner != null && owner.equals(targetOwner)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Cuando el jugador se desconecta - guardar todas las mascotas
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PetManager petManager = plugin.getPetManager();

        // Guardar y desinvocar todas las mascotas
        petManager.dismissAllPets(player);
    }

    /**
     * Cuando el jugador se teletransporta - las mascotas van con él
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        PetManager petManager = plugin.getPetManager();

        List<Pet> pets = petManager.getActivePets(player);
        if (pets.isEmpty()) return;

        // Teletransportar mascotas después de un tick para evitar problemas
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Pet pet : pets) {
                if (pet.getEntity() != null && !pet.getEntity().isDead()) {
                    pet.getEntity().teleport(player.getLocation());
                }
            }
        }, 1L);
    }

    /**
     * Cuando el jugador cambia de mundo - las mascotas van con él
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        PetManager petManager = plugin.getPetManager();

        List<Pet> pets = petManager.getActivePets(player);
        if (pets.isEmpty()) return;

        // Teletransportar mascotas al nuevo mundo
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Pet pet : pets) {
                if (pet.getEntity() != null && !pet.getEntity().isDead()) {
                    pet.getEntity().teleport(player.getLocation());
                }
            }
        }, 1L);
    }

    /**
     * Dar experiencia a las mascotas cuando el dueño mata a un mob
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobKillByOwner(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        PetManager petManager = plugin.getPetManager();
        List<Pet> pets = petManager.getActivePets(killer);
        if (pets.isEmpty()) return;

        // Dar experiencia a mascotas cercanas
        for (Pet pet : pets) {
            if (pet.getEntity() == null || pet.getEntity().isDead()) continue;

            // Solo dar XP si la mascota está cerca (participó)
            if (pet.getEntity().getLocation().distance(event.getEntity().getLocation()) <= 20) {
                int xpGain = 10 + (event.getDroppedExp() / 2);
                boolean leveledUp = pet.addExperience(xpGain);

                if (leveledUp) {
                    // Recalcular stats
                    pet.calculateStats(petManager.getPetType(pet.getTypeId()));
                    pet.setCurrentHealth(pet.getMaxHealth()); // Curar al subir de nivel

                    // Actualizar nombre con colores correctos
                    if (pet.getEntity() instanceof LivingEntity) {
                        LivingEntity living = (LivingEntity) pet.getEntity();
                        String petNameFormatted = "§a" + pet.getDisplayName() + " §7[Nv." + pet.getLevel() + "]";
                        living.customName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(petNameFormatted));
                    }

                    killer.sendMessage("§a✓ ¡" + pet.getDisplayName() + " subió al nivel " + pet.getLevel() + "!");
                }

                // Guardar progreso
                plugin.getDatabaseManager().savePet(pet);
            }
        }
    }
}
