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
 * Menú de gestión del clan (para líder y comandantes)
 */
public class ClanManageMenu implements InventoryHolder {
    private final TunamaRPG plugin;
    private final Clan clan;
    private final Player manager;
    private Inventory inventory;

    public ClanManageMenu(TunamaRPG plugin, Clan clan, Player manager) {
        this.plugin = plugin;
        this.clan = clan;
        this.manager = manager;
    }

    public void open(Player player) {
        inventory = Bukkit.createInventory(this, 27, "§5§lGestión - " + clan.getName());

        boolean isLeader = clan.getLeaderId().equals(player.getUniqueId());

        // Expulsar miembros (slot 10)
        ItemStack kick = new ItemStack(Material.IRON_DOOR);
        ItemMeta kickMeta = kick.getItemMeta();
        if (kickMeta != null) {
            kickMeta.setDisplayName("§c§lExpulsar Miembros");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Expulsa miembros del clan");
            if (!isLeader) {
                lore.add("");
                lore.add("§c⚠ No puedes expulsar comandantes");
            }
            lore.add("");
            lore.add("§e» Click para expulsar");
            kickMeta.setLore(lore);
            kick.setItemMeta(kickMeta);
        }
        inventory.setItem(10, kick);

        // Ver logs (slot 12)
        ItemStack logs = new ItemStack(Material.BOOK);
        ItemMeta logsMeta = logs.getItemMeta();
        if (logsMeta != null) {
            logsMeta.setDisplayName("§6§lVer Logs");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Registro de actividad del banco");
            lore.add("§7Depósitos y retiros de oro/items");
            lore.add("");
            lore.add("§e» Click para ver logs");
            logsMeta.setLore(lore);
            logs.setItemMeta(logsMeta);
        }
        inventory.setItem(12, logs);

        // Gestión de alianzas (slot 14)
        ItemStack allies = new ItemStack(Material.EMERALD);
        ItemMeta alliesMeta = allies.getItemMeta();
        if (alliesMeta != null) {
            alliesMeta.setDisplayName("§a§lGestión de Alianzas");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Clanes aliados: §a" + clan.getAllies().size());
            lore.add("");
            lore.add("§7Click derecho para retirar alianza");
            lore.add("");
            lore.add("§e» Click para gestionar");
            alliesMeta.setLore(lore);
            allies.setItemMeta(alliesMeta);
        }
        inventory.setItem(14, allies);

        // Gestión de guerras (slot 16)
        ItemStack wars = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta warsMeta = wars.getItemMeta();
        if (warsMeta != null) {
            warsMeta.setDisplayName("§c§lGestión de Guerras");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Clanes enemigos: §c" + clan.getEnemies().size());
            lore.add("");
            lore.add("§7Click derecho para retirar guerra");
            lore.add("");
            lore.add("§e» Click para gestionar");
            warsMeta.setLore(lore);
            wars.setItemMeta(warsMeta);
        }
        inventory.setItem(16, wars);

        // Gestión de miembros (solo líder) (slot 20)
        if (isLeader) {
            ItemStack memberManage = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta memberManageMeta = memberManage.getItemMeta();
            if (memberManageMeta != null) {
                memberManageMeta.setDisplayName("§e§lGestión de Miembros");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Promover o degradar miembros");
                lore.add("");
                lore.add("§a§lCLICK IZQ §7- Promover a Comandante");
                lore.add("§c§lCLICK DER §7- Degradar de Comandante");
                lore.add("");
                lore.add("§e» Click para gestionar rangos");
                memberManageMeta.setLore(lore);
                memberManage.setItemMeta(memberManageMeta);
            }
            inventory.setItem(20, memberManage);
        }

        // Disolver clan (solo líder) (slot 22)
        if (isLeader) {
            ItemStack dissolve = new ItemStack(Material.TNT);
            ItemMeta dissolveMeta = dissolve.getItemMeta();
            if (dissolveMeta != null) {
                dissolveMeta.setDisplayName("§4§lDisolver Clan");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§c§lADVERTENCIA: Esta acción es irreversible");
                lore.add("");
                lore.add("§7Elimina permanentemente el clan");
                lore.add("§7y todos sus datos");
                lore.add("");
                lore.add("§c» Click para disolver");
                dissolveMeta.setLore(lore);
                dissolve.setItemMeta(dissolveMeta);
            }
            inventory.setItem(22, dissolve);
        }

        // Transferir liderazgo (solo líder) (slot 24)
        if (isLeader) {
            ItemStack transfer = new ItemStack(Material.GOLDEN_HELMET);
            ItemMeta transferMeta = transfer.getItemMeta();
            if (transferMeta != null) {
                transferMeta.setDisplayName("§6§lTransferir Liderazgo");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Transfiere el liderazgo del clan");
                lore.add("§7a otro comandante");
                lore.add("");
                lore.add("§c§lADVERTENCIA: No podrás recuperarlo");
                lore.add("");
                lore.add("§e» Click para transferir");
                transferMeta.setLore(lore);
                transfer.setItemMeta(transferMeta);
            }
            inventory.setItem(24, transfer);
        }

        // Botón volver (slot 18)
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
        inventory.setItem(18, back);

        player.openInventory(inventory);
    }

    public Clan getClan() {
        return clan;
    }

    public Player getManager() {
        return manager;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
