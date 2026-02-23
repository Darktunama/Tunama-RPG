package com.irdem.tunama.menus;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.Pet;
import com.irdem.tunama.data.PetCommand;
import com.irdem.tunama.data.PlayerData;
import com.irdem.tunama.managers.PetManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Menú de comandos para mascotas
 * Permite enviar órdenes a las mascotas activas
 */
public class PetCommandMenu implements InventoryHolder {

    private final Inventory inventory;
    private final Player player;
    private final PlayerData playerData;
    private final TunamaRPG plugin;
    private Pet targetPet; // Si es null, aplica a todas las mascotas

    // Slots de comandos
    private static final int FOLLOW_SLOT = 10;
    private static final int STAY_SLOT = 12;
    private static final int ATTACK_SLOT = 14;
    private static final int DEFEND_SLOT = 16;
    private static final int AGGRESSIVE_SLOT = 22;

    // Slot para aplicar a todas
    private static final int ALL_PETS_SLOT = 31;
    // Slot de volver
    private static final int BACK_SLOT = 40;

    public PetCommandMenu(TunamaRPG plugin, Player player, PlayerData playerData) {
        this(plugin, player, playerData, null);
    }

    public PetCommandMenu(TunamaRPG plugin, Player player, PlayerData playerData, Pet targetPet) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = playerData;
        this.targetPet = targetPet;
        this.inventory = Bukkit.createInventory(this, 45, "§8◆ §d§lComandos de Mascotas §8◆");
        setupItems();
    }

    private void setupItems() {
        // Decoración de bordes
        ItemStack border = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ", new ArrayList<>());
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(36 + i, border);
        }
        inventory.setItem(9, border);
        inventory.setItem(17, border);
        inventory.setItem(18, border);
        inventory.setItem(26, border);
        inventory.setItem(27, border);
        inventory.setItem(35, border);

        // Info de mascota objetivo
        if (targetPet != null) {
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§7Enviando comandos a:");
            infoLore.add("§b" + targetPet.getDisplayName());
            infoLore.add("");
            infoLore.add("§7Comando actual: §e" + targetPet.getCurrentCommand().getDisplayName());
            inventory.setItem(4, createItem(Material.NAME_TAG, "§e§lMascota Seleccionada", infoLore));
        } else {
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§7Enviando comandos a:");
            infoLore.add("§aTodas las mascotas activas");
            inventory.setItem(4, createItem(Material.BEACON, "§e§lTodas las Mascotas", infoLore));
        }

        // FOLLOW - Seguir
        List<String> followLore = new ArrayList<>();
        followLore.add("§7" + PetCommand.FOLLOW.getDescription());
        followLore.add("");
        followLore.add("§7La mascota te seguirá a donde vayas");
        followLore.add("§7y atacará solo si tú atacas primero.");
        followLore.add("");
        followLore.add("§e⚡ Clic para ordenar");
        inventory.setItem(FOLLOW_SLOT, createItem(Material.LEAD, "§a§l" + PetCommand.FOLLOW.getDisplayName(), followLore));

        // STAY - Quedarse
        List<String> stayLore = new ArrayList<>();
        stayLore.add("§7" + PetCommand.STAY.getDescription());
        stayLore.add("");
        stayLore.add("§7La mascota se quedará en su");
        stayLore.add("§7posición actual sin moverse.");
        stayLore.add("");
        stayLore.add("§e⚡ Clic para ordenar");
        inventory.setItem(STAY_SLOT, createItem(Material.BARRIER, "§c§l" + PetCommand.STAY.getDisplayName(), stayLore));

        // ATTACK - Atacar
        List<String> attackLore = new ArrayList<>();
        attackLore.add("§7" + PetCommand.ATTACK.getDescription());
        attackLore.add("");
        attackLore.add("§7La mascota atacará al objetivo");
        attackLore.add("§7que tú marques (golpeándolo).");
        attackLore.add("");
        attackLore.add("§e⚡ Clic para ordenar");
        inventory.setItem(ATTACK_SLOT, createItem(Material.IRON_SWORD, "§c§l" + PetCommand.ATTACK.getDisplayName(), attackLore));

        // DEFEND - Defender
        List<String> defendLore = new ArrayList<>();
        defendLore.add("§7" + PetCommand.DEFEND.getDescription());
        defendLore.add("");
        defendLore.add("§7La mascota se quedará cerca de ti");
        defendLore.add("§7y atacará a quien te ataque.");
        defendLore.add("");
        defendLore.add("§e⚡ Clic para ordenar");
        inventory.setItem(DEFEND_SLOT, createItem(Material.SHIELD, "§b§l" + PetCommand.DEFEND.getDisplayName(), defendLore));

        // AGGRESSIVE - Agresivo
        List<String> aggressiveLore = new ArrayList<>();
        aggressiveLore.add("§7" + PetCommand.AGGRESSIVE.getDescription());
        aggressiveLore.add("");
        aggressiveLore.add("§7La mascota atacará automáticamente");
        aggressiveLore.add("§7a cualquier mob hostil cercano.");
        aggressiveLore.add("");
        aggressiveLore.add("§e⚡ Clic para ordenar");
        inventory.setItem(AGGRESSIVE_SLOT, createItem(Material.TNT, "§4§l" + PetCommand.AGGRESSIVE.getDisplayName(), aggressiveLore));

        // Botón para cambiar entre mascota específica y todas
        if (targetPet != null) {
            List<String> allLore = new ArrayList<>();
            allLore.add("§7Clic para enviar comandos");
            allLore.add("§7a todas las mascotas activas");
            inventory.setItem(ALL_PETS_SLOT, createItem(Material.ENDER_EYE, "§d§lAplicar a Todas", allLore));
        }

        // Botón de volver
        List<String> backLore = new ArrayList<>();
        backLore.add("§7Volver al menú de mascotas");
        inventory.setItem(BACK_SLOT, createItem(Material.ARROW, "§c✖ Volver", backLore));
    }

    /**
     * Ejecuta un comando desde un slot
     */
    public boolean executeCommand(int slot) {
        PetCommand command = getCommandFromSlot(slot);
        if (command == null) return false;

        PetManager petManager = plugin.getPetManager();

        if (targetPet != null) {
            petManager.commandPet(player, targetPet, command);
        } else {
            petManager.commandAllPets(player, command);
        }

        return true;
    }

    /**
     * Obtiene el comando correspondiente a un slot
     */
    private PetCommand getCommandFromSlot(int slot) {
        switch (slot) {
            case FOLLOW_SLOT: return PetCommand.FOLLOW;
            case STAY_SLOT: return PetCommand.STAY;
            case ATTACK_SLOT: return PetCommand.ATTACK;
            case DEFEND_SLOT: return PetCommand.DEFEND;
            case AGGRESSIVE_SLOT: return PetCommand.AGGRESSIVE;
            default: return null;
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
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

    public Player getPlayer() {
        return player;
    }

    public PlayerData getPlayerData() {
        return playerData;
    }

    public TunamaRPG getPlugin() {
        return plugin;
    }

    public Pet getTargetPet() {
        return targetPet;
    }

    public void setTargetPet(Pet pet) {
        this.targetPet = pet;
        setupItems();
    }

    public static int getFollowSlot() {
        return FOLLOW_SLOT;
    }

    public static int getStaySlot() {
        return STAY_SLOT;
    }

    public static int getAttackSlot() {
        return ATTACK_SLOT;
    }

    public static int getDefendSlot() {
        return DEFEND_SLOT;
    }

    public static int getAggressiveSlot() {
        return AGGRESSIVE_SLOT;
    }

    public static int getAllPetsSlot() {
        return ALL_PETS_SLOT;
    }

    public static int getBackSlot() {
        return BACK_SLOT;
    }
}
