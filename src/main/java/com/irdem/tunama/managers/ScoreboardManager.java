package com.irdem.tunama.managers;

import com.irdem.tunama.TunamaRPG;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private final TunamaRPG plugin;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private final Map<UUID, List<String>> playerLastEntries = new HashMap<>();

    // Sistema de combate - ocultar scoreboard mientras está en combate
    private final Map<UUID, Long> playersInCombat = new HashMap<>();
    private final Map<UUID, Boolean> scoreboardHidden = new HashMap<>();
    private static final long COMBAT_TIMEOUT_MS = 10000; // 10 segundos sin daño = fuera de combate

    // Scoreboard line data
    private final List<ScoreboardLine> lines = new ArrayList<>();

    // Title frames (pre-generated)
    private final List<String> titleFrames = new ArrayList<>();
    private int currentTitleFrame = 0;
    private int titleInterval = 2;
    private int titleTickCounter = 0;

    // Config
    private boolean enabled = true;
    private int updateInterval = 10;

    // Animated colors for lines
    private final List<String> animatedColors = new ArrayList<>();
    private int animatedColorOffset = 0;
    private int animatedSpeed = 2;
    private int animatedTickCounter = 0;

    // Conditions
    private final Map<String, Condition> conditions = new HashMap<>();

    private BukkitRunnable updateTask;
    private boolean placeholderApiAvailable = false;

    public ScoreboardManager(TunamaRPG plugin) {
        this.plugin = plugin;
        this.placeholderApiAvailable = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        loadConfig();
        loadDefaultScoreboard();

        if (enabled) {
            startUpdateTask();
            plugin.getLogger().info("Sistema de scoreboard activado con " + lines.size() + " lineas, " + titleFrames.size() + " frames de titulo");
        }
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "scoreboard-config.yml");
        if (!configFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        enabled = config.getBoolean("scoreboard.enabled", true);
        updateInterval = config.getInt("scoreboard.update-interval", 10);

        // Colores animados para lineas
        List<String> colors = config.getStringList("scoreboard.animated-colors.colors");
        if (!colors.isEmpty()) {
            animatedColors.addAll(colors);
        } else {
            animatedColors.add("&b");
            animatedColors.add("&3");
            animatedColors.add("&9");
            animatedColors.add("&d");
            animatedColors.add("&c");
            animatedColors.add("&6");
            animatedColors.add("&e");
            animatedColors.add("&a");
        }
        animatedSpeed = config.getInt("scoreboard.animated-colors.speed", 2);

        // Condiciones
        ConfigurationSection condSection = config.getConfigurationSection("scoreboard.conditions");
        if (condSection != null) {
            for (String key : condSection.getKeys(false)) {
                Condition cond = new Condition();
                cond.type = condSection.getString(key + ".type", "placeholder-equals");
                cond.placeholder = condSection.getString(key + ".placeholder", "");
                cond.value = condSection.getString(key + ".value", "");
                conditions.put(key, cond);
            }
        }
    }

    private void loadDefaultScoreboard() {
        File scoreboardsFolder = new File(plugin.getDataFolder(), "scoreboards");
        if (!scoreboardsFolder.exists()) return;

        File configFile = new File(plugin.getDataFolder(), "scoreboard-config.yml");
        FileConfiguration globalConfig = YamlConfiguration.loadConfiguration(configFile);
        String defaultSb = globalConfig.getString("scoreboard.default-scoreboard", "lobby");

        File sbFile = new File(scoreboardsFolder, defaultSb + ".yml");
        if (!sbFile.exists()) {
            File[] files = scoreboardsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null && files.length > 0) {
                sbFile = files[0];
            } else {
                plugin.getLogger().warning("No se encontraron scoreboards en la carpeta scoreboards/");
                return;
            }
        }

        loadScoreboardFile(sbFile);
    }

    private void loadScoreboardFile(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Title
        String titleType = config.getString("title.type", "static");
        titleInterval = config.getInt("title.interval", 2);

        titleFrames.clear();

        if ("build-glow".equals(titleType)) {
            generateBuildGlowFrames(config);
        } else if ("animated-colors".equals(titleType)) {
            // Solo un frame base, los colores se aplican dinamicamente
            String text = config.getString("title.text", "Scoreboard");
            titleFrames.add(translateColors(text));
        } else {
            // static: un solo frame
            String text = config.getString("title.text", "Scoreboard");
            titleFrames.add(translateColors(text));
        }

        if (titleFrames.isEmpty()) {
            titleFrames.add("Scoreboard");
        }

        // Lines
        lines.clear();
        List<Map<?, ?>> linesList = config.getMapList("lines");
        for (Map<?, ?> lineMap : linesList) {
            ScoreboardLine line = new ScoreboardLine();
            Object textObj = lineMap.get("text");
            line.text = textObj != null ? textObj.toString() : "";
            Object condObj = lineMap.get("condition");
            line.condition = condObj != null ? condObj.toString() : null;
            Object animObj = lineMap.get("animated-colors");
            line.animatedColors = animObj != null && Boolean.parseBoolean(animObj.toString());
            lines.add(line);
        }

        plugin.getLogger().info("Scoreboard cargado desde " + file.getName() + ": " + lines.size() + " lineas, " + titleFrames.size() + " frames de titulo");
    }

    /**
     * Genera frames para la animacion build-up + glow del titulo.
     * Fases:
     * 1. Cursor parpadeante ">" (hold-cursor frames)
     * 2. Build-up letra por letra "S", "Se", "Ser", ..., "Servidor Lobby"
     * 3. Texto completo estático (hold-full frames)
     * 4. Glow que barre de izquierda a derecha
     * 5. Vuelve a fase 4 (el glow se repite)
     */
    private void generateBuildGlowFrames(FileConfiguration config) {
        String text = config.getString("title.text", "Servidor Lobby");
        String buildColor = config.getString("title.build-color", "&b&l");
        String buildSecondaryColor = config.getString("title.build-secondary-color", "&3&l");
        int secondaryStart = config.getInt("title.build-secondary-start", text.indexOf(' ') + 1);
        String glowNormal = config.getString("title.glow-normal", "&f&l");
        String glowCenter = config.getString("title.glow-center", "&3&l");
        String glowEdge = config.getString("title.glow-edge", "&b&l");
        int glowSize = config.getInt("title.glow-size", 2);
        int holdCursor = config.getInt("title.hold-cursor", 10);
        int holdFull = config.getInt("title.hold-full", 20);

        // Fase 1: Cursor parpadeante
        for (int i = 0; i < holdCursor; i++) {
            if (i % 4 < 2) {
                titleFrames.add(translateColors("&f&l>"));
            } else {
                titleFrames.add(translateColors("&f&l "));
            }
        }

        // Fase 2: Build-up letra por letra
        for (int len = 1; len <= text.length(); len++) {
            StringBuilder frame = new StringBuilder();
            for (int c = 0; c < len; c++) {
                if (c >= secondaryStart) {
                    frame.append(translateColors(buildSecondaryColor));
                } else {
                    frame.append(translateColors(buildColor));
                }
                frame.append(text.charAt(c));
            }
            titleFrames.add(frame.toString());
        }

        // Fase 3: Texto completo estatico
        StringBuilder fullText = new StringBuilder();
        for (int c = 0; c < text.length(); c++) {
            if (c >= secondaryStart) {
                fullText.append(translateColors(buildSecondaryColor));
            } else {
                fullText.append(translateColors(buildColor));
            }
            fullText.append(text.charAt(c));
        }
        String fullTextStr = fullText.toString();
        for (int i = 0; i < holdFull; i++) {
            titleFrames.add(fullTextStr);
        }

        // Fase 4: Glow que barre de izquierda a derecha
        for (int glowPos = -glowSize; glowPos <= text.length() + glowSize; glowPos++) {
            StringBuilder frame = new StringBuilder();
            for (int c = 0; c < text.length(); c++) {
                int distance = Math.abs(c - glowPos);
                if (distance == 0) {
                    frame.append(translateColors(glowCenter));
                } else if (distance <= glowSize) {
                    frame.append(translateColors(glowEdge));
                } else {
                    frame.append(translateColors(glowNormal));
                }
                frame.append(text.charAt(c));
            }
            titleFrames.add(frame.toString());
        }
    }

    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Avanzar colores animados de lineas
                animatedTickCounter++;
                if (animatedTickCounter >= animatedSpeed) {
                    animatedTickCounter = 0;
                    animatedColorOffset = (animatedColorOffset + 1) % animatedColors.size();
                }

                // Avanzar frame del titulo
                titleTickCounter++;
                if (titleTickCounter >= titleInterval) {
                    titleTickCounter = 0;
                    currentTitleFrame = (currentTitleFrame + 1) % titleFrames.size();
                }

                // Actualizar cada jugador en el main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        // Verificar si el jugador salió de combate
                        checkCombatStatus(player);

                        // Solo actualizar si tiene scoreboard y no está en combate
                        if (playerScoreboards.containsKey(player.getUniqueId())
                            && !scoreboardHidden.getOrDefault(player.getUniqueId(), false)) {
                            updateScoreboard(player);
                        }
                    }
                });
            }
        };
        updateTask.runTaskTimerAsynchronously(plugin, 20L, updateInterval);
    }

    public void stopTask() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
        }
    }

    public void createScoreboard(Player player) {
        if (!enabled) return;

        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("rpg", Criteria.DUMMY,
                LegacyComponentSerializer.legacySection().deserialize(
                        titleFrames.isEmpty() ? "RPG" : titleFrames.get(0)));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Ocultar numeros del lateral
        try {
            obj.numberFormat(NumberFormat.blank());
        } catch (Exception e) {
            plugin.getLogger().warning("No se pudo ocultar los numeros del scoreboard: " + e.getMessage());
        }

        playerScoreboards.put(player.getUniqueId(), sb);
        playerLastEntries.put(player.getUniqueId(), new ArrayList<>());

        player.setScoreboard(sb);

        // Primera actualizacion
        updateScoreboard(player);
    }

    public void removeScoreboard(Player player) {
        playerScoreboards.remove(player.getUniqueId());
        playerLastEntries.remove(player.getUniqueId());
    }

    private void updateScoreboard(Player player) {
        Scoreboard sb = playerScoreboards.get(player.getUniqueId());
        if (sb == null) return;

        Objective obj = sb.getObjective("rpg");
        if (obj == null) return;

        // Actualizar titulo con el frame actual
        if (!titleFrames.isEmpty()) {
            String titleFrame = titleFrames.get(currentTitleFrame);
            obj.displayName(LegacyComponentSerializer.legacySection().deserialize(titleFrame));
        }

        // Construir lineas visibles
        List<String> visibleLines = new ArrayList<>();
        int emptyCount = 0;

        for (ScoreboardLine line : lines) {
            if (line.condition != null && !evaluateCondition(player, line.condition)) {
                continue;
            }

            String text = line.text;
            text = resolvePlaceholders(player, text);

            if (line.animatedColors) {
                text = applyAnimatedColorsWithOffset(text, animatedColorOffset, false);
            } else {
                text = translateColors(text);
            }

            // Manejar lineas vacias duplicadas
            if (text.isEmpty() || text.trim().isEmpty()) {
                emptyCount++;
                StringBuilder emptyLine = new StringBuilder();
                for (int i = 0; i < emptyCount; i++) {
                    emptyLine.append(ChatColor.RESET);
                }
                text = emptyLine.toString();
            }

            if (text.length() > 64) {
                text = text.substring(0, 64);
            }

            visibleLines.add(text);
        }

        // Limitar a 15 lineas
        if (visibleLines.size() > 15) {
            visibleLines = new ArrayList<>(visibleLines.subList(0, 15));
        }

        // Limpiar entries anteriores
        List<String> lastEntries = playerLastEntries.get(player.getUniqueId());
        if (lastEntries != null) {
            for (String entry : lastEntries) {
                sb.resetScores(entry);
            }
        }

        // Colocar lineas (score mas alto = arriba)
        List<String> newEntries = new ArrayList<>();
        for (int i = 0; i < visibleLines.size(); i++) {
            String lineText = visibleLines.get(i);

            while (newEntries.contains(lineText)) {
                lineText = lineText + ChatColor.RESET;
            }

            if (lineText.length() > 64) {
                lineText = lineText.substring(0, 64);
            }

            int score = visibleLines.size() - i;
            obj.getScore(lineText).setScore(score);
            newEntries.add(lineText);
        }

        playerLastEntries.put(player.getUniqueId(), newEntries);
    }

    private boolean evaluateCondition(Player player, String conditionName) {
        Condition cond = conditions.get(conditionName);
        if (cond == null) return true;

        if ("placeholder-equals".equals(cond.type)) {
            String resolved = resolvePlaceholders(player, cond.placeholder);
            return resolved.equalsIgnoreCase(cond.value);
        }

        return true;
    }

    private String resolvePlaceholders(Player player, String text) {
        if (text == null || text.isEmpty()) return text;

        if (placeholderApiAvailable && text.contains("%")) {
            try {
                text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
            } catch (Exception ignored) {}
        }

        text = text.replace("%server_online%", String.valueOf(Bukkit.getOnlinePlayers().size()));

        return text;
    }

    private String applyAnimatedColorsWithOffset(String text, int offset, boolean bold) {
        if (text == null || text.isEmpty()) return text;

        String cleanText = ChatColor.stripColor(translateColors(text));

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < cleanText.length(); i++) {
            int colorIndex = (offset + i) % animatedColors.size();
            String color = translateColors(animatedColors.get(colorIndex));
            result.append(color);
            if (bold) {
                result.append(ChatColor.BOLD);
            }
            result.append(cleanText.charAt(i));
        }

        return result.toString();
    }

    private String translateColors(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    // --- Sistema de combate ---

    /**
     * Marca al jugador como en combate y oculta su scoreboard
     */
    public void enterCombat(Player player) {
        UUID uuid = player.getUniqueId();
        playersInCombat.put(uuid, System.currentTimeMillis());

        // Ocultar scoreboard si no está ya oculto
        if (!scoreboardHidden.getOrDefault(uuid, false)) {
            hideScoreboardForCombat(player);
        }
    }

    /**
     * Verifica si el jugador sigue en combate (llamado en el update task)
     */
    private void checkCombatStatus(Player player) {
        UUID uuid = player.getUniqueId();
        Long lastCombat = playersInCombat.get(uuid);

        if (lastCombat != null) {
            if (System.currentTimeMillis() - lastCombat > COMBAT_TIMEOUT_MS) {
                // Timeout de combate - salir de combate
                exitCombat(player);
            }
        }
    }

    /**
     * Saca al jugador del modo combate y restaura su scoreboard
     */
    public void exitCombat(Player player) {
        UUID uuid = player.getUniqueId();
        playersInCombat.remove(uuid);

        // Restaurar scoreboard si estaba oculto
        if (scoreboardHidden.getOrDefault(uuid, false)) {
            showScoreboardAfterCombat(player);
        }
    }

    /**
     * Oculta el scoreboard durante el combate
     */
    private void hideScoreboardForCombat(Player player) {
        UUID uuid = player.getUniqueId();
        scoreboardHidden.put(uuid, true);

        // Asignar scoreboard vacío
        Scoreboard emptyScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        player.setScoreboard(emptyScoreboard);
    }

    /**
     * Restaura el scoreboard después del combate
     */
    private void showScoreboardAfterCombat(Player player) {
        UUID uuid = player.getUniqueId();
        scoreboardHidden.put(uuid, false);

        // Restaurar el scoreboard del jugador
        Scoreboard sb = playerScoreboards.get(uuid);
        if (sb != null) {
            player.setScoreboard(sb);
            updateScoreboard(player);
        }
    }

    /**
     * Verifica si un jugador está en combate
     */
    public boolean isInCombat(Player player) {
        return playersInCombat.containsKey(player.getUniqueId());
    }

    /**
     * Limpia los datos de combate de un jugador (al desconectar)
     */
    public void cleanupPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        playersInCombat.remove(uuid);
        scoreboardHidden.remove(uuid);
    }

    // --- Inner classes ---

    private static class ScoreboardLine {
        String text = "";
        String condition = null;
        boolean animatedColors = false;
    }

    private static class Condition {
        String type = "placeholder-equals";
        String placeholder = "";
        String value = "";
    }
}
