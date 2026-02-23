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
 * Menú principal del clan para miembros
 */
public class ClanMainMenu implements InventoryHolder {
    private final TunamaRPG plugin;
    private final Clan clan;
    private Inventory inventory;

    public ClanMainMenu(TunamaRPG plugin, Clan clan) {
        this.plugin = plugin;
        this.clan = clan;
    }

    public void open(Player player) {
        inventory = Bukkit.createInventory(this, 54, "§6§lCLAN: " + clan.getFormattedTag());

        Clan.ClanRank playerRank = clan.getMemberRank(player.getUniqueId());

        // Información del clan (slot 4)
        ItemStack info = new ItemStack(Material.WHITE_BANNER);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§6§l" + clan.getName());
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Tag: " + clan.getFormattedTag());
            lore.add("§7Tu Rango: §e" + (playerRank != null ? playerRank.getDisplayName() : "Miembro"));

            Player leader = Bukkit.getPlayer(clan.getLeaderId());
            String leaderName = leader != null ? leader.getName() : "Desconocido";
            lore.add("§7Líder: §f" + leaderName);

            lore.add("§7Miembros: §f" + clan.getMemberCount());

            // Calcular posición en top
            List<Clan> topGold = plugin.getClanManager().getTopClansByGold(100);
            int posGold = -1;
            for (int i = 0; i < topGold.size(); i++) {
                if (topGold.get(i).getId() == clan.getId()) {
                    posGold = i + 1;
                    break;
                }
            }

            List<Clan> topKills = plugin.getClanManager().getTopClansByPvpKills(100);
            int posKills = -1;
            for (int i = 0; i < topKills.size(); i++) {
                if (topKills.get(i).getId() == clan.getId()) {
                    posKills = i + 1;
                    break;
                }
            }

            lore.add("§7Posición (Oro): §e#" + (posGold > 0 ? posGold : "N/A"));
            lore.add("§7Posición (Kills): §e#" + (posKills > 0 ? posKills : "N/A"));
            lore.add("§7Oro del Clan: §6" + clan.getGold());
            lore.add("");
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        inventory.setItem(4, info);

        // Miembros (slot 20)
        ItemStack members = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta membersMeta = members.getItemMeta();
        if (membersMeta != null) {
            membersMeta.setDisplayName("§9§lMiembros del Clan");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Total de miembros: §f" + clan.getMemberCount());
            lore.add("");
            lore.add("§e» Click para ver todos los miembros");
            membersMeta.setLore(lore);
            members.setItemMeta(membersMeta);
        }
        inventory.setItem(20, members);

        // Banco (slot 22)
        ItemStack bank = new ItemStack(Material.CHEST);
        ItemMeta bankMeta = bank.getItemMeta();
        if (bankMeta != null) {
            bankMeta.setDisplayName("§6§lBanco del Clan");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Oro disponible: §6" + clan.getGold());
            lore.add("");
            lore.add("§7Almacena items y oro compartido");
            lore.add("§7entre todos los miembros del clan");
            lore.add("");
            lore.add("§e» Click para abrir el banco");
            bankMeta.setLore(lore);
            bank.setItemMeta(bankMeta);
        }
        inventory.setItem(22, bank);

        // Alianzas (slot 24)
        ItemStack allies = new ItemStack(Material.EMERALD);
        ItemMeta alliesMeta = allies.getItemMeta();
        if (alliesMeta != null) {
            alliesMeta.setDisplayName("§a§lAlianzas");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Clanes aliados: §a" + clan.getAllies().size());
            lore.add("");
            for (String allyTag : clan.getAllies()) {
                Clan allyClan = plugin.getClanManager().getClanByTag(allyTag);
                if (allyClan != null) {
                    lore.add("§a• " + allyClan.getFormattedTag() + " §f" + allyClan.getName());
                }
            }
            lore.add("");
            lore.add("§e» Click para ver alianzas");
            alliesMeta.setLore(lore);
            allies.setItemMeta(alliesMeta);
        }
        inventory.setItem(24, allies);

        // Guerras (slot 30)
        ItemStack wars = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta warsMeta = wars.getItemMeta();
        if (warsMeta != null) {
            warsMeta.setDisplayName("§c§lGuerras");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Clanes enemigos: §c" + clan.getEnemies().size());
            lore.add("");
            for (String enemyTag : clan.getEnemies()) {
                Clan enemyClan = plugin.getClanManager().getClanByTag(enemyTag);
                if (enemyClan != null) {
                    lore.add("§c• " + enemyClan.getFormattedTag() + " §f" + enemyClan.getName());
                }
            }
            lore.add("");
            lore.add("§e» Click para ver enemigos");
            warsMeta.setLore(lore);
            wars.setItemMeta(warsMeta);
        }
        inventory.setItem(30, wars);

        // Gestión (solo para líder y comandantes) (slot 32)
        if (playerRank == Clan.ClanRank.LEADER || playerRank == Clan.ClanRank.COMMANDER) {
            ItemStack manage = new ItemStack(Material.COMMAND_BLOCK);
            ItemMeta manageMeta = manage.getItemMeta();
            if (manageMeta != null) {
                manageMeta.setDisplayName("§5§lGestión del Clan");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Panel de administración del clan");
                lore.add("");
                lore.add("§e» Click para gestionar");
                manageMeta.setLore(lore);
                manage.setItemMeta(manageMeta);
            }
            inventory.setItem(32, manage);
        }

        // Salir del clan (slot 49) - Solo si no es líder
        if (!clan.getLeaderId().equals(player.getUniqueId())) {
            ItemStack leave = new ItemStack(Material.BARRIER);
            ItemMeta leaveMeta = leave.getItemMeta();
            if (leaveMeta != null) {
                leaveMeta.setDisplayName("§c§lSalir del Clan");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Abandonar el clan actual");
                lore.add("");
                lore.add("§c» Click para salir");
                leaveMeta.setLore(lore);
                leave.setItemMeta(leaveMeta);
            }
            inventory.setItem(49, leave);
        }

        player.openInventory(inventory);
    }

    public Clan getClan() {
        return clan;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
