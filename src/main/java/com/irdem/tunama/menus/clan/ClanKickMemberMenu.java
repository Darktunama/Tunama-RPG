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
 * Menú para expulsar miembros del clan
 */
public class ClanKickMemberMenu implements InventoryHolder {
    private final TunamaRPG plugin;
    private final Clan clan;
    private final Player kicker;
    private Inventory inventory;
    private final Map<Integer, UUID> slotToMemberUuid = new HashMap<>();

    public ClanKickMemberMenu(TunamaRPG plugin, Clan clan, Player kicker) {
        this.plugin = plugin;
        this.clan = clan;
        this.kicker = kicker;
    }

    public void open(Player player) {
        inventory = Bukkit.createInventory(this, 54, "§c§lExpulsar Miembro");

        boolean isLeader = clan.getLeaderId().equals(player.getUniqueId());
        Clan.ClanRank kickerRank = clan.getMemberRank(player.getUniqueId());

        List<UUID> kickableMembers = new ArrayList<>();

        // Solo mostrar miembros que se pueden expulsar
        for (UUID memberId : clan.getMembers().keySet()) {
            // No puedes expulsarte a ti mismo
            if (memberId.equals(player.getUniqueId())) continue;

            // No puedes expulsar al líder
            if (memberId.equals(clan.getLeaderId())) continue;

            // Si eres comandante, no puedes expulsar a otros comandantes
            if (!isLeader && kickerRank == Clan.ClanRank.COMMANDER) {
                Clan.ClanRank memberRank = clan.getMemberRank(memberId);
                if (memberRank == Clan.ClanRank.COMMANDER) continue;
            }

            kickableMembers.add(memberId);
        }

        if (kickableMembers.isEmpty()) {
            // No hay miembros para expulsar
            ItemStack noMembers = new ItemStack(Material.BARRIER);
            ItemMeta noMembersMeta = noMembers.getItemMeta();
            if (noMembersMeta != null) {
                noMembersMeta.setDisplayName("§7No hay miembros para expulsar");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7No hay miembros que puedas expulsar");
                lore.add("");
                noMembersMeta.setLore(lore);
                noMembers.setItemMeta(noMembersMeta);
            }
            inventory.setItem(22, noMembers);
        } else {
            // Mostrar miembros expulsables (slots 9-44)
            slotToMemberUuid.clear();
            int slot = 9;
            for (UUID memberId : kickableMembers) {
                if (slot >= 45) break;

                Clan.ClanRank rank = clan.getMemberRank(memberId);

                ItemStack memberItem = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) memberItem.getItemMeta();

                if (meta != null) {
                    // Usar OfflinePlayer para obtener nombre y establecer skin incluso si está offline
                    org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberId);
                    String memberName = offlinePlayer.getName() != null ? offlinePlayer.getName() : memberId.toString().substring(0, 8);

                    meta.setOwningPlayer(offlinePlayer);

                    String displayName;
                    if (rank == Clan.ClanRank.COMMANDER) {
                        displayName = "§e⚔ " + memberName + " §7(Comandante)";
                    } else {
                        displayName = "§f" + memberName;
                    }

                    meta.setDisplayName(displayName);

                    List<String> lore = new ArrayList<>();
                    lore.add("");
                    lore.add("§7Rango: §e" + (rank != null ? rank.getDisplayName() : "Miembro"));
                    lore.add("§7Estado: " + (offlinePlayer.isOnline() ? "§aConectado" : "§cDesconectado"));
                    lore.add("");
                    lore.add("§c§l⚠ ADVERTENCIA");
                    lore.add("§7Click para expulsar a este miembro");
                    lore.add("");
                    meta.setLore(lore);

                    memberItem.setItemMeta(meta);
                }

                // Guardar mapeo de slot -> UUID
                slotToMemberUuid.put(slot, memberId);
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

    public Player getKicker() {
        return kicker;
    }

    /**
     * Obtiene el UUID del miembro en un slot específico
     * @param slot El slot del inventario
     * @return El UUID del miembro, o null si no hay miembro en ese slot
     */
    public UUID getMemberUuidAtSlot(int slot) {
        return slotToMemberUuid.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
