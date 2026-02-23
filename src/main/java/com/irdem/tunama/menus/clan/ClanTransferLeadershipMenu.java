package com.irdem.tunama.menus.clan;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.Clan;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Menú para transferir el liderazgo del clan (solo líder)
 */
public class ClanTransferLeadershipMenu implements InventoryHolder {
    private final TunamaRPG plugin;
    private final Clan clan;
    private final Player leader;
    private Inventory inventory;
    private List<UUID> commanderList;

    // Mapa estático para almacenar transferencias pendientes
    private static final Map<UUID, PendingTransfer> pendingTransfers = new HashMap<>();

    private boolean adminMode;

    public ClanTransferLeadershipMenu(TunamaRPG plugin, Clan clan, Player leader) {
        this.plugin = plugin;
        this.clan = clan;
        this.leader = leader;
        this.adminMode = false;
    }

    public void open(Player player) {
        // Detectar si es admin y no es miembro del clan
        this.adminMode = player.hasPermission("rpg.admin") && !clan.isMember(player.getUniqueId());

        inventory = Bukkit.createInventory(this, 54, adminMode ? "§c§lADMIN - Cambiar Líder" : "§6§lTransferir Liderazgo");

        // En modo admin mostrar TODOS los miembros, en modo normal solo comandantes
        commanderList = new ArrayList<>();
        for (Map.Entry<UUID, Clan.ClanRank> entry : clan.getMembers().entrySet()) {
            if (adminMode || entry.getValue() == Clan.ClanRank.COMMANDER) {
                commanderList.add(entry.getKey());
            }
        }

        if (commanderList.isEmpty()) {
            ItemStack noMembers = new ItemStack(Material.BARRIER);
            ItemMeta noMembersMeta = noMembers.getItemMeta();
            if (noMembersMeta != null) {
                noMembersMeta.setDisplayName(adminMode ? "§7Sin miembros" : "§7Sin comandantes");
                List<String> lore = new ArrayList<>();
                lore.add("");
                if (adminMode) {
                    lore.add("§7No hay miembros en el clan");
                } else {
                    lore.add("§7No hay comandantes en el clan");
                    lore.add("§7Primero debes promover a alguien");
                    lore.add("§7a comandante para transferir el liderazgo");
                }
                lore.add("");
                noMembersMeta.setLore(lore);
                noMembers.setItemMeta(noMembersMeta);
            }
            inventory.setItem(22, noMembers);
        } else {
            int slot = 9;
            for (UUID memberId : commanderList) {
                if (slot >= 45) break;

                ItemStack memberItem = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) memberItem.getItemMeta();

                if (meta != null) {
                    Player member = Bukkit.getPlayer(memberId);
                    org.bukkit.OfflinePlayer offlineMember = Bukkit.getOfflinePlayer(memberId);
                    String memberName = member != null ? member.getName() :
                        (offlineMember.getName() != null ? offlineMember.getName() : memberId.toString().substring(0, 8));

                    if (member != null) {
                        meta.setOwningPlayer(member);
                    } else {
                        meta.setOwningPlayer(offlineMember);
                    }

                    Clan.ClanRank rank = clan.getMemberRank(memberId);
                    String rankDisplay = rank != null ? rank.getDisplayName() : "Desconocido";

                    meta.setDisplayName("§e§l⚔ " + memberName);

                    List<String> lore = new ArrayList<>();
                    lore.add("");
                    lore.add("§7Rango: §e" + rankDisplay);
                    lore.add("§7Estado: " + (member != null && member.isOnline() ? "§aConectado" : "§cDesconectado"));
                    lore.add("");
                    lore.add("§6§l✓ CLICK IZQUIERDO");
                    lore.add("§7Transferir el liderazgo a este jugador");
                    lore.add("");
                    lore.add("§c§lADVERTENCIA: Esta acción es irreversible");
                    lore.add("");
                    meta.setLore(lore);

                    memberItem.setItemMeta(meta);
                }

                inventory.setItem(slot, memberItem);
                slot++;
            }
        }

        // Botón volver (slot 49)
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c◄ Volver");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Volver al menú de gestión");
            lore.add("");
            backMeta.setLore(lore);
            back.setItemMeta(backMeta);
        }
        inventory.setItem(49, back);

        player.openInventory(inventory);
    }

    public Clan getClan() {
        return clan;
    }

    public Player getLeader() {
        return leader;
    }

    /**
     * Obtiene el UUID del comandante en un slot específico
     */
    public UUID getCommanderUuidAtSlot(int slot) {
        if (slot < 9 || slot >= 45) return null;

        int index = slot - 9;
        if (index >= commanderList.size()) return null;

        return commanderList.get(index);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Clase para almacenar transferencias pendientes
     */
    public static class PendingTransfer {
        private final Clan clan;
        private final UUID newLeaderId;
        private final String newLeaderName;

        public PendingTransfer(Clan clan, UUID newLeaderId, String newLeaderName) {
            this.clan = clan;
            this.newLeaderId = newLeaderId;
            this.newLeaderName = newLeaderName;
        }

        public Clan getClan() {
            return clan;
        }

        public UUID getNewLeaderId() {
            return newLeaderId;
        }

        public String getNewLeaderName() {
            return newLeaderName;
        }
    }

    /**
     * Añade una transferencia pendiente
     */
    public static void addPendingTransfer(UUID leaderId, PendingTransfer transfer) {
        pendingTransfers.put(leaderId, transfer);
    }

    /**
     * Obtiene una transferencia pendiente
     */
    public static PendingTransfer getPendingTransfer(UUID leaderId) {
        return pendingTransfers.get(leaderId);
    }

    /**
     * Elimina una transferencia pendiente
     */
    public static void removePendingTransfer(UUID leaderId) {
        pendingTransfers.remove(leaderId);
    }
}
