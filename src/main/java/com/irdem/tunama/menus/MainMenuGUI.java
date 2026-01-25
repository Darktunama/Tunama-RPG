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

public class MainMenuGUI implements InventoryHolder {
    private Inventory inventory;
    private TunamaRPG plugin;
    private Player player;
    private PlayerData playerData;

    public MainMenuGUI(TunamaRPG plugin, Player player, PlayerData playerData) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = playerData;
        this.inventory = Bukkit.createInventory(this, 54, "§6Menu Principal RPG");
        setupItems();
    }

    private void setupItems() {
        // Obtener nombres de raza y clase
        String raceName = "No asignada";
        String className = "No asignada";
        
        if (playerData.getRace() != null && !playerData.getRace().isEmpty()) {
            com.irdem.tunama.data.Race race = plugin.getRaceManager().getRace(playerData.getRace());
            if (race != null) {
                raceName = race.getName();
            }
        }
        
        if (playerData.getPlayerClass() != null && !playerData.getPlayerClass().isEmpty()) {
            com.irdem.tunama.data.RPGClass rpgClass = plugin.getClassManager().getClass(playerData.getPlayerClass());
            if (rpgClass != null) {
                className = rpgClass.getName();
            }
        }
        
        // Info del Jugador (slot 11)
        inventory.setItem(11, createItem(Material.PLAYER_HEAD, "§6Información", 
            "§7Jugador: §f" + playerData.getUsername(),
            "§7Raza: §f" + raceName,
            "§7Clase: §f" + className,
            "§7Nivel: §f" + playerData.getLevel(),
            "§7EXP: §f" + playerData.getExperience()
        ));

        // Razas (slot 12) - Solo si no tiene raza
        if (playerData.getRace() == null || playerData.getRace().isEmpty()) {
            inventory.setItem(12, createItem(Material.YELLOW_DYE, "§eRazas", 
                "§7Haz clic para seleccionar tu raza"
            ));
        }

        // Clases (slot 13) - Solo si no tiene clase
        if (playerData.getPlayerClass() == null || playerData.getPlayerClass().isEmpty()) {
            inventory.setItem(13, createItem(Material.BLUE_DYE, "§9Clases", 
                "§7Haz clic para seleccionar tu clase"
            ));
        }

        // Subclases (slot 14)
        inventory.setItem(14, createItem(Material.PURPLE_DYE, "§5Subclases", 
            "§7Haz clic para seleccionar tu subclase",
            "§c(Requiere Nivel 30)"
        ));

        // Estadísticas (slot 20)
        inventory.setItem(20, createItem(Material.EMERALD, "§2Estadísticas", 
            "§7Haz clic para ver tus estadísticas"
        ));

        // Clan (slot 21)
        inventory.setItem(21, createItem(Material.SHIELD, "§8Clan", 
            "§7Haz clic para administrar tu clan",
            "§7Clan: §f" + (playerData.getClanName() != null ? playerData.getClanName() : "Sin clan")
        ));

        // Habilidades (slot 22)
        inventory.setItem(22, createItem(Material.BLAZE_ROD, "§cHabilidades", 
            "§7Haz clic para ver tus habilidades"
        ));

        // Misiones (slot 23)
        inventory.setItem(23, createItem(Material.WRITABLE_BOOK, "§6Misiones", 
            "§7Haz clic para ver disponibles misiones"
        ));

        // Equipo (slot 24)
        inventory.setItem(24, createItem(Material.DIAMOND_CHESTPLATE, "§6Equipo", 
            "§7Haz clic para ver tu equipo",
            "§7(Anillos, Collar, Amuletos)"
        ));

        // Logros (slot 29)
        inventory.setItem(29, createItem(Material.GOLD_BLOCK, "§6Logros", 
            "§7Haz clic para ver tus logros"
        ));

        // Mejores Jugadores (slot 30)
        inventory.setItem(30, createItem(Material.DIAMOND, "§bMejores Jugadores", 
            "§7Haz clic para ver el ranking"
        ));

        // Cerrar (slot 49)
        inventory.setItem(49, createItem(Material.BARRIER, "§cCerrar", 
            "§7Haz clic para cerrar el menú"
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
}
