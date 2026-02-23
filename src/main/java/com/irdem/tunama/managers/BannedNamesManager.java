package com.irdem.tunama.managers;

import com.irdem.tunama.TunamaRPG;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de nombres prohibidos para personajes
 */
public class BannedNamesManager {

    private final TunamaRPG plugin;
    private FileConfiguration config;
    private File configFile;
    private List<String> bannedNames;

    public BannedNamesManager(TunamaRPG plugin) {
        this.plugin = plugin;
        this.bannedNames = new ArrayList<>();
        loadConfig();
    }

    /**
     * Carga la configuración de nombres prohibidos
     */
    public void loadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "nombres-prohibidos.yml");
        }

        // Crear archivo si no existe
        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                plugin.saveResource("nombres-prohibidos.yml", false);
            } catch (Exception e) {
                plugin.getLogger().severe("No se pudo crear nombres-prohibidos.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Cargar valores por defecto del JAR
        try (InputStream defConfigStream = plugin.getResource("nombres-prohibidos.yml")) {
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)
                );
                config.setDefaults(defConfig);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudieron cargar los valores por defecto de nombres-prohibidos.yml");
        }

        // Cargar lista de nombres prohibidos
        bannedNames.clear();
        List<String> names = config.getStringList("nombres-prohibidos");
        if (names != null) {
            bannedNames.addAll(names);
        }

        plugin.getLogger().info("Cargados " + bannedNames.size() + " nombres prohibidos");
    }

    /**
     * Recarga la configuración
     */
    public void reloadConfig() {
        loadConfig();
    }

    /**
     * Guarda la configuración
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar nombres-prohibidos.yml: " + e.getMessage());
        }
    }

    /**
     * Verifica si un nombre está prohibido
     * @param name Nombre a verificar
     * @return true si el nombre está prohibido, false si está permitido
     */
    public boolean isNameBanned(String name) {
        if (name == null || name.isEmpty()) {
            return true;
        }

        String nameLower = name.toLowerCase().trim();

        // Verificar si el nombre contiene alguna palabra prohibida
        for (String bannedName : bannedNames) {
            if (nameLower.contains(bannedName.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Verifica si un nombre es válido
     * @param name Nombre a verificar
     * @return true si el nombre es válido, false si no lo es
     */
    public boolean isNameValid(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        // Verificar longitud (mínimo 3, máximo 16 caracteres)
        if (name.length() < 3 || name.length() > 16) {
            return false;
        }

        // Verificar que solo contenga letras, números y guiones bajos
        if (!name.matches("^[a-zA-Z0-9_]+$")) {
            return false;
        }

        // Verificar si está prohibido
        if (isNameBanned(name)) {
            return false;
        }

        return true;
    }

    /**
     * Obtiene un mensaje de error apropiado para un nombre inválido
     * @param name Nombre a verificar
     * @return Mensaje de error
     */
    public String getErrorMessage(String name) {
        if (name == null || name.isEmpty()) {
            return "§c✗ Debes ingresar un nombre";
        }

        if (name.length() < 3) {
            return "§c✗ El nombre debe tener al menos 3 caracteres";
        }

        if (name.length() > 16) {
            return "§c✗ El nombre no puede tener más de 16 caracteres";
        }

        if (!name.matches("^[a-zA-Z0-9_]+$")) {
            return "§c✗ El nombre solo puede contener letras, números y guiones bajos";
        }

        if (isNameBanned(name)) {
            return "§c✗ Ese nombre no está disponible";
        }

        return "§c✗ Nombre inválido";
    }

    /**
     * Obtiene la lista de nombres prohibidos
     * @return Lista de nombres prohibidos
     */
    public List<String> getBannedNames() {
        return new ArrayList<>(bannedNames);
    }

    /**
     * Añade un nombre a la lista de prohibidos
     * @param name Nombre a añadir
     */
    public void addBannedName(String name) {
        if (name != null && !name.isEmpty() && !bannedNames.contains(name)) {
            bannedNames.add(name);
            config.set("nombres-prohibidos", bannedNames);
            saveConfig();
        }
    }

    /**
     * Elimina un nombre de la lista de prohibidos
     * @param name Nombre a eliminar
     */
    public void removeBannedName(String name) {
        if (bannedNames.remove(name)) {
            config.set("nombres-prohibidos", bannedNames);
            saveConfig();
        }
    }
}
