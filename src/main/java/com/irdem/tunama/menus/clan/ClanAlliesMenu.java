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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Menú para ver las alianzas del clan
 */
public class ClanAlliesMenu implements InventoryHolder {
    private final TunamaRPG plugin;
    private final Clan clan;
    private final Player viewer;
    private Inventory inventory;

    public ClanAlliesMenu(TunamaRPG plugin, Clan clan, Player viewer) {
        this.plugin = plugin;
        this.clan = clan;
        this.viewer = viewer;
    }

    public void open(Player player) {
        inventory = Bukkit.createInventory(this, 54, "§a§lAlianzas - " + clan.getName());

        Set<String> allies = clan.getAllies();

        // Verificar si el jugador puede cancelar alianzas (líder o comandante)
        Clan.ClanRank rank = clan.getMemberRank(player.getUniqueId());
        boolean canManage = (rank == Clan.ClanRank.LEADER || rank == Clan.ClanRank.COMMANDER);

        if (allies.isEmpty()) {
            // Mensaje de que no hay alianzas
            ItemStack noAllies = new ItemStack(Material.BARRIER);
            ItemMeta noAlliesMeta = noAllies.getItemMeta();
            if (noAlliesMeta != null) {
                noAlliesMeta.setDisplayName("§7Sin alianzas");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Tu clan no tiene alianzas actualmente");
                lore.add("");
                noAlliesMeta.setLore(lore);
                noAllies.setItemMeta(noAlliesMeta);
            }
            inventory.setItem(22, noAllies);
        } else {
            // Mostrar aliados (slots 9-44)
            int slot = 9;
            for (String allyTag : allies) {
                if (slot >= 45) break;

                // Buscar el clan aliado
                Clan allyClan = plugin.getClanManager().getClanByTag(allyTag);

                ItemStack allyItem = new ItemStack(Material.EMERALD_BLOCK);
                ItemMeta allyMeta = allyItem.getItemMeta();

                if (allyMeta != null) {
                    if (allyClan != null) {
                        allyMeta.setDisplayName("§a§l" + allyClan.getName());
                        List<String> lore = new ArrayList<>();
                        lore.add("");
                        lore.add("§7Tag: " + allyClan.getFormattedTag());
                        lore.add("§7Miembros: §f" + allyClan.getMemberCount());
                        lore.add("§7Oro: §6" + allyClan.getGold());
                        lore.add("");
                        lore.add("§7Estado: §a✓ Aliados");
                        lore.add("");
                        if (canManage) {
                            lore.add("§c§l⚠ Click para cancelar alianza");
                            lore.add("");
                        }
                        allyMeta.setLore(lore);
                    } else {
                        // Clan no encontrado
                        allyMeta.setDisplayName("§7[" + allyTag + "]");
                        List<String> lore = new ArrayList<>();
                        lore.add("");
                        lore.add("§cClan no encontrado");
                        lore.add("");
                        if (canManage) {
                            lore.add("§c§l⚠ Click para cancelar alianza");
                            lore.add("");
                        }
                        allyMeta.setLore(lore);
                    }
                    allyItem.setItemMeta(allyMeta);
                }

                inventory.setItem(slot, allyItem);
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
            lore.add("§7Volver al menú principal");
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

    public Player getViewer() {
        return viewer;
    }

    /**
     * Obtiene el tag del aliado en un slot específico
     */
    public String getAllyTagAtSlot(int slot) {
        if (slot < 9 || slot >= 45) return null;

        Set<String> allies = clan.getAllies();
        int index = slot - 9;

        if (index >= allies.size()) return null;

        int i = 0;
        for (String allyTag : allies) {
            if (i == index) return allyTag;
            i++;
        }

        return null;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
