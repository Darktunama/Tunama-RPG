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
 * Menú para ver las guerras del clan
 */
public class ClanWarsMenu implements InventoryHolder {
    private final TunamaRPG plugin;
    private final Clan clan;
    private final Player viewer;
    private Inventory inventory;

    public ClanWarsMenu(TunamaRPG plugin, Clan clan, Player viewer) {
        this.plugin = plugin;
        this.clan = clan;
        this.viewer = viewer;
    }

    public void open(Player player) {
        inventory = Bukkit.createInventory(this, 54, "§c§lGuerras - " + clan.getName());

        Set<String> enemies = clan.getEnemies();

        // Verificar si el jugador puede cancelar guerras (líder o comandante)
        Clan.ClanRank rank = clan.getMemberRank(player.getUniqueId());
        boolean canManage = (rank == Clan.ClanRank.LEADER || rank == Clan.ClanRank.COMMANDER);

        if (enemies.isEmpty()) {
            // Mensaje de que no hay guerras
            ItemStack noWars = new ItemStack(Material.WHITE_BANNER);
            ItemMeta noWarsMeta = noWars.getItemMeta();
            if (noWarsMeta != null) {
                noWarsMeta.setDisplayName("§7Sin guerras");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Tu clan no tiene guerras actualmente");
                lore.add("§a¡Tiempos de paz!");
                lore.add("");
                noWarsMeta.setLore(lore);
                noWars.setItemMeta(noWarsMeta);
            }
            inventory.setItem(22, noWars);
        } else {
            // Mostrar enemigos (slots 9-44)
            int slot = 9;
            for (String enemyTag : enemies) {
                if (slot >= 45) break;

                // Buscar el clan enemigo
                Clan enemyClan = plugin.getClanManager().getClanByTag(enemyTag);

                ItemStack enemyItem = new ItemStack(Material.NETHERITE_SWORD);
                ItemMeta enemyMeta = enemyItem.getItemMeta();

                if (enemyMeta != null) {
                    if (enemyClan != null) {
                        enemyMeta.setDisplayName("§c§l" + enemyClan.getName());
                        List<String> lore = new ArrayList<>();
                        lore.add("");
                        lore.add("§7Tag: " + enemyClan.getFormattedTag());
                        lore.add("§7Miembros: §f" + enemyClan.getMemberCount());
                        lore.add("§7Oro: §6" + enemyClan.getGold());
                        lore.add("§7Kills PvP: §c" + enemyClan.getPvpKills());
                        lore.add("");
                        lore.add("§7Estado: §c⚔ En guerra");
                        lore.add("");
                        if (canManage) {
                            lore.add("§a§l✓ Click para proponer paz");
                            lore.add("");
                        }
                        enemyMeta.setLore(lore);
                    } else {
                        // Clan no encontrado
                        enemyMeta.setDisplayName("§7[" + enemyTag + "]");
                        List<String> lore = new ArrayList<>();
                        lore.add("");
                        lore.add("§cClan no encontrado");
                        lore.add("");
                        if (canManage) {
                            lore.add("§a§l✓ Click para proponer paz");
                            lore.add("");
                        }
                        enemyMeta.setLore(lore);
                    }
                    enemyItem.setItemMeta(enemyMeta);
                }

                inventory.setItem(slot, enemyItem);
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
     * Obtiene el tag del enemigo en un slot específico
     */
    public String getEnemyTagAtSlot(int slot) {
        if (slot < 9 || slot >= 45) return null;

        Set<String> enemies = clan.getEnemies();
        int index = slot - 9;

        if (index >= enemies.size()) return null;

        int i = 0;
        for (String enemyTag : enemies) {
            if (i == index) return enemyTag;
            i++;
        }

        return null;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
