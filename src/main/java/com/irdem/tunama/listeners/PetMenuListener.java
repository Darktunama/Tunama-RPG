package com.irdem.tunama.listeners;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.PetType;
import com.irdem.tunama.data.PlayerData;
import com.irdem.tunama.menus.MainMenuGUI;
import com.irdem.tunama.menus.PetCommandMenu;
import com.irdem.tunama.menus.PetMenu;
import com.irdem.tunama.menus.PetShopMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listener para eventos de menú de mascotas
 */
public class PetMenuListener implements Listener {

    private final TunamaRPG plugin;

    public PetMenuListener(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof PetMenu) {
            handlePetMenuClick(event, (PetMenu) holder);
        } else if (holder instanceof PetCommandMenu) {
            handlePetCommandMenuClick(event, (PetCommandMenu) holder);
        } else if (holder instanceof PetShopMenu) {
            handlePetShopMenuClick(event, (PetShopMenu) holder);
        }
    }

    /**
     * Maneja clicks en el menú principal de mascotas
     */
    private void handlePetMenuClick(InventoryClickEvent event, PetMenu menu) {
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // Clic fuera del inventario
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        // Verificar si es un slot de mascotas (guardadas o activas)
        if (isStoredSlot(slot) || isActiveSlot(slot)) {
            boolean selected = menu.selectPetFromSlot(slot);
            if (selected) {
                player.sendMessage("§a✓ Mascota seleccionada");
            }
            return;
        }

        // Botón de acción (invocar/guardar)
        if (slot == PetMenu.getActionSlot()) {
            menu.executeAction();
            return;
        }

        // Botón de comandos
        if (slot == PetMenu.getCommandsSlot()) {
            // Verificar que tenga mascotas activas
            if (plugin.getPetManager().getActivePets(player).isEmpty()) {
                player.sendMessage("§c✗ No tienes mascotas activas");
                return;
            }

            player.closeInventory();
            PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
            PetCommandMenu commandMenu = new PetCommandMenu(plugin, player, playerData, menu.getSelectedPet());
            commandMenu.open();
            return;
        }

        // Botón de tienda
        if (slot == PetMenu.getShopSlot()) {
            player.closeInventory();
            PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
            PetShopMenu shopMenu = new PetShopMenu(plugin, player, playerData);
            shopMenu.open();
            return;
        }

        // Botón de resurrección
        if (slot == PetMenu.getResurrectSlot()) {
            if (menu.countDeadPets() == 0) {
                player.sendMessage("§c✗ No tienes mascotas muertas");
                return;
            }

            com.irdem.tunama.data.Pet deadPet = menu.getFirstDeadPet();
            if (deadPet != null) {
                boolean resurrected = menu.resurrectPet(deadPet);
                if (resurrected) {
                    menu.refresh();
                }
            }
            return;
        }

        // Botón de liberar mascota
        if (slot == PetMenu.getReleaseSlot()) {
            if (menu.getSelectedPet() == null) {
                player.sendMessage("§c✗ Selecciona una mascota primero");
                return;
            }

            boolean released = menu.releasePet();
            if (released) {
                menu.refresh();
            }
            return;
        }

        // Navegación de páginas
        if (slot == PetMenu.getPrevPageSlot()) {
            int currentPage = menu.getCurrentPage();
            if (currentPage > 0) {
                player.closeInventory();
                PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
                PetMenu newMenu = new PetMenu(plugin, player, playerData, currentPage - 1);
                newMenu.open();
            }
            return;
        }

        if (slot == PetMenu.getNextPageSlot()) {
            int currentPage = menu.getCurrentPage();
            int totalPages = menu.getTotalPages();
            if (currentPage < totalPages - 1) {
                player.closeInventory();
                PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
                PetMenu newMenu = new PetMenu(plugin, player, playerData, currentPage + 1);
                newMenu.open();
            }
            return;
        }

        // Botón de volver
        if (slot == PetMenu.getBackSlot()) {
            player.closeInventory();
            PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
            MainMenuGUI mainMenu = new MainMenuGUI(plugin, player, playerData);
            mainMenu.open();
            return;
        }
    }

    /**
     * Maneja clicks en el menú de comandos de mascotas
     */
    private void handlePetCommandMenuClick(InventoryClickEvent event, PetCommandMenu menu) {
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // Clic fuera del inventario
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        // Slots de comandos
        if (slot == PetCommandMenu.getFollowSlot() ||
            slot == PetCommandMenu.getStaySlot() ||
            slot == PetCommandMenu.getAttackSlot() ||
            slot == PetCommandMenu.getDefendSlot() ||
            slot == PetCommandMenu.getAggressiveSlot()) {

            boolean executed = menu.executeCommand(slot);
            if (executed) {
                // Refrescar el menú para mostrar el nuevo comando
                player.closeInventory();
                menu.open();
            }
            return;
        }

        // Botón para aplicar a todas las mascotas
        if (slot == PetCommandMenu.getAllPetsSlot()) {
            // Cambiar a modo "todas las mascotas"
            player.closeInventory();
            PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
            PetCommandMenu newMenu = new PetCommandMenu(plugin, player, playerData, null);
            newMenu.open();
            return;
        }

        // Botón de volver
        if (slot == PetCommandMenu.getBackSlot()) {
            player.closeInventory();
            PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
            PetMenu petMenu = new PetMenu(plugin, player, playerData);
            petMenu.open();
            return;
        }
    }

    /**
     * Maneja clicks en la tienda de mascotas
     */
    private void handlePetShopMenuClick(InventoryClickEvent event, PetShopMenu menu) {
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // Clic fuera del inventario
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        // Botón de volver
        if (slot == PetShopMenu.getBackSlot()) {
            player.closeInventory();
            PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
            PetMenu petMenu = new PetMenu(plugin, player, playerData);
            petMenu.open();
            return;
        }

        // Navegación de páginas
        if (slot == PetShopMenu.getPrevPageSlot()) {
            int currentPage = menu.getCurrentPage();
            if (currentPage > 0) {
                player.closeInventory();
                PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
                PetShopMenu newMenu = new PetShopMenu(menu.getPlugin(), player, playerData, currentPage - 1);
                newMenu.open();
            }
            return;
        }

        if (slot == PetShopMenu.getNextPageSlot()) {
            player.closeInventory();
            PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
            PetShopMenu newMenu = new PetShopMenu(menu.getPlugin(), player, playerData, menu.getCurrentPage() + 1);
            newMenu.open();
            return;
        }

        // Verificar si es un slot de mascota para comprar
        PetType petType = menu.getPetTypeFromSlot(slot);
        if (petType != null) {
            boolean bought = menu.buyPet(petType);
            if (bought) {
                // Refrescar el menú para actualizar el balance
                player.closeInventory();
                PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
                PetShopMenu newMenu = new PetShopMenu(menu.getPlugin(), player, playerData, menu.getCurrentPage());
                newMenu.open();
            }
        }
    }

    /**
     * Verifica si un slot es de mascotas guardadas
     */
    private boolean isStoredSlot(int slot) {
        for (int s : PetMenu.getStoredSlots()) {
            if (s == slot) return true;
        }
        return false;
    }

    /**
     * Verifica si un slot es de mascotas activas
     */
    private boolean isActiveSlot(int slot) {
        for (int s : PetMenu.getActiveSlots()) {
            if (s == slot) return true;
        }
        return false;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Aquí se puede agregar lógica adicional si es necesario al cerrar el menú
    }
}
