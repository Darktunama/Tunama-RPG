package com.irdem.tunama.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.menus.ClassMenu;
import com.irdem.tunama.menus.RaceMenu;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMenuListener implements Listener {

    private TunamaRPG plugin;
    private Map<UUID, ClassMenu> playerClassMenus;
    private Map<UUID, String> selectedRaces;

    public PlayerMenuListener(TunamaRPG plugin) {
        this.plugin = plugin;
        this.playerClassMenus = new HashMap<>();
        this.selectedRaces = new HashMap<>();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        // Verificar si es un menú del plugin
        if (!(event.getInventory().getHolder() instanceof RaceMenu ||
              event.getInventory().getHolder() instanceof ClassMenu)) {
            return;
        }

        // Menú de Razas
        if (event.getInventory().getHolder() instanceof RaceMenu) {
            event.setCancelled(true);

            String raceName = clickedItem.getItemMeta().getDisplayName()
                .replace("§6", "").replace("§r", "").trim();

            // Buscar el ID de la raza por su nombre
            String raceId = getRaceIdByName(raceName);

            if (raceId != null && plugin.getRaceManager().isValidRace(raceId)) {
                selectedRaces.put(player.getUniqueId(), raceId);
                player.sendMessage("§aHas seleccionado la raza: §6" + raceName);

                ClassMenu classMenu = new ClassMenu(plugin, raceId);
                playerClassMenus.put(player.getUniqueId(), classMenu);
                classMenu.open(player);
            }
        }

        // Menú de Clases
        else if (event.getInventory().getHolder() instanceof ClassMenu) {
            event.setCancelled(true);

            String className = clickedItem.getItemMeta().getDisplayName()
                .replace("§6", "").replace("§r", "").trim();

            // Buscar el ID de la clase por su nombre
            String classId = getClassIdByName(className);

            if (classId != null && plugin.getClassManager().isValidClass(classId) && selectedRaces.containsKey(player.getUniqueId())) {
                String raceId = selectedRaces.get(player.getUniqueId());

                // Verificar una vez más que la clase no está restringida (validación de seguridad)
                com.irdem.tunama.data.Race race = plugin.getRaceManager().getRace(raceId);
                if (race != null && race.isClassRestricted(classId)) {
                    player.sendMessage("§c¡Esta clase está bloqueada para tu raza!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                player.sendMessage("§aHas seleccionado la clase: §6" + className);
                player.closeInventory();

                // Registrar el personaje pendiente en el ChatInputListener
                com.irdem.tunama.listeners.ChatInputListener chatListener = plugin.getChatInputListener();
                if (chatListener != null) {
                    chatListener.registerPendingCharacter(player, raceId, classId);
                }

                // Limpiar datos temporales
                selectedRaces.remove(player.getUniqueId());
                playerClassMenus.remove(player.getUniqueId());
            }
        }
    }

    /**
     * Busca el ID de una raza por su nombre
     */
    private String getRaceIdByName(String raceName) {
        for (java.util.Map.Entry<String, com.irdem.tunama.data.Race> entry : plugin.getRaceManager().getAllRaces().entrySet()) {
            if (entry.getValue().getName().equalsIgnoreCase(raceName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Busca el ID de una clase por su nombre
     */
    private String getClassIdByName(String className) {
        for (java.util.Map.Entry<String, com.irdem.tunama.data.RPGClass> entry : plugin.getClassManager().getAllClasses().entrySet()) {
            if (entry.getValue().getName().equalsIgnoreCase(className)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
