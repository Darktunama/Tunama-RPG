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
 * Menú de administración del clan (solo para admins)
 */
public class ClanAdminMenu implements InventoryHolder {
    private final TunamaRPG plugin;
    private final Clan clan;
    private Inventory inventory;

    public ClanAdminMenu(TunamaRPG plugin, Clan clan) {
        this.plugin = plugin;
        this.clan = clan;
    }

    public void open(Player player) {
        inventory = Bukkit.createInventory(this, 27, "§c§lADMIN - " + clan.getName());

        // Expulsar jugadores (slot 10)
        ItemStack kick = new ItemStack(Material.IRON_DOOR);
        ItemMeta kickMeta = kick.getItemMeta();
        if (kickMeta != null) {
            kickMeta.setDisplayName("§c§lExpulsar Jugadores");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Expulsa cualquier jugador del clan");
            lore.add("");
            lore.add("§e» Click para expulsar");
            kickMeta.setLore(lore);
            kick.setItemMeta(kickMeta);
        }
        inventory.setItem(10, kick);

        // Cambiar líder (slot 11)
        ItemStack changeLeader = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta leaderMeta = changeLeader.getItemMeta();
        if (leaderMeta != null) {
            leaderMeta.setDisplayName("§6§lCambiar Líder");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Transfiere el liderazgo del clan");
            lore.add("§7a otro miembro");
            lore.add("");
            lore.add("§e» Click para cambiar");
            leaderMeta.setLore(lore);
            changeLeader.setItemMeta(leaderMeta);
        }
        inventory.setItem(11, changeLeader);

        // Acceder al banco (slot 13)
        ItemStack bank = new ItemStack(Material.CHEST);
        ItemMeta bankMeta = bank.getItemMeta();
        if (bankMeta != null) {
            bankMeta.setDisplayName("§6§lAcceder al Banco");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Accede al banco del clan");
            lore.add("§7Oro: §6" + clan.getGold());
            lore.add("");
            lore.add("§e» Click para abrir");
            bankMeta.setLore(lore);
            bank.setItemMeta(bankMeta);
        }
        inventory.setItem(13, bank);

        // Ver logs (slot 15)
        ItemStack logs = new ItemStack(Material.BOOK);
        ItemMeta logsMeta = logs.getItemMeta();
        if (logsMeta != null) {
            logsMeta.setDisplayName("§9§lVer Logs");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Registro completo de actividad");
            lore.add("§7del banco del clan");
            lore.add("");
            lore.add("§e» Click para ver");
            logsMeta.setLore(lore);
            logs.setItemMeta(logsMeta);
        }
        inventory.setItem(15, logs);

        // Eliminar clan (slot 16)
        ItemStack delete = new ItemStack(Material.TNT);
        ItemMeta deleteMeta = delete.getItemMeta();
        if (deleteMeta != null) {
            deleteMeta.setDisplayName("§4§lEliminar Clan");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§c§lADVERTENCIA: Acción irreversible");
            lore.add("");
            lore.add("§7Elimina permanentemente el clan");
            lore.add("");
            lore.add("§c» Click para eliminar");
            deleteMeta.setLore(lore);
            delete.setItemMeta(deleteMeta);
        }
        inventory.setItem(16, delete);

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
