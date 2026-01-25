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

public class PlayerMenuListener implements Listener {

    private TunamaRPG plugin;
    private Map<String, ClassMenu> playerClassMenus;
    private Map<String, String> selectedRaces;

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
                .replace("§6", "").replace("§r", "");
            
            String raceId = null;
            for (com.irdem.tunama.data.Race race : plugin.getRaceManager().getAllRaces().values()) {
                if (race.getName().equals(raceName)) {
                    raceId = race.getId();
                    break;
                }
            }
            
            if (raceId != null) {
                selectedRaces.put(player.getName(), raceId);
                player.sendMessage("§aHas seleccionado la raza: §6" + raceName);
                
                ClassMenu classMenu = new ClassMenu(plugin, raceId);
                playerClassMenus.put(player.getName(), classMenu);
                classMenu.open(player);
            }
        }
        
        // Menú de Clases
        else if (event.getInventory().getHolder() instanceof ClassMenu) {
            event.setCancelled(true);
            
            String className = clickedItem.getItemMeta().getDisplayName()
                .replace("§6", "").replace("§r", "");
            
            String classId = null;
            for (com.irdem.tunama.data.RPGClass rpgClass : plugin.getClassManager().getAllClasses().values()) {
                if (rpgClass.getName().equals(className)) {
                    classId = rpgClass.getId();
                    break;
                }
            }
            
            if (classId != null && selectedRaces.containsKey(player.getName())) {
                String raceId = selectedRaces.get(player.getName());
                
                player.sendMessage("§aHas seleccionado la clase: §6" + className);
                player.sendMessage("§e¡Bienvenido a TunamaRPG, " + player.getName() + "!");
                
                // Guardar datos del jugador en la BD
                plugin.getDatabaseManager().saveNewPlayer(player.getUniqueId(), player.getName(), raceId, classId);
                
                // Limpiar datos temporales
                selectedRaces.remove(player.getName());
                playerClassMenus.remove(player.getName());
                
                player.closeInventory();
            }
        }
    }
}
