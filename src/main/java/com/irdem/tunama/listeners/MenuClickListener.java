package com.irdem.tunama.listeners;

import org.bukkit.Bukkit;
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
                    player.sendMessage("§6Clan: [Sistema en desarrollo]");
                    break;
                case 22: // Habilidades
                    openAbilitiesMenu(player, playerData);
                    break;
                case 23: // Misiones
                    openMissionsMenu(player, playerData);
                    break;
                case 29: // Logros
                    openAchievementsMenu(player, playerData);
                    break;
                case 30: // Mejores Jugadores
                    openRankingMenu(player);
                    break;
                case 49: // Cerrar
                    player.closeInventory();
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
                    player.sendMessage("§a✓ Subclase §e" + selectedSubclass + "§a seleccionada");
                    player.closeInventory();
                }
            }
        }

        // Manejar clicks en StatsMenu
        if (holder instanceof com.irdem.tunama.menus.StatsMenu) {
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
}
