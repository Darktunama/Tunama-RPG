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
 * Menú para ver los miembros del clan
 */
public class ClanMembersMenu implements InventoryHolder {
    private final TunamaRPG plugin;
    private final Clan clan;
    private Inventory inventory;
    private int currentPage = 0;

    public ClanMembersMenu(TunamaRPG plugin, Clan clan) {
        this.plugin = plugin;
        this.clan = clan;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        this.currentPage = page;
        inventory = Bukkit.createInventory(this, 54, "§6§lMiembros - " + clan.getName());

        List<UUID> memberList = new ArrayList<>(clan.getMembers().keySet());
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
                String memberName = member != null ? member.getName() : "Desconocido";

                if (member != null) {
                    meta.setOwningPlayer(member);
                }

                String displayName;
                if (clan.getLeaderId().equals(memberId)) {
                    displayName = "§6§l★ " + memberName + " §r§7(Líder)";
                } else if (rank == Clan.ClanRank.COMMANDER) {
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

        // Info de página (slot 49)
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageInfoMeta = pageInfo.getItemMeta();
        if (pageInfoMeta != null) {
            pageInfoMeta.setDisplayName("§7Página " + (page + 1) + " de " + Math.max(1, totalPages));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Total de miembros: §f" + memberList.size());
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

        // Botón volver (slot 48)
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c« Volver");
            back.setItemMeta(backMeta);
        }
        inventory.setItem(48, back);

        player.openInventory(inventory);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public Clan getClan() {
        return clan;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
