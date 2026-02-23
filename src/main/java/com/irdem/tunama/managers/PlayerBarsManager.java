package com.irdem.tunama.managers;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.PlayerData;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestor de barras de progreso para experiencia, vida y mana
 */
public class PlayerBarsManager {

    private final TunamaRPG plugin;
    private final Map<UUID, PlayerBars> playerBars;
    private BukkitRunnable updateTask;
    private int manaRegenTick = 0; // Contador para regeneración de maná cada 5 segundos

    public PlayerBarsManager(TunamaRPG plugin) {
        this.plugin = plugin;
        this.playerBars = new HashMap<>();
        startUpdateTask();
    }

    /**
     * Inicia la tarea de actualización de barras
     */
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                manaRegenTick++;
                boolean doManaRegen = manaRegenTick >= 5; // Cada 5 segundos
                if (doManaRegen) manaRegenTick = 0;

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (doManaRegen) {
                        regenMana(player);
                    }
                    updateBars(player);
                }
            }
        };
        updateTask.runTaskTimer(plugin, 20L, 20L); // Actualizar cada segundo
    }

    /**
     * Detiene la tarea de actualización
     */
    public void stopUpdateTask() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
        }
    }

    /**
     * Crea las barras para un jugador
     */
    public void createBars(Player player) {
        if (playerBars.containsKey(player.getUniqueId())) {
            return;
        }

        // Crear barra de experiencia (dorada)
        BossBar expBar = BossBar.bossBar(
            Component.text("Experiencia: 0", NamedTextColor.GOLD),
            0.0f,
            BossBar.Color.YELLOW,
            BossBar.Overlay.PROGRESS
        );

        // Crear barra de vida (roja)
        BossBar healthBar = BossBar.bossBar(
            Component.text("Vida: 0", NamedTextColor.RED),
            1.0f,
            BossBar.Color.RED,
            BossBar.Overlay.PROGRESS
        );

        // Crear barra de mana (azul)
        BossBar manaBar = BossBar.bossBar(
            Component.text("Mana: 0", NamedTextColor.BLUE),
            1.0f,
            BossBar.Color.BLUE,
            BossBar.Overlay.PROGRESS
        );

        // Mostrar las barras al jugador
        player.showBossBar(expBar);
        player.showBossBar(healthBar);
        player.showBossBar(manaBar);

        // Guardar referencia
        playerBars.put(player.getUniqueId(), new PlayerBars(expBar, healthBar, manaBar));

        // Actualizar inmediatamente
        updateBars(player);
    }

    /**
     * Elimina las barras de un jugador
     */
    public void removeBars(Player player) {
        PlayerBars bars = playerBars.remove(player.getUniqueId());
        if (bars != null) {
            player.hideBossBar(bars.expBar);
            player.hideBossBar(bars.healthBar);
            player.hideBossBar(bars.manaBar);
        }
    }

    /**
     * Actualiza las barras de un jugador
     */
    public void updateBars(Player player) {
        PlayerBars bars = playerBars.get(player.getUniqueId());
        if (bars == null) {
            return;
        }

        PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }

        com.irdem.tunama.data.ExperienceManager expManager = plugin.getExperienceManager();

        // Actualizar barra de experiencia
        long currentExp = playerData.getExperience();
        int currentLevel = playerData.getLevel();
        long expForNextLevel = expManager.getExperienceForLevel(currentLevel + 1);
        long expForCurrentLevel = expManager.getExperienceForLevel(currentLevel);
        long expInCurrentLevel = currentExp - expForCurrentLevel;
        long expNeededForNextLevel = expForNextLevel - expForCurrentLevel;

        float expProgress = 0.0f;
        if (expNeededForNextLevel > 0) {
            expProgress = Math.min(1.0f, (float) expInCurrentLevel / expNeededForNextLevel);
        }

        bars.expBar.name(Component.text(
            String.format("§6Experiencia: §f%d§7/§f%d §a(Nivel %d)",
                expInCurrentLevel, expNeededForNextLevel, currentLevel),
            NamedTextColor.GOLD
        ));
        bars.expBar.progress(expProgress);

        // Forzar MAX_HEALTH de Minecraft a 20 (10 corazones) siempre
        // Esto previene que corazones extra aparezcan si se subieron stats de vida
        org.bukkit.attribute.AttributeInstance maxHealthAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null && maxHealthAttr.getBaseValue() != 20.0) {
            maxHealthAttr.setBaseValue(20.0);
            // Si la vida actual es mayor que 20, clampearla
            if (player.getHealth() > 20.0) {
                player.setHealth(20.0);
            }
        }

        // Actualizar barra de vida (usando stats RPG + equipo, no corazones de Minecraft)
        com.irdem.tunama.data.Race race = playerData.getRace() != null ?
            plugin.getRaceManager().getRace(playerData.getRace().toLowerCase()) : null;
        double lifeMult = race != null ? race.getLifeMultiplier() : 1.0;

        // Calcular vida base + equipo
        int baseHealth = playerData.getStats().getHealth();
        int equipHealth = 0;
        try {
            java.util.Map<String, Integer> equipStats = playerData.getEquipmentStats(plugin.getItemManager());
            equipHealth = equipStats.getOrDefault("health", 0);
        } catch (Exception ignored) {}
        int rpgMaxHealth = Math.max(20, (int)((baseHealth + equipHealth) * lifeMult));

        // La vida actual es proporcional: (vida MC actual / 20) * vida RPG máxima
        double mcHealth = Math.min(player.getHealth(), 20.0);
        double mcMaxHealth = 20.0;
        int rpgCurrentHealth = (int)((mcHealth / mcMaxHealth) * rpgMaxHealth);

        float healthPercent = (float) mcHealth / (float) mcMaxHealth;

        bars.healthBar.name(Component.text(
            String.format("§cVida: §f%d§7/§f%d",
                rpgCurrentHealth, rpgMaxHealth),
            NamedTextColor.RED
        ));
        bars.healthBar.progress(Math.max(0.0f, Math.min(1.0f, healthPercent)));

        // Actualizar velocidad de movimiento basada en agilidad
        // Cada 50 puntos de agilidad total = +1 nivel de velocidad (+0.02 walk speed)
        int baseAgility = playerData.getStats().getAgility();
        int equipAgility = 0;
        try {
            java.util.Map<String, Integer> equipStatsAgility = playerData.getEquipmentStats(plugin.getItemManager());
            equipAgility = equipStatsAgility.getOrDefault("agility", 0);
        } catch (Exception ignored) {}
        double agilityMult = race != null ? race.getAgilityMultiplier() : 1.0;
        int totalAgility = (int)((baseAgility + equipAgility) * agilityMult);
        int speedBonus = totalAgility / 50;
        float walkSpeed = Math.min(1.0f, 0.2f + (speedBonus * 0.02f));
        if (player.getWalkSpeed() != walkSpeed) {
            player.setWalkSpeed(walkSpeed);
        }

        // Actualizar barra de mana
        playerData.recalculateMaxMana();
        int maxMana = playerData.getMaxMana();
        int currentMana = playerData.getCurrentMana();
        if (currentMana > maxMana) {
            playerData.setCurrentMana(maxMana);
            currentMana = maxMana;
        }

        bars.manaBar.name(Component.text(
            String.format("§9Mana: §f%d§7/§f%d",
                currentMana, maxMana),
            NamedTextColor.BLUE
        ));
        float manaProgress = maxMana > 0 ? (float) currentMana / maxMana : 0f;
        bars.manaBar.progress(Math.max(0f, Math.min(1f, manaProgress)));
    }

    /**
     * Regenera el 10% del maná máximo del jugador
     */
    private void regenMana(Player player) {
        PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        playerData.recalculateMaxMana();
        int maxMana = playerData.getMaxMana();
        int currentMana = playerData.getCurrentMana();
        if (currentMana < maxMana) {
            int regenAmount = Math.max(1, maxMana / 10); // 10% del maná máximo
            playerData.regenMana(regenAmount);
        }
    }

    /**
     * Clase interna para almacenar las barras de un jugador
     */
    private static class PlayerBars {
        final BossBar expBar;
        final BossBar healthBar;
        final BossBar manaBar;

        PlayerBars(BossBar expBar, BossBar healthBar, BossBar manaBar) {
            this.expBar = expBar;
            this.healthBar = healthBar;
            this.manaBar = manaBar;
        }
    }
}
