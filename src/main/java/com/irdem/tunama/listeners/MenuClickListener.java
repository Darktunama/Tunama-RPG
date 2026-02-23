package com.irdem.tunama.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.menus.MainMenuGUI;
import com.irdem.tunama.menus.SubclassSelectionMenu;
import com.irdem.tunama.data.PlayerData;

public class MenuClickListener implements Listener {
    private TunamaRPG plugin;

    public MenuClickListener(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        InventoryHolder holder = event.getInventory().getHolder();

        // Manejar clicks en MainMenuGUI
        if (holder instanceof MainMenuGUI) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
            if (playerData == null) return;

            switch (slot) {
                case 12: // Razas
                    openRaceMenu(player);
                    break;
                case 13: // Clases
                    openClassMenu(player);
                    break;
                case 14: // Subclases
                    openSubclassMenu(player, playerData);
                    break;
                case 20: // Estadísticas
                    openStatsMenu(player, playerData);
                    break;
                case 21: // Clan
                    openClanMenu(player);
                    break;
                case 22: // Habilidades
                    openAbilitiesMenu(player, playerData);
                    break;
                case 23: // Misiones
                    openMissionsMenu(player, playerData);
                    break;
                case 24: // Equipo
                    openEquipmentMenu(player, playerData);
                    break;
                case 29: // Logros
                    openAchievementsMenu(player, playerData);
                    break;
                case 30: // Mejores Jugadores
                    openRankingMenu(player);
                    break;
                case 38: // Mascotas
                    openPetsMenu(player, playerData);
                    break;
                case 47: // Renacer
                    player.closeInventory();
                    player.performCommand("rpg renacer");
                    break;
                case 49: // Cerrar
                    player.closeInventory();
                    break;
                case 51: // Personajes
                    openCharacterSelectionMenu(player);
                    break;
            }
        }

        // Manejar clicks en SubclassSelectionMenu
        if (holder instanceof SubclassSelectionMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            SubclassSelectionMenu menu = (SubclassSelectionMenu) holder;
            PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
            if (playerData == null) return;

            if (slot == 49) { // Botón Volver
                MainMenuGUI mainMenu = new MainMenuGUI(plugin, player, playerData);
                mainMenu.open();
            } else if (slot >= 10 && slot <= 35) {
                String selectedSubclass = getSubclassFromSlot(player, slot, menu.getPlayerClass());
                if (selectedSubclass != null) {
                    playerData.setSubclass(selectedSubclass);
                    plugin.getDatabaseManager().updatePlayerData(playerData);
                    // Obtener el nombre de la subclase para mostrar
                    com.irdem.tunama.data.Subclass subclassData = plugin.getSubclassManager().getSubclass(selectedSubclass);
                    String displayName = subclassData != null ? subclassData.getName() : selectedSubclass;
                    player.sendMessage("§a✓ Subclase §e" + displayName + "§a seleccionada");
                    player.closeInventory();
                }
            }
        }

        // Manejar clicks en StatsMenu
        if (holder instanceof com.irdem.tunama.menus.StatsMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
            if (playerData == null) return;

            // Shift+Click añade 5 puntos, click normal añade 1
            boolean isShiftClick = event.isShiftClick();
            int pointsToAdd = isShiftClick ? 5 : 1;

            if (slot == 49) { // Botón Volver
                MainMenuGUI mainMenu = new MainMenuGUI(plugin, player, playerData);
                mainMenu.open();
            } else if (slot == 10) { // Vida
                distributeStatPoints(player, playerData, "health", pointsToAdd);
            } else if (slot == 11) { // Fuerza
                distributeStatPoints(player, playerData, "strength", pointsToAdd);
            } else if (slot == 12) { // Agilidad
                distributeStatPoints(player, playerData, "agility", pointsToAdd);
            } else if (slot == 13) { // Inteligencia
                distributeStatPoints(player, playerData, "intelligence", pointsToAdd);
            } else if (slot == 19) { // Poder Sagrado
                distributeStatPoints(player, playerData, "sacred", pointsToAdd);
            } else if (slot == 20) { // Poder Corrupto
                distributeStatPoints(player, playerData, "corrupt", pointsToAdd);
            } else if (slot == 21) { // Poder Naturaleza
                distributeStatPoints(player, playerData, "nature", pointsToAdd);
            }
        }

        // Manejar clicks en MissionsMenu
        if (holder instanceof com.irdem.tunama.menus.MissionsMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            if (slot == 49) { // Botón Volver
                PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
                if (playerData != null) {
                    MainMenuGUI mainMenu = new MainMenuGUI(plugin, player, playerData);
                    mainMenu.open();
                }
            }
        }

        // Manejar clicks en AchievementsMenu
        if (holder instanceof com.irdem.tunama.menus.AchievementsMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            if (slot == 49) { // Botón Volver
                PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
                if (playerData != null) {
                    MainMenuGUI mainMenu = new MainMenuGUI(plugin, player, playerData);
                    mainMenu.open();
                }
            }
        }

        // Manejar clicks en RankingMenu
        if (holder instanceof com.irdem.tunama.menus.RankingMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            if (slot == 49) { // Botón Volver
                PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
                if (playerData != null) {
                    MainMenuGUI mainMenu = new MainMenuGUI(plugin, player, playerData);
                    mainMenu.open();
                }
            }
        }

        // Manejar clicks en AbilitiesMenu
        if (holder instanceof com.irdem.tunama.menus.AbilitiesMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            if (slot == 49) { // Botón Volver
                PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
                if (playerData != null) {
                    MainMenuGUI mainMenu = new MainMenuGUI(plugin, player, playerData);
                    mainMenu.open();
                }
            }
        }

        // Manejar clicks en EquipmentMenu (el listener específico maneja la lógica)
        if (holder instanceof com.irdem.tunama.menus.EquipmentMenu) {
            // El EquipmentMenuListener manejará todos los clicks
        }

        // Manejar clicks en CharacterSelectionMenu
        if (holder instanceof com.irdem.tunama.menus.CharacterSelectionMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            // Ignorar clics fuera del inventario
            if (slot < 0 || slot >= 54) return;

            // Botón de volver (slot 45)
            if (slot == 45) {
                PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
                if (playerData != null) {
                    player.closeInventory();
                    MainMenuGUI mainMenu = new MainMenuGUI(plugin, player, playerData);
                    mainMenu.open();
                } else {
                    player.sendMessage("§c✗ No tienes un personaje activo. Crea o selecciona uno primero.");
                    player.closeInventory();
                }
                return;
            }

            // Ignorar el botón de información
            if (slot == 49) return;

            int characterSlot = com.irdem.tunama.menus.CharacterSelectionMenu.getCharacterSlotFromInventory(slot);
            if (characterSlot > 0) {
                handleCharacterSelection(player, characterSlot);
            }
        }
    }

    private void openRaceMenu(Player player) {
        com.irdem.tunama.menus.RaceMenu raceMenu = new com.irdem.tunama.menus.RaceMenu(plugin);
        raceMenu.open(player);
    }

    private void openClassMenu(Player player) {
        com.irdem.tunama.menus.ClassMenu classMenu = new com.irdem.tunama.menus.ClassMenu(plugin, "");
        classMenu.open(player);
    }

    private void openSubclassMenu(Player player, PlayerData playerData) {
        if (playerData.getLevel() < 30) {
            player.sendMessage("§c✗ Necesitas nivel 30 para desbloquear subclases. Nivel actual: §f" + playerData.getLevel());
            return;
        }

        if (playerData.getPlayerClass() == null || playerData.getPlayerClass().isEmpty()) {
            player.sendMessage("§c✗ Primero debes seleccionar una clase");
            return;
        }

        SubclassSelectionMenu menu = new SubclassSelectionMenu(plugin, player, playerData);
        menu.open();
    }

    private void openStatsMenu(Player player, PlayerData playerData) {
        com.irdem.tunama.menus.StatsMenu statsMenu = new com.irdem.tunama.menus.StatsMenu(plugin, player, playerData);
        statsMenu.open();
    }

    private void openMissionsMenu(Player player, PlayerData playerData) {
        com.irdem.tunama.menus.MissionsMenu missionsMenu = new com.irdem.tunama.menus.MissionsMenu(plugin, player, playerData);
        missionsMenu.open();
    }

    private void openAchievementsMenu(Player player, PlayerData playerData) {
        com.irdem.tunama.menus.AchievementsMenu achievementsMenu = new com.irdem.tunama.menus.AchievementsMenu(plugin, player, playerData);
        achievementsMenu.open();
    }

    private void openRankingMenu(Player player) {
        com.irdem.tunama.menus.RankingMenu rankingMenu = new com.irdem.tunama.menus.RankingMenu(plugin, player);
        rankingMenu.open();
    }

    private void openAbilitiesMenu(Player player, PlayerData playerData) {
        com.irdem.tunama.menus.AbilitiesMenu abilitiesMenu = new com.irdem.tunama.menus.AbilitiesMenu(plugin, player, playerData);
        abilitiesMenu.open();
    }

    private void openEquipmentMenu(Player player, PlayerData playerData) {
        com.irdem.tunama.menus.EquipmentMenu equipmentMenu = new com.irdem.tunama.menus.EquipmentMenu(plugin, player, playerData);
        equipmentMenu.open();
    }

    private void openCharacterSelectionMenu(Player player) {
        com.irdem.tunama.menus.CharacterSelectionMenu menu = new com.irdem.tunama.menus.CharacterSelectionMenu(plugin);
        menu.open(player);
    }

    private void openClanMenu(Player player) {
        com.irdem.tunama.data.Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        if (clan == null) {
            // No tiene clan - mostrar menú de creación
            com.irdem.tunama.menus.clan.ClanNoClanMenu menu = new com.irdem.tunama.menus.clan.ClanNoClanMenu(plugin);
            menu.open(player);
        } else {
            // Tiene clan - mostrar menú principal del clan
            com.irdem.tunama.menus.clan.ClanMainMenu menu = new com.irdem.tunama.menus.clan.ClanMainMenu(plugin, clan);
            menu.open(player);
        }
    }

    private void openPetsMenu(Player player, PlayerData playerData) {
        if (!plugin.getPetManager().canHavePets(player)) {
            player.sendMessage("§c✗ Tu clase no tiene acceso a mascotas");
            return;
        }
        com.irdem.tunama.menus.PetMenu petMenu = new com.irdem.tunama.menus.PetMenu(plugin, player, playerData);
        petMenu.open();
    }

    private String getSubclassFromSlot(Player player, int slot, String playerClass) {
        if (playerClass == null) return null;

        java.util.Set<String> subclassesSet = plugin.getClassManager().getClass(playerClass.toLowerCase()).getSubclasses();
        java.util.List<String> subclasses = new java.util.ArrayList<>(subclassesSet);
        if (subclasses == null || subclasses.isEmpty()) return null;

        int index = slot - 10;
        if (index >= 0 && index < subclasses.size()) {
            return subclasses.get(index);
        }
        return null;
    }
    
    private void distributeStatPoint(Player player, PlayerData playerData, String statType) {
        distributeStatPoints(player, playerData, statType, 1);
    }

    private void distributeStatPoints(Player player, PlayerData playerData, String statType, int amount) {
        if (playerData.getStatPoints() <= 0) {
            player.sendMessage("§c✗ No tienes puntos de estadística disponibles");
            return;
        }

        // Ajustar la cantidad si no hay suficientes puntos
        int pointsToUse = Math.min(amount, playerData.getStatPoints());

        boolean success = false;
        String statName = "";

        switch (statType.toLowerCase()) {
            case "health":
                playerData.getStats().addHealth(pointsToUse);
                statName = "§cVida";
                success = true;

                // Actualizar el MAX_HEALTH del jugador
                updatePlayerMaxHealth(player, playerData);
                break;
            case "strength":
                playerData.getStats().addStrength(pointsToUse);
                statName = "§cFuerza";
                success = true;
                break;
            case "agility":
                playerData.getStats().addAgility(pointsToUse);
                statName = "§eAgilidad";
                success = true;
                break;
            case "intelligence":
                playerData.getStats().addIntelligence(pointsToUse);
                statName = "§5Inteligencia";
                success = true;

                // Actualizar las barras para reflejar el nuevo mana
                plugin.getPlayerBarsManager().updateBars(player);
                break;
            case "sacred":
                playerData.getStats().addSacredPower(pointsToUse);
                statName = "§6Poder Sagrado";
                success = true;
                break;
            case "corrupt":
                playerData.getStats().addCorruptPower(pointsToUse);
                statName = "§4Poder Corrupto";
                success = true;
                break;
            case "nature":
                playerData.getStats().addNaturePower(pointsToUse);
                statName = "§2Poder de la Naturaleza";
                success = true;
                break;
        }

        if (success) {
            for (int i = 0; i < pointsToUse; i++) {
                playerData.useStatPoint();
            }
            plugin.getDatabaseManager().updatePlayerData(playerData);
            player.sendMessage("§a✓ Has aumentado tu " + statName + " §aen " + pointsToUse + " punto" + (pointsToUse > 1 ? "s" : ""));
            player.sendMessage("§7Puntos restantes: §f" + playerData.getStatPoints());

            // Actualizar el menú
            com.irdem.tunama.menus.StatsMenu statsMenu = new com.irdem.tunama.menus.StatsMenu(plugin, player, playerData);
            statsMenu.open();
        }
    }

    /**
     * Actualiza el MAX_HEALTH del jugador basado en sus estadísticas
     */
    private void updatePlayerMaxHealth(Player player, PlayerData playerData) {
        // Mantener MAX_HEALTH en 20 (vanilla). La vida RPG se muestra en la barra de boss.
        org.bukkit.attribute.AttributeInstance maxHealthAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(20.0);
            // Curar al jugador completamente al aumentar vida
            player.setHealth(20.0);
        }
        // Actualizar las barras para reflejar la nueva vida RPG
        plugin.getPlayerBarsManager().updateBars(player);
    }

    private void handleCharacterSelection(Player player, int characterSlot) {
        com.irdem.tunama.data.CharacterManager charManager = plugin.getDatabaseManager().getCharacterManager();
        int maxCharacters = charManager.getMaxCharacters(player);

        // Verificar que el slot está dentro del límite permitido
        if (characterSlot > maxCharacters) {
            player.sendMessage("§c✗ No tienes permiso para usar este slot");
            player.sendMessage("§7Máximo de personajes permitidos: §f" + maxCharacters);
            return;
        }

        int currentActiveSlot = charManager.getActiveSlot(player.getUniqueId());

        // Si es el mismo slot activo, ignorar
        if (characterSlot == currentActiveSlot) {
            player.sendMessage("§e⚠ Este ya es tu personaje activo");
            return;
        }

        boolean characterExists = charManager.characterExists(player.getUniqueId(), characterSlot);

        if (characterExists) {
            // Cambiar a este personaje
            charManager.setActiveSlot(player.getUniqueId(), characterSlot);

            // Limpiar caché de maná y bindings del personaje anterior
            PlayerData.clearManaCache(player.getUniqueId());
            com.irdem.tunama.listeners.AbilitiesMenuListener.clearBindings(player.getUniqueId());

            // Recargar barra de habilidades del nuevo personaje desde la base de datos
            com.irdem.tunama.listeners.AbilityBarListener.reloadAbilitySlots(player.getUniqueId());
            com.irdem.tunama.listeners.AbilityBarListener.forceDisableAbilityMode(player);

            // Guardar mascotas del personaje anterior
            plugin.getPetManager().dismissAllPets(player);

            // Cargar datos del nuevo personaje
            PlayerData newCharacterData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
            if (newCharacterData != null) {
                player.sendMessage("§a✓ Has cambiado al personaje: §f" + newCharacterData.getUsername());
                player.sendMessage("§7Nivel " + newCharacterData.getLevel() + " §8| §7" + newCharacterData.getRace() + " - " + newCharacterData.getPlayerClass());

                // Cambiar display name del jugador usando Adventure API
                if (newCharacterData.getUsername() != null && !newCharacterData.getUsername().isEmpty()) {
                    player.displayName(net.kyori.adventure.text.Component.text(newCharacterData.getUsername()));
                    player.playerListName(net.kyori.adventure.text.Component.text(newCharacterData.getUsername()));
                }

                // Actualizar la vida máxima del jugador basándose en las estadísticas del nuevo personaje
                updatePlayerMaxHealth(player, newCharacterData);

                player.closeInventory();
            } else {
                player.sendMessage("§c✗ Error al cargar el personaje");
            }
        } else {
            // Crear nuevo personaje en este slot
            charManager.setActiveSlot(player.getUniqueId(), characterSlot);

            // Limpiar habilidades del personaje anterior
            PlayerData.clearManaCache(player.getUniqueId());
            com.irdem.tunama.listeners.AbilitiesMenuListener.clearBindings(player.getUniqueId());
            com.irdem.tunama.listeners.AbilityBarListener.clearAbilitySlots(player.getUniqueId());
            com.irdem.tunama.listeners.AbilityBarListener.forceDisableAbilityMode(player);

            // Guardar mascotas del personaje anterior
            plugin.getPetManager().dismissAllPets(player);

            player.sendMessage("§a✓ Slot " + characterSlot + " seleccionado");
            player.sendMessage("§7Creando nuevo personaje...");
            player.closeInventory();

            // Abrir menú de creación de personaje (razas)
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                com.irdem.tunama.menus.RaceMenu raceMenu = new com.irdem.tunama.menus.RaceMenu(plugin);
                raceMenu.open(player);
            }, 5L);
        }
    }
}
