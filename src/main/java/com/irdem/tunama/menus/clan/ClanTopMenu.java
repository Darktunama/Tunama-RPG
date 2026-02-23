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

/**
 * Menú del top 20 de clanes
 */
public class ClanTopMenu implements InventoryHolder {
    private final TunamaRPG plugin;
    private Inventory inventory;
    private TopType currentType = TopType.GOLD;

    public ClanTopMenu(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, TopType.GOLD);
    }

    public void open(Player player, TopType type) {
        this.currentType = type;
        inventory = Bukkit.createInventory(this, 54, "§6§lTOP 20 CLANES");

        // Botón cambiar a Top Oro (slot 3)
        ItemStack goldButton = new ItemStack(Material.GOLD_INGOT);
        ItemMeta goldMeta = goldButton.getItemMeta();
        if (goldMeta != null) {
            goldMeta.setDisplayName(type == TopType.GOLD ? "§6§l» Top por Oro «" : "§7Top por Oro");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Los 20 clanes con más oro");
            lore.add("");
            if (type != TopType.GOLD) {
                lore.add("§e» Click para cambiar");
            } else {
                lore.add("§a§lViendo ahora");
            }
            goldMeta.setLore(lore);
            goldButton.setItemMeta(goldMeta);
        }
        inventory.setItem(3, goldButton);

        // Botón cambiar a Top Kills (slot 5)
        ItemStack killsButton = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta killsMeta = killsButton.getItemMeta();
        if (killsMeta != null) {
            killsMeta.setDisplayName(type == TopType.KILLS ? "§c§l» Top por Kills PvP «" : "§7Top por Kills PvP");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Los 20 clanes con más kills");
            lore.add("");
            if (type != TopType.KILLS) {
                lore.add("§e» Click para cambiar");
            } else {
                lore.add("§a§lViendo ahora");
            }
            killsMeta.setLore(lore);
            killsButton.setItemMeta(killsMeta);
        }
        inventory.setItem(5, killsButton);

        // Obtener lista de clanes según el tipo
        List<Clan> topClans;
        if (type == TopType.GOLD) {
            topClans = plugin.getClanManager().getTopClansByGold(20);
        } else {
            topClans = plugin.getClanManager().getTopClansByPvpKills(20);
        }

        // Mostrar top clanes (slots 9-44)
        int slot = 9;
        int position = 1;
        for (Clan clan : topClans) {
            if (slot >= 45) break;

            Material material;
            String prefix;
            if (position == 1) {
                material = Material.GOLD_BLOCK;
                prefix = "§6§l#1 ";
            } else if (position == 2) {
                material = Material.IRON_BLOCK;
                prefix = "§7§l#2 ";
            } else if (position == 3) {
                material = Material.COPPER_BLOCK;
                prefix = "§c§l#3 ";
            } else {
                material = Material.PAPER;
                prefix = "§f#" + position + " ";
            }

            ItemStack clanItem = new ItemStack(material);
            ItemMeta clanMeta = clanItem.getItemMeta();

            if (clanMeta != null) {
                clanMeta.setDisplayName(prefix + "§f" + clan.getName());

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Tag: " + clan.getFormattedTag());

                Player leader = Bukkit.getPlayer(clan.getLeaderId());
                String leaderName = leader != null ? leader.getName() : "Desconocido";
                lore.add("§7Líder: §f" + leaderName);

                lore.add("§7Miembros: §f" + clan.getMemberCount());

                if (type == TopType.GOLD) {
                    lore.add("§7Oro: §6" + clan.getGold());
                } else {
                    lore.add("§7Kills PvP: §c" + clan.getPvpKills());
                }

                lore.add("");
                clanMeta.setLore(lore);
                clanItem.setItemMeta(clanMeta);
            }

            inventory.setItem(slot, clanItem);
            slot++;
            position++;
        }

        // Botón volver (slot 49)
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c« Volver");
            back.setItemMeta(backMeta);
        }
        inventory.setItem(49, back);

        player.openInventory(inventory);
    }

    public TopType getCurrentType() {
        return currentType;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public enum TopType {
        GOLD,
        KILLS
    }
}
