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
import java.util.List;
import java.util.UUID;

/**
 * Menú para gestionar rangos de miembros del clan (solo líder)
 */
public class ClanMemberManageMenu implements InventoryHolder {
    private final TunamaRPG plugin;
    private final Clan clan;
    private final Player manager;
    private Inventory inventory;
    private int currentPage = 0;
    private List<UUID> memberList;

    public ClanMemberManageMenu(TunamaRPG plugin, Clan clan, Player manager) {
        this.plugin = plugin;
        this.clan = clan;
        this.manager = manager;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        this.currentPage = page;
        inventory = Bukkit.createInventory(this, 54, "§e§lGestión de Miembros");

        // Filtrar miembros (excluir al líder)
        memberList = new ArrayList<>();
        for (UUID memberId : clan.getMembers().keySet()) {
            if (!memberId.equals(clan.getLeaderId())) {
                memberList.add(memberId);
            }
        }

        int totalPages = (int) Math.ceil(memberList.size() / 45.0);

        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, memberList.size());

        // Mostrar miembros
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            UUID memberId = memberList.get(i);
            Clan.ClanRank rank = clan.getMemberRank(memberId);

            ItemStack memberItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) memberItem.getItemMeta();

            if (meta != null) {
                Player member = Bukkit.getPlayer(memberId);
                String memberName = member != null ? member.getName() : memberId.toString().substring(0, 8);

                if (member != null) {
                    meta.setOwningPlayer(member);
                }

                String displayName;
                if (rank == Clan.ClanRank.COMMANDER) {
                    displayName = "§e§l⚔ " + memberName + " §r§7(Comandante)";
                } else {
                    displayName = "§f" + memberName;
                }

                meta.setDisplayName(displayName);

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Rango: §e" + (rank != null ? rank.getDisplayName() : "Miembro"));
                lore.add("§7Estado: " + (member != null && member.isOnline() ? "§aConectado" : "§cDesconectado"));
                lore.add("");

                if (rank == Clan.ClanRank.COMMANDER) {
                    lore.add("§c§lCLICK DERECHO §7- Degradar a Miembro");
                } else {
                    lore.add("§a§lCLICK IZQUIERDO §7- Promover a Comandante");
                }
                lore.add("");
                meta.setLore(lore);

                memberItem.setItemMeta(meta);
            }

            inventory.setItem(slot, memberItem);
            slot++;
        }

        // Botón de página anterior (slot 45)
        if (page > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§e« Página Anterior");
                prevPage.setItemMeta(prevMeta);
            }
            inventory.setItem(45, prevPage);
        }

        // Botón volver (slot 48)
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c« Volver");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Volver al menú de gestión");
            lore.add("");
            backMeta.setLore(lore);
            back.setItemMeta(backMeta);
        }
        inventory.setItem(48, back);

        // Info de página (slot 49)
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageInfoMeta = pageInfo.getItemMeta();
        if (pageInfoMeta != null) {
            pageInfoMeta.setDisplayName("§7Página " + (page + 1) + " de " + Math.max(1, totalPages));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Miembros gestionables: §f" + memberList.size());
            lore.add("");
            lore.add("§a§lCLICK IZQ §7- Promover a Comandante");
            lore.add("§c§lCLICK DER §7- Degradar de Comandante");
            lore.add("");
            pageInfoMeta.setLore(lore);
            pageInfo.setItemMeta(pageInfoMeta);
        }
        inventory.setItem(49, pageInfo);

        // Botón de página siguiente (slot 53)
        if (page < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§ePágina Siguiente »");
                nextPage.setItemMeta(nextMeta);
            }
            inventory.setItem(53, nextPage);
        }

        player.openInventory(inventory);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public Clan getClan() {
        return clan;
    }

    public Player getManager() {
        return manager;
    }

    /**
     * Obtiene el UUID del miembro en un slot específico
     */
    public UUID getMemberUuidAtSlot(int slot) {
        if (slot < 0 || slot >= 45) return null;

        int index = (currentPage * 45) + slot;
        if (index >= memberList.size()) return null;

        return memberList.get(index);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
