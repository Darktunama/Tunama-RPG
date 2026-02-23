package com.irdem.tunama.menus.clan;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.Clan;
import com.irdem.tunama.data.ClanLog;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Menú del banco del clan
 */
public class ClanBankMenu implements InventoryHolder {
    private final TunamaRPG plugin;
    private final Clan clan;
    private final Player opener;
    private Inventory inventory;
    private int currentPage = 0;
    private static final int MAX_PAGES = 6;
    private static final int ITEMS_PER_PAGE = 45;
    private ItemStack[] allBankItems; // Array completo de 270 items (6 páginas)

    // Almacena qué jugadores están esperando ingresar cantidad de oro
    private static final Map<UUID, PendingGoldAction> pendingGoldActions = new HashMap<>();

    // Almacena qué clanes tienen el banco abierto para evitar duplicación
    private static final Map<Integer, UUID> activeBankUsers = new HashMap<>();

    public ClanBankMenu(TunamaRPG plugin, Clan clan, Player opener) {
        this.plugin = plugin;
        this.clan = clan;
        this.opener = opener;
        this.allBankItems = new ItemStack[MAX_PAGES * ITEMS_PER_PAGE]; // 270 items
    }

    public void open(Player player) {
        open(player, 0);
    }

    /**
     * Verifica si el banco de un clan está siendo usado por otro jugador
     */
    public static boolean isBankInUse(int clanId, UUID requesterId) {
        UUID currentUser = activeBankUsers.get(clanId);
        if (currentUser == null) return false;
        // Si el usuario actual es el mismo, permitir (cambio de página)
        if (currentUser.equals(requesterId)) return false;
        // Verificar si el jugador sigue online
        Player onlineUser = Bukkit.getPlayer(currentUser);
        if (onlineUser == null || !onlineUser.isOnline()) {
            activeBankUsers.remove(clanId);
            return false;
        }
        return true;
    }

    public static void releaseBankLock(int clanId, UUID playerId) {
        UUID currentUser = activeBankUsers.get(clanId);
        if (currentUser != null && currentUser.equals(playerId)) {
            activeBankUsers.remove(clanId);
        }
    }

    public void open(Player player, int page) {
        // Verificar si otro jugador ya tiene el banco abierto
        if (isBankInUse(clan.getId(), player.getUniqueId())) {
            player.sendMessage("§c✗ El banco del clan está siendo utilizado por otro miembro. Espera a que termine.");
            return;
        }
        activeBankUsers.put(clan.getId(), player.getUniqueId());
        this.currentPage = Math.max(0, Math.min(page, MAX_PAGES - 1));
        inventory = Bukkit.createInventory(this, 54, "§6§lBanco - Pág " + (currentPage + 1) + "/" + MAX_PAGES);

        // Cargar items del banco si no se han cargado aún
        if (allBankItems[0] == null && allBankItems[allBankItems.length - 1] == null) {
            try {
                ItemStack[] loadedItems = plugin.getClanBankManager().loadBankItems(clan.getId());
                // Copiar items cargados al array completo
                if (loadedItems != null) {
                    System.arraycopy(loadedItems, 0, allBankItems, 0, Math.min(loadedItems.length, allBankItems.length));
                }
            } catch (SQLException e) {
                player.sendMessage("§c✗ Error al cargar el banco: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Colocar items de la página actual en slots 0-44
        int startIndex = currentPage * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int bankIndex = startIndex + i;
            if (bankIndex < allBankItems.length && allBankItems[bankIndex] != null) {
                inventory.setItem(i, allBankItems[bankIndex]);
            }
        }

        // Botón: Página Anterior (slot 45) - solo si no estamos en la primera página
        if (currentPage > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§e◄ Página Anterior");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Ir a página " + currentPage);
                lore.add("");
                prevMeta.setLore(lore);
                prevPage.setItemMeta(prevMeta);
            }
            inventory.setItem(45, prevPage);
        }

        // Botón: Depositar Oro (slot 46)
        ItemStack depositGold = new ItemStack(Material.GOLD_INGOT);
        ItemMeta depositMeta = depositGold.getItemMeta();
        if (depositMeta != null) {
            depositMeta.setDisplayName("§6§lDepositar Oro");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Deposita oro al clan desde");
            lore.add("§7tu economía personal");
            lore.add("");
            lore.add("§e» Click para depositar");
            depositMeta.setLore(lore);
            depositGold.setItemMeta(depositMeta);
        }
        inventory.setItem(46, depositGold);

        // Botón: Volver (slot 48)
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c« Volver");
            back.setItemMeta(backMeta);
        }
        inventory.setItem(48, back);

        // Info: Oro del Clan + Página (slot 49)
        ItemStack goldInfo = new ItemStack(Material.EMERALD);
        ItemMeta goldInfoMeta = goldInfo.getItemMeta();
        if (goldInfoMeta != null) {
            goldInfoMeta.setDisplayName("§6§lOro del Clan");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Oro disponible: §6" + clan.getGold());
            lore.add("");
            lore.add("§7Página: §e" + (currentPage + 1) + "/" + MAX_PAGES);
            lore.add("");
            goldInfoMeta.setLore(lore);
            goldInfo.setItemMeta(goldInfoMeta);
        }
        inventory.setItem(49, goldInfo);

        // Botón: Retirar Oro (slot 50)
        ItemStack withdrawGold = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta withdrawMeta = withdrawGold.getItemMeta();
        if (withdrawMeta != null) {
            withdrawMeta.setDisplayName("§e§lRetirar Oro");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Retira oro del clan a tu");
            lore.add("§7economía personal");
            lore.add("");
            lore.add("§e» Click para retirar");
            withdrawMeta.setLore(lore);
            withdrawGold.setItemMeta(withdrawMeta);
        }
        inventory.setItem(50, withdrawGold);

        // Botón: Página Siguiente (slot 53) - solo si no estamos en la última página
        if (currentPage < MAX_PAGES - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§ePágina Siguiente ►");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Ir a página " + (currentPage + 2));
                lore.add("");
                nextMeta.setLore(lore);
                nextPage.setItemMeta(nextMeta);
            }
            inventory.setItem(53, nextPage);
        }

        player.openInventory(inventory);
    }

    public void saveBank() {
        // Guardar los items de la página actual en el array completo
        int startIndex = currentPage * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            allBankItems[startIndex + i] = inventory.getItem(i);
        }

        // Guardar todo el array al cerrar
        try {
            plugin.getClanBankManager().saveBankItems(clan.getId(), allBankItems);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al guardar banco del clan " + clan.getTag() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public Clan getClan() {
        return clan;
    }

    public Player getOpener() {
        return opener;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static void addPendingGoldAction(UUID playerId, PendingGoldAction action) {
        pendingGoldActions.put(playerId, action);
    }

    public static PendingGoldAction getPendingGoldAction(UUID playerId) {
        return pendingGoldActions.get(playerId);
    }

    public static void removePendingGoldAction(UUID playerId) {
        pendingGoldActions.remove(playerId);
    }

    public static boolean hasPendingGoldAction(UUID playerId) {
        return pendingGoldActions.containsKey(playerId);
    }

    public static class PendingGoldAction {
        public final Clan clan;
        public final ActionType type;

        public PendingGoldAction(Clan clan, ActionType type) {
            this.clan = clan;
            this.type = type;
        }

        public enum ActionType {
            DEPOSIT,
            WITHDRAW
        }
    }

    /**
     * Procesa el depósito de oro
     */
    public static void processDeposit(Player player, Clan clan, long amount) {
        if (amount <= 0) {
            player.sendMessage("§c✗ La cantidad debe ser mayor a 0");
            return;
        }

        // Aquí verificarías si el jugador tiene el oro necesario
        // Por simplicidad, asumimos que siempre tiene el oro
        // En producción, integrarías con un plugin de economía como Vault

        try {
            clan.addGold(amount);
            TunamaRPG.getInstance().getClanManager().updateClanGold(clan.getId(), clan.getGold());

            // Registrar log
            ClanLog log = new ClanLog(
                clan.getId(),
                player.getUniqueId(),
                player.getName(),
                "DEPOSITO_ORO",
                amount,
                null
            );
            TunamaRPG.getInstance().getClanManager().addLog(log);

            player.sendMessage("§a✓ Has depositado §6" + amount + " oro §aal clan");

        } catch (SQLException e) {
            player.sendMessage("§c✗ Error al depositar oro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Procesa el retiro de oro
     */
    public static void processWithdraw(Player player, Clan clan, long amount) {
        if (amount <= 0) {
            player.sendMessage("§c✗ La cantidad debe ser mayor a 0");
            return;
        }

        if (clan.getGold() < amount) {
            player.sendMessage("§c✗ El clan no tiene suficiente oro");
            player.sendMessage("§7Disponible: §6" + clan.getGold());
            return;
        }

        try {
            clan.removeGold(amount);
            TunamaRPG.getInstance().getClanManager().updateClanGold(clan.getId(), clan.getGold());

            // Registrar log
            ClanLog log = new ClanLog(
                clan.getId(),
                player.getUniqueId(),
                player.getName(),
                "RETIRO_ORO",
                amount,
                null
            );
            TunamaRPG.getInstance().getClanManager().addLog(log);

            player.sendMessage("§a✓ Has retirado §6" + amount + " oro §adel clan");

        } catch (SQLException e) {
            player.sendMessage("§c✗ Error al retirar oro: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
