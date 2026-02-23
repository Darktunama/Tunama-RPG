package com.irdem.tunama.menus;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.CharacterManager;
import com.irdem.tunama.data.Pet;
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

/**
 * Menú principal de mascotas
 * Layout (54 slots):
 * - Fila 1: Decoración + Título
 * - Fila 2 (slots 10-16): Mascotas guardadas
 * - Fila 3 (slots 19-25): Mascotas activas
 * - Fila 4: Info y acciones
 * - Fila 5 (slot 40): Comandos
 * - Fila 6 (slot 49): Volver
 */
public class PetMenu implements InventoryHolder {

    private final Inventory inventory;
    private final Player player;
    private final PlayerData playerData;
    private final TunamaRPG plugin;
    private Pet selectedPet;
    private int currentPage;

    // Slots para mascotas guardadas (7 slots)
    private static final int[] STORED_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    // Slots para mascotas activas (7 slots)
    private static final int[] ACTIVE_SLOTS = {19, 20, 21, 22, 23, 24, 25};

    // Slot de acción (invocar/guardar)
    private static final int ACTION_SLOT = 31;
    // Slot de comandos
    private static final int COMMANDS_SLOT = 40;
    // Slot de tienda
    private static final int SHOP_SLOT = 41;
    // Slot de resurrección
    private static final int RESURRECT_SLOT = 39;
    // Slot de liberar mascota
    private static final int RELEASE_SLOT = 38;
    // Slot de volver
    private static final int BACK_SLOT = 49;
    // Slot de info
    private static final int INFO_SLOT = 4;
    // Slots de navegación
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    // Mascotas por página
    private static final int PETS_PER_PAGE = 7;
    private static final int MAX_PAGES = 4;

    public PetMenu(TunamaRPG plugin, Player player, PlayerData playerData) {
        this(plugin, player, playerData, 0);
    }

    public PetMenu(TunamaRPG plugin, Player player, PlayerData playerData, int page) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = playerData;
        this.currentPage = page;
        this.inventory = Bukkit.createInventory(this, 54, "§8◆ §b§lMascotas §8◆");
        this.selectedPet = null;
        setupItems();
    }

    private void setupItems() {
        // Limpiar inventario
        inventory.clear();

        PetManager petManager = plugin.getPetManager();

        // Decoración de bordes
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", new ArrayList<>());
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

        // Info del sistema de mascotas
        List<String> infoLore = new ArrayList<>();
        int maxPets = petManager.getMaxPets(player);
        List<Pet> activePets = petManager.getActivePets(player);
        int characterSlot = getCharacterSlot();
        List<Pet> storedPets = petManager.getStoredPets(
            player.getUniqueId(),
            characterSlot
        );

        // Filtrar solo las inactivas para paginación
        List<Pet> inactivePets = new ArrayList<>();
        for (Pet pet : storedPets) {
            if (!pet.isActive()) {
                inactivePets.add(pet);
            }
        }

        int totalPages = Math.max(1, (int) Math.ceil(inactivePets.size() / (double) PETS_PER_PAGE));
        if (totalPages > MAX_PAGES) totalPages = MAX_PAGES;

        infoLore.add("§7Mascotas activas: §a" + activePets.size() + "§7/§e" + maxPets);
        infoLore.add("§7Mascotas guardadas: §b" + inactivePets.size());
        infoLore.add("§7Página: §f" + (currentPage + 1) + "§7/§f" + totalPages);
        infoLore.add("");
        infoLore.add("§7Selecciona una mascota para");
        infoLore.add("§7invocarla o guardarla.");

        inventory.setItem(INFO_SLOT, createItem(Material.LEAD, "§e§lTus Mascotas", infoLore));

        // Etiqueta de mascotas guardadas
        List<String> storedLabel = new ArrayList<>();
        storedLabel.add("§7Las mascotas guardadas descansan");
        storedLabel.add("§7y recuperan vida lentamente.");
        inventory.setItem(9, createItem(Material.CHEST, "§6Guardadas", storedLabel));

        // Mostrar mascotas guardadas (con paginación)
        int startIndex = currentPage * PETS_PER_PAGE;
        int endIndex = Math.min(startIndex + PETS_PER_PAGE, inactivePets.size());

        int storedIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            if (storedIndex >= STORED_SLOTS.length) break;
            Pet pet = inactivePets.get(i);
            inventory.setItem(STORED_SLOTS[storedIndex], createPetItem(pet, false));
            storedIndex++;
        }

        // Llenar slots vacíos de guardadas
        for (int i = storedIndex; i < STORED_SLOTS.length; i++) {
            inventory.setItem(STORED_SLOTS[i], createEmptySlot("§8Slot vacío"));
        }

        // Navegación de páginas
        if (currentPage > 0) {
            List<String> prevLore = new ArrayList<>();
            prevLore.add("§7Ir a la página " + currentPage);
            inventory.setItem(PREV_PAGE_SLOT, createItem(Material.ARROW, "§e◀ Página Anterior", prevLore));
        }

        if (currentPage < totalPages - 1) {
            List<String> nextLore = new ArrayList<>();
            nextLore.add("§7Ir a la página " + (currentPage + 2));
            inventory.setItem(NEXT_PAGE_SLOT, createItem(Material.ARROW, "§e▶ Página Siguiente", nextLore));
        }

        // Etiqueta de mascotas activas
        List<String> activeLabel = new ArrayList<>();
        activeLabel.add("§7Las mascotas activas te");
        activeLabel.add("§7siguen y luchan por ti.");
        inventory.setItem(18, createItem(Material.DIAMOND_SWORD, "§aActivas", activeLabel));

        // Mostrar mascotas activas
        int activeIndex = 0;
        for (Pet pet : activePets) {
            if (activeIndex >= ACTIVE_SLOTS.length) break;

            inventory.setItem(ACTIVE_SLOTS[activeIndex], createPetItem(pet, true));
            activeIndex++;
        }

        // Llenar slots vacíos de activas
        for (int i = activeIndex; i < ACTIVE_SLOTS.length; i++) {
            inventory.setItem(ACTIVE_SLOTS[i], createEmptySlot("§8Slot vacío"));
        }

        // Botón de acción (invocar/guardar)
        updateActionButton();

        // Botón de comandos
        List<String> cmdLore = new ArrayList<>();
        cmdLore.add("§7Envía órdenes a tus mascotas");
        cmdLore.add("");
        cmdLore.add("§e⚡ Clic para abrir");
        inventory.setItem(COMMANDS_SLOT, createItem(Material.COMMAND_BLOCK, "§d§lComandos", cmdLore));

        // Botón de resurrección
        updateResurrectButton(inactivePets);

        // Botón de liberar mascota
        updateReleaseButton();

        // Botón de tienda
        List<String> shopLore = new ArrayList<>();
        shopLore.add("§7Compra nuevas mascotas");
        shopLore.add("");
        shopLore.add("§e⚡ Clic para abrir");
        inventory.setItem(SHOP_SLOT, createItem(Material.GOLD_INGOT, "§6§lTienda", shopLore));

        // Botón de volver
        List<String> backLore = new ArrayList<>();
        backLore.add("§7Volver al menú principal");
        inventory.setItem(BACK_SLOT, createItem(Material.BARRIER, "§c✖ Volver", backLore));
    }

    /**
     * Actualiza el botón de liberar mascota
     */
    private void updateReleaseButton() {
        if (selectedPet == null) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Selecciona una mascota primero");
            inventory.setItem(RELEASE_SLOT, createItem(Material.GRAY_DYE, "§7Liberar", lore));
        } else {
            List<String> lore = new ArrayList<>();
            lore.add("§7Mascota: §b" + selectedPet.getDisplayName());
            lore.add("");
            lore.add("§c⚠ ¡Esta acción es permanente!");
            lore.add("§7La mascota será eliminada.");
            lore.add("");
            lore.add("§e⚡ Clic para liberar");
            inventory.setItem(RELEASE_SLOT, createItem(Material.LAVA_BUCKET, "§c§lLiberar Mascota", lore));
        }
    }

    /**
     * Crea un item representando una mascota
     */
    private ItemStack createPetItem(Pet pet, boolean isActive) {
        PetType type = plugin.getPetManager().getPetType(pet.getTypeId());
        Material icon = type != null ? type.getMenuIcon() : Material.EGG;

        // Si está muerta, cambiar el icono
        if (pet.isDead()) {
            icon = Material.SKELETON_SKULL;
        }

        List<String> lore = new ArrayList<>();
        lore.add("§7Tipo: §f" + (type != null ? type.getName() : pet.getTypeId()));
        lore.add("§7Nivel: §e" + pet.getLevel());
        lore.add("");

        // Stats
        if (type != null) {
            pet.calculateStats(type);
        }

        // Si está muerta, mostrar estado especial
        if (pet.isDead()) {
            lore.add("§c☠ MUERTA");
            lore.add("");
            lore.add("§7Vida máxima: §a" + pet.getMaxHealth());
            lore.add("§7Daño: §c" + pet.getDamage());
            lore.add("§7Armadura: §b" + pet.getArmor());
            lore.add("");
            lore.add("§7XP: §d" + pet.getExperience() + "§7/§d" + pet.getRequiredExperienceForNextLevel());
            lore.add("");
            lore.add("§c✗ Necesita resurrección");
            lore.add("§7Usa el botón §aResucitar §7para");
            lore.add("§7devolverla a la vida.");

            String displayName = "§c☠ " + pet.getDisplayName() + " §7[Nv." + pet.getLevel() + "]";
            return createItem(icon, displayName, lore);
        }

        int healthPercent = pet.getMaxHealth() > 0 ?
            (pet.getCurrentHealth() * 100 / pet.getMaxHealth()) : 100;
        String healthColor = healthPercent > 50 ? "§a" : (healthPercent > 25 ? "§e" : "§c");

        lore.add("§7Vida: " + healthColor + pet.getCurrentHealth() + "§7/§a" + pet.getMaxHealth());
        lore.add("§7Daño: §c" + pet.getDamage());
        lore.add("§7Armadura: §b" + pet.getArmor());
        lore.add("");

        // Experiencia
        int xpPercent = pet.getLevel() < 50 ?
            (pet.getExperience() * 100 / pet.getRequiredExperienceForNextLevel()) : 100;
        lore.add("§7XP: §d" + pet.getExperience() + "§7/§d" + pet.getRequiredExperienceForNextLevel());
        lore.add(createProgressBar(xpPercent, "§d"));
        lore.add("");

        if (isActive) {
            lore.add("§a● Activa");
            lore.add("§7Comando: §e" + pet.getCurrentCommand().getDisplayName());
            lore.add("");
            lore.add("§e⚡ Clic para seleccionar");
        } else {
            lore.add("§7● Guardada");
            lore.add("");
            lore.add("§e⚡ Clic para seleccionar");
        }

        String displayName = "§b" + pet.getDisplayName() + " §7[Nv." + pet.getLevel() + "]";
        if (selectedPet != null && selectedPet.getId().equals(pet.getId())) {
            displayName = "§a▶ " + displayName;
            lore.add("");
            lore.add("§a✓ SELECCIONADA");
        }

        return createItem(icon, displayName, lore);
    }

    /**
     * Crea una barra de progreso visual
     */
    private String createProgressBar(int percent, String color) {
        int filled = percent / 10;
        int empty = 10 - filled;
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < filled; i++) {
            bar.append(color).append("▮");
        }
        for (int i = 0; i < empty; i++) {
            bar.append("§7▯");
        }
        bar.append("§8]");
        return bar.toString();
    }

    /**
     * Crea un slot vacío
     */
    private ItemStack createEmptySlot(String name) {
        List<String> lore = new ArrayList<>();
        lore.add("§7No hay mascota aquí");
        return createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, name, lore);
    }

    /**
     * Actualiza el botón de resurrección según si hay mascotas muertas
     */
    private void updateResurrectButton(List<Pet> storedPets) {
        // Contar mascotas muertas
        int deadCount = 0;
        for (Pet pet : storedPets) {
            if (pet.isDead()) {
                deadCount++;
            }
        }

        if (deadCount > 0) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Mascotas muertas: §c" + deadCount);
            lore.add("");
            lore.add("§7Coste de resurrección:");
            lore.add("§7• §e$500 §7por mascota");
            lore.add("");
            lore.add("§e⚡ Clic para resucitar");
            inventory.setItem(RESURRECT_SLOT, createItem(Material.TOTEM_OF_UNDYING, "§a§lResucitar", lore));
        } else {
            List<String> lore = new ArrayList<>();
            lore.add("§7No tienes mascotas muertas");
            inventory.setItem(RESURRECT_SLOT, createItem(Material.GRAY_DYE, "§7Resucitar", lore));
        }
    }

    /**
     * Actualiza el botón de acción según la mascota seleccionada
     */
    public void updateActionButton() {
        if (selectedPet == null) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Selecciona una mascota primero");
            inventory.setItem(ACTION_SLOT, createItem(Material.GRAY_DYE, "§7Sin selección", lore));
        } else if (selectedPet.isActive()) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Mascota: §b" + selectedPet.getDisplayName());
            lore.add("");
            lore.add("§e⚡ Clic para guardar");
            inventory.setItem(ACTION_SLOT, createItem(Material.ENDER_CHEST, "§c§lGuardar Mascota", lore));
        } else {
            List<String> lore = new ArrayList<>();
            lore.add("§7Mascota: §b" + selectedPet.getDisplayName());
            lore.add("");

            int maxPets = plugin.getPetManager().getMaxPets(player);
            int activePets = plugin.getPetManager().getActivePets(player).size();

            if (activePets >= maxPets) {
                lore.add("§c✗ Máximo de mascotas activas");
                lore.add("§7Guarda una mascota primero");
                inventory.setItem(ACTION_SLOT, createItem(Material.BARRIER, "§c§lNo disponible", lore));
            } else {
                lore.add("§e⚡ Clic para invocar");
                inventory.setItem(ACTION_SLOT, createItem(Material.NETHER_STAR, "§a§lInvocar Mascota", lore));
            }
        }
    }

    /**
     * Selecciona una mascota del slot dado
     */
    public boolean selectPetFromSlot(int slot) {
        PetManager petManager = plugin.getPetManager();

        // Verificar si es un slot de mascotas guardadas
        for (int i = 0; i < STORED_SLOTS.length; i++) {
            if (STORED_SLOTS[i] == slot) {
                List<Pet> storedPets = petManager.getStoredPets(
                    player.getUniqueId(),
                    getCharacterSlot()
                );
                // Filtrar solo las no activas
                List<Pet> inactivePets = new ArrayList<>();
                for (Pet pet : storedPets) {
                    if (!pet.isActive()) {
                        inactivePets.add(pet);
                    }
                }
                // Calcular índice real considerando la página actual
                int realIndex = (currentPage * PETS_PER_PAGE) + i;
                if (realIndex < inactivePets.size()) {
                    selectedPet = inactivePets.get(realIndex);
                    refresh();
                    return true;
                }
                return false;
            }
        }

        // Verificar si es un slot de mascotas activas
        for (int i = 0; i < ACTIVE_SLOTS.length; i++) {
            if (ACTIVE_SLOTS[i] == slot) {
                List<Pet> activePets = petManager.getActivePets(player);
                if (i < activePets.size()) {
                    selectedPet = activePets.get(i);
                    refresh();
                    return true;
                }
                return false;
            }
        }

        return false;
    }

    /**
     * Ejecuta la acción del botón principal
     */
    public boolean executeAction() {
        if (selectedPet == null) return false;

        PetManager petManager = plugin.getPetManager();

        if (selectedPet.isActive()) {
            // Guardar mascota
            petManager.dismissPet(player, selectedPet);
            selectedPet = null;
            refresh();
            return true;
        } else {
            // Invocar mascota
            int maxPets = petManager.getMaxPets(player);
            int activePets = petManager.getActivePets(player).size();

            if (activePets >= maxPets) {
                player.sendMessage("§c✗ Ya tienes el máximo de mascotas activas");
                return false;
            }

            boolean success = petManager.summonPet(player, selectedPet);
            if (success) {
                refresh();
            }
            return success;
        }
    }

    /**
     * Refresca el menú
     */
    public void refresh() {
        setupItems();
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

    /**
     * Obtiene el slot de personaje activo del jugador
     */
    private int getCharacterSlot() {
        CharacterManager charManager = plugin.getDatabaseManager().getCharacterManager();
        return charManager.getActiveSlot(player.getUniqueId());
    }

    public Pet getSelectedPet() {
        return selectedPet;
    }

    public void setSelectedPet(Pet pet) {
        this.selectedPet = pet;
        updateActionButton();
    }

    public static int getActionSlot() {
        return ACTION_SLOT;
    }

    public static int getCommandsSlot() {
        return COMMANDS_SLOT;
    }

    public static int getBackSlot() {
        return BACK_SLOT;
    }

    public static int[] getStoredSlots() {
        return STORED_SLOTS;
    }

    public static int[] getActiveSlots() {
        return ACTIVE_SLOTS;
    }

    public static int getShopSlot() {
        return SHOP_SLOT;
    }

    public static int getResurrectSlot() {
        return RESURRECT_SLOT;
    }

    public static int getReleaseSlot() {
        return RELEASE_SLOT;
    }

    public static int getPrevPageSlot() {
        return PREV_PAGE_SLOT;
    }

    public static int getNextPageSlot() {
        return NEXT_PAGE_SLOT;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        PetManager petManager = plugin.getPetManager();
        int characterSlot = getCharacterSlot();
        List<Pet> storedPets = petManager.getStoredPets(player.getUniqueId(), characterSlot);

        List<Pet> inactivePets = new ArrayList<>();
        for (Pet pet : storedPets) {
            if (!pet.isActive()) {
                inactivePets.add(pet);
            }
        }

        int totalPages = Math.max(1, (int) Math.ceil(inactivePets.size() / (double) PETS_PER_PAGE));
        if (totalPages > MAX_PAGES) totalPages = MAX_PAGES;
        return totalPages;
    }

    /**
     * Libera (elimina permanentemente) la mascota seleccionada
     */
    public boolean releasePet() {
        if (selectedPet == null) {
            player.sendMessage("§c✗ No tienes ninguna mascota seleccionada");
            return false;
        }

        // No permitir liberar mascotas activas
        if (selectedPet.isActive()) {
            player.sendMessage("§c✗ Debes guardar la mascota antes de liberarla");
            return false;
        }

        String petName = selectedPet.getDisplayName();

        // Eliminar de la base de datos
        plugin.getDatabaseManager().deletePet(selectedPet.getId());

        // Limpiar selección
        selectedPet = null;

        player.sendMessage("§a✓ Has liberado a §b" + petName + "§a. Adiós, amigo...");

        return true;
    }

    /**
     * Resucita una mascota muerta (requiere dinero)
     */
    public boolean resurrectPet(Pet pet) {
        if (pet == null || !pet.isDead()) {
            player.sendMessage("§c✗ Esta mascota no está muerta");
            return false;
        }

        double cost = 500.0; // Coste de resurrección

        // Verificar economía
        if (plugin.getEconomyManager() != null && plugin.getEconomyManager().isEnabled()) {
            double balance = plugin.getEconomyManager().getBalance(player);
            if (balance < cost) {
                player.sendMessage("§c✗ No tienes suficiente dinero. Necesitas §e$" + String.format("%.0f", cost));
                return false;
            }

            // Retirar dinero
            plugin.getEconomyManager().withdraw(player, cost);
        }

        // Resucitar con 50% de vida
        pet.resurrect(50);

        // Guardar en base de datos
        plugin.getDatabaseManager().savePet(pet);

        player.sendMessage("§a✓ ¡" + pet.getDisplayName() + " ha sido resucitado!");
        return true;
    }

    /**
     * Cuenta las mascotas muertas
     */
    public int countDeadPets() {
        List<Pet> storedPets = plugin.getPetManager().getStoredPets(
            player.getUniqueId(),
            getCharacterSlot()
        );
        int count = 0;
        for (Pet pet : storedPets) {
            if (pet.isDead()) count++;
        }
        return count;
    }

    /**
     * Obtiene la primera mascota muerta
     */
    public Pet getFirstDeadPet() {
        List<Pet> storedPets = plugin.getPetManager().getStoredPets(
            player.getUniqueId(),
            getCharacterSlot()
        );
        for (Pet pet : storedPets) {
            if (pet.isDead()) return pet;
        }
        return null;
    }
}
