package com.irdem.tunama.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.PlayerData;
import java.util.ArrayList;
import java.util.List;

public class SubclassSelectionMenu implements InventoryHolder {
    private Inventory inventory;
    private TunamaRPG plugin;
    private Player player;
    private PlayerData playerData;

    public SubclassSelectionMenu(TunamaRPG plugin, Player player, PlayerData playerData) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = playerData;
        this.inventory = Bukkit.createInventory(this, 54, "§5Seleccionar Subclase");
        setupItems();
    }

    private void setupItems() {
        // Verificar nivel
        if (playerData.getLevel() < 30) {
            inventory.setItem(22, createItem(Material.BARRIER, "§cNivel Insuficiente", 
                "§7Tu nivel: §f" + playerData.getLevel(),
                "§7Requerido: §f30",
                "§7Sube de nivel para desbloquear subclases"
            ));
            return;
        }

        // Obtener la clase del jugador
        String playerClass = playerData.getPlayerClass();
        if (playerClass == null || playerClass.isEmpty()) {
            inventory.setItem(22, createItem(Material.BARRIER, "§cSin Clase", 
                "§7Primero debes seleccionar una clase"
            ));
            return;
        }

        // Obtener subclases de la clase
        java.util.Set<String> subclassesSet = plugin.getClassManager().getClass(playerClass.toLowerCase()).getSubclasses();
        java.util.List<String> subclasses = new java.util.ArrayList<>(subclassesSet);
        int slot = 10;

        if (subclasses != null && !subclasses.isEmpty()) {
            for (String subclassName : subclasses) {
                if (plugin.getSubclassManager().isValidSubclass(subclassName)) {
                    inventory.setItem(slot, createItem(Material.PURPLE_DYE, "§5" + subclassName, 
                        "§7Haz clic para seleccionar"
                    ));
                    slot++;
                    if (slot > 35) break; // Limitar a los slots disponibles
                }
            }
        }

        // Botón de volver
        inventory.setItem(49, createItem(Material.BARRIER, "§cVolver", 
            "§7Haz clic para volver al menú anterior"
        ));
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }

    public String getPlayerClass() {
        return playerData.getPlayerClass();
    }
}
