package com.irdem.tunama.menus;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.PetAbility;
import com.irdem.tunama.data.PetType;
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
import java.util.Map;

/**
 * Menú de tienda de mascotas
 * Muestra todas las mascotas disponibles para comprar
 */
public class PetShopMenu implements InventoryHolder {

    private final Inventory inventory;
    private final Player player;
    private final PlayerData playerData;
    private final TunamaRPG plugin;
    private int currentPage;
    private List<PetType> availablePets;

    private static final int ITEMS_PER_PAGE = 28; // 4 filas de 7 items
    private static final int BACK_SLOT = 45;
    private static final int PREV_PAGE_SLOT = 48;
    private static final int NEXT_PAGE_SLOT = 50;
    private static final int INFO_SLOT = 49;

    public PetShopMenu(TunamaRPG plugin, Player player, PlayerData playerData) {
        this(plugin, player, playerData, 0);
    }

    public PetShopMenu(TunamaRPG plugin, Player player, PlayerData playerData, int page) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = playerData;
        this.currentPage = page;
        this.inventory = Bukkit.createInventory(this, 54, "§8◆ §6§lTienda de Mascotas §8◆");
        loadAvailablePets();
        setupItems();
    }

    private void loadAvailablePets() {
        availablePets = new ArrayList<>();
        PetManager petManager = plugin.getPetManager();
        Map<String, PetType> allTypes = petManager.getAllPetTypes();

        String playerClass = playerData.getPlayerClass();
        int playerLevel = playerData.getLevel();

        for (PetType type : allTypes.values()) {
            // Verificar nivel requerido
            if (playerLevel < type.getRequiredLevel()) continue;

            // Verificar clase permitida
            if (!type.isClassAllowed(playerClass)) continue;

            // Verificar permiso si existe
            String permission = type.getPermission();
            if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
                continue;
            }

            availablePets.add(type);
        }
    }

    private void setupItems() {
        inventory.clear();

        // Decoración de bordes
        ItemStack border = createItem(Material.ORANGE_STAINED_GLASS_PANE, " ", new ArrayList<>());
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }
        inventory.setItem(9, border);
        inventory.setItem(17, border);
        inventory.setItem(18, border);
        inventory.setItem(26, border);
        inventory.setItem(27, border);
        inventory.setItem(35, border);
        inventory.setItem(36, border);
        inventory.setItem(44, border);

        // Info de la tienda
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Mascotas disponibles: §e" + availablePets.size());
        infoLore.add("§7Página: §f" + (currentPage + 1) + "/" + getMaxPages());
        infoLore.add("");
        infoLore.add("§7Haz clic en una mascota");
        infoLore.add("§7para comprarla.");
        inventory.setItem(4, createItem(Material.GOLD_INGOT, "§6§lTienda de Mascotas", infoLore));

        // Mostrar mascotas
        int[] slots = {10, 11, 12, 13, 14, 15, 16,
                       19, 20, 21, 22, 23, 24, 25,
                       28, 29, 30, 31, 32, 33, 34,
                       37, 38, 39, 40, 41, 42, 43};

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, availablePets.size());

        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex >= slots.length) break;

            PetType type = availablePets.get(i);
            inventory.setItem(slots[slotIndex], createPetShopItem(type));
        }

        // Botón de volver
        List<String> backLore = new ArrayList<>();
        backLore.add("§7Volver al menú de mascotas");
        inventory.setItem(BACK_SLOT, createItem(Material.BARRIER, "§c✖ Volver", backLore));

        // Navegación de páginas
        if (currentPage > 0) {
            List<String> prevLore = new ArrayList<>();
            prevLore.add("§7Ir a la página " + currentPage);
            inventory.setItem(PREV_PAGE_SLOT, createItem(Material.ARROW, "§e◀ Página Anterior", prevLore));
        }

        if (currentPage < getMaxPages() - 1) {
            List<String> nextLore = new ArrayList<>();
            nextLore.add("§7Ir a la página " + (currentPage + 2));
            inventory.setItem(NEXT_PAGE_SLOT, createItem(Material.ARROW, "§e▶ Página Siguiente", nextLore));
        }

        // Info del jugador
        List<String> playerLore = new ArrayList<>();
        double balance = getPlayerBalance();
        playerLore.add("§7Tu dinero: §a$" + String.format("%.2f", balance));
        inventory.setItem(INFO_SLOT, createItem(Material.EMERALD, "§a§lTu Balance", playerLore));
    }

    private ItemStack createPetShopItem(PetType type) {
        List<String> lore = new ArrayList<>();
        lore.add("§7" + type.getDescription());
        lore.add("");

        // Estadísticas base
        lore.add("§e§lEstadísticas Base:");
        lore.add("§7Vida: §c❤ " + type.getBaseHealth());
        lore.add("§7Defensa: §b🛡 " + type.getBaseDefense());
        lore.add("§7Ataque: §c⚔ " + type.getBaseAttack());
        lore.add("§7Vel. Ataque: §e⚡ " + String.format("%.1f", type.getBaseAttackSpeed()));
        lore.add("§7Vel. Movimiento: §a➤ " + String.format("%.1f", type.getBaseMovementSpeed()));
        lore.add("");

        // Habilidades
        if (!type.getAbilities().isEmpty()) {
            lore.add("§d§lHabilidades:");
            for (PetAbility ability : type.getAbilities()) {
                lore.add("§7• §f" + ability.getName() + " §8(CD: " + ability.getCooldown() + "s)");
            }
            lore.add("");
        }

        // Precio
        lore.add("§6§lPrecio: §e$" + String.format("%.0f", type.getPrice()));
        lore.add("");

        // Nivel requerido
        lore.add("§7Nivel requerido: §f" + type.getRequiredLevel());

        // Clases permitidas
        if (!type.getAllowedClasses().isEmpty()) {
            lore.add("§7Clases: §f" + String.join(", ", type.getAllowedClasses()));
        }

        lore.add("");

        // Verificar si puede comprar
        double balance = getPlayerBalance();
        if (balance >= type.getPrice()) {
            lore.add("§a⚡ Clic para comprar");
        } else {
            lore.add("§c✗ No tienes suficiente dinero");
        }

        return createItem(type.getMenuIcon(), "§b" + type.getName(), lore);
    }

    private double getPlayerBalance() {
        if (plugin.getEconomyManager() != null && plugin.getEconomyManager().isEnabled()) {
            return plugin.getEconomyManager().getBalance(player);
        }
        return 0;
    }

    private int getMaxPages() {
        return Math.max(1, (int) Math.ceil(availablePets.size() / (double) ITEMS_PER_PAGE));
    }

    /**
     * Obtiene el tipo de mascota del slot clicado
     */
    public PetType getPetTypeFromSlot(int slot) {
        int[] slots = {10, 11, 12, 13, 14, 15, 16,
                       19, 20, 21, 22, 23, 24, 25,
                       28, 29, 30, 31, 32, 33, 34,
                       37, 38, 39, 40, 41, 42, 43};

        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                int petIndex = currentPage * ITEMS_PER_PAGE + i;
                if (petIndex < availablePets.size()) {
                    return availablePets.get(petIndex);
                }
            }
        }
        return null;
    }

    /**
     * Intenta comprar una mascota
     */
    public boolean buyPet(PetType type) {
        if (type == null) return false;

        double price = type.getPrice();
        double balance = getPlayerBalance();

        if (balance < price) {
            player.sendMessage("§c✗ No tienes suficiente dinero para comprar esta mascota");
            return false;
        }

        // Retirar dinero
        if (plugin.getEconomyManager() != null && plugin.getEconomyManager().isEnabled()) {
            plugin.getEconomyManager().withdraw(player, price);
        }

        // Crear la mascota
        PetManager petManager = plugin.getPetManager();
        int characterSlot = plugin.getDatabaseManager().getCharacterManager().getActiveSlot(player.getUniqueId());

        com.irdem.tunama.data.Pet pet = petManager.createPet(player.getUniqueId(), characterSlot, type.getId());

        if (pet != null) {
            player.sendMessage("§a✓ ¡Has comprado un " + type.getName() + " por $" + String.format("%.0f", price) + "!");
            player.sendMessage("§7Puedes verlo en tu menú de mascotas.");
            return true;
        } else {
            // Devolver dinero si falla
            if (plugin.getEconomyManager() != null && plugin.getEconomyManager().isEnabled()) {
                plugin.getEconomyManager().deposit(player, price);
            }
            player.sendMessage("§c✗ Error al crear la mascota");
            return false;
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

    public int getCurrentPage() {
        return currentPage;
    }

    public static int getBackSlot() {
        return BACK_SLOT;
    }

    public static int getPrevPageSlot() {
        return PREV_PAGE_SLOT;
    }

    public static int getNextPageSlot() {
        return NEXT_PAGE_SLOT;
    }
}
