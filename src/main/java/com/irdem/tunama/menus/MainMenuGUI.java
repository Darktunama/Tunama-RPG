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
import com.irdem.tunama.data.Race;
import com.irdem.tunama.data.RPGClass;
import org.bukkit.inventory.meta.SkullMeta;
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
        // Obtener nombres de raza y clase (no IDs)
        String raceName = "No asignada";
        String className = "No asignada";
        if (playerData.getRace() != null && !playerData.getRace().isEmpty()) {
            Race r = plugin.getRaceManager().getRace(playerData.getRace().toLowerCase());
            raceName = r != null ? r.getName() : playerData.getRace();
        }
        if (playerData.getPlayerClass() != null && !playerData.getPlayerClass().isEmpty()) {
            RPGClass c = plugin.getClassManager().getClass(playerData.getPlayerClass().toLowerCase());
            className = c != null ? c.getName() : playerData.getPlayerClass();
        }
        
        // Info del Jugador (slot 11) - Cabeza del jugador
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName("§6Información");
            List<String> headLore = new ArrayList<>();
            headLore.add("§7Jugador: §f" + playerData.getUsername());
            headLore.add("§7Raza: §f" + raceName);
            headLore.add("§7Clase: §f" + className);
            headLore.add("§7Nivel: §f" + playerData.getLevel());
            headLore.add("§7EXP: §f" + playerData.getExperience());
            skullMeta.setLore(headLore);
            playerHead.setItemMeta(skullMeta);
        }
        inventory.setItem(11, playerHead);

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

        // Subclases (slot 14) - Solo si no tiene subclase
        if (playerData.getSubclass() == null || playerData.getSubclass().isEmpty()) {
            inventory.setItem(14, createItem(Material.PURPLE_DYE, "§5Subclases",
                "§7Haz clic para seleccionar tu subclase",
                "§c(Requiere Nivel 30)"
            ));
        }

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

        // Mascotas (slot 38) - Solo si el jugador puede tener mascotas
        if (canShowPetsMenu()) {
            int activePets = plugin.getPetManager().getActivePets(player).size();
            int maxPets = plugin.getPetManager().getMaxPets(player);
            inventory.setItem(38, createItem(Material.WOLF_SPAWN_EGG, "§bMascotas",
                "§7Gestiona tus mascotas de combate",
                "",
                "§7Mascotas activas: §f" + activePets + "/" + maxPets,
                "",
                "§e⚡ Clic para abrir"
            ));
        }

        // Renacer (slot 47 - abajo izquierda)
        inventory.setItem(47, createItem(Material.SOUL_CAMPFIRE, "§4Renacer",
            "§7Elimina tu personaje actual",
            "§7y comienza de nuevo",
            "",
            "§c§l¡ADVERTENCIA! Esta acción no se puede deshacer"
        ));

        // Cerrar (slot 49)
        inventory.setItem(49, createItem(Material.BARRIER, "§cCerrar",
            "§7Haz clic para cerrar el menú"
        ));

        // Personajes (slot 51 - abajo derecha)
        inventory.setItem(51, createItem(Material.ENCHANTED_BOOK, "§dPersonajes",
            "§7Gestiona tus personajes",
            "§7Cambia entre personajes o crea nuevos",
            "",
            "§7Máximo de personajes: §f" + getMaxCharacters()
        ));
    }

    private int getMaxCharacters() {
        com.irdem.tunama.data.CharacterManager charManager = plugin.getDatabaseManager().getCharacterManager();
        return charManager.getMaxCharacters(player);
    }

    /**
     * Verifica si el jugador puede ver el menú de mascotas
     */
    private boolean canShowPetsMenu() {
        // Verificar permiso
        if (player.hasPermission("rpg.pets")) return true;

        // Verificar clase
        String playerClass = playerData.getPlayerClass();
        if (playerClass == null || playerClass.isEmpty()) return false;

        RPGClass rpgClass = plugin.getClassManager().getClass(playerClass.toLowerCase());
        return rpgClass != null && rpgClass.hasPets();
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
