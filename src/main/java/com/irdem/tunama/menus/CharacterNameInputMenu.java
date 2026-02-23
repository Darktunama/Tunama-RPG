package com.irdem.tunama.menus;

import com.irdem.tunama.TunamaRPG;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Menú de yunque para ingresar el nombre del personaje
 */
public class CharacterNameInputMenu {

    private final TunamaRPG plugin;
    private final Player player;
    private final String raceId;
    private final String classId;

    public CharacterNameInputMenu(TunamaRPG plugin, Player player, String raceId, String classId) {
        this.plugin = plugin;
        this.player = player;
        this.raceId = raceId;
        this.classId = classId;
    }

    /**
     * Abre el yunque para que el jugador ingrese el nombre
     */
    public void open() {
        // Crear item de papel con instrucciones
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Ingresa el nombre del personaje");
            List<String> lore = new ArrayList<>();
            lore.add("§7Escribe el nombre de tu personaje");
            lore.add("§7en el yunque y haz clic en el resultado");
            lore.add("");
            lore.add("§7Requisitos:");
            lore.add("§7• Mínimo 3 caracteres");
            lore.add("§7• Máximo 16 caracteres");
            lore.add("§7• Solo letras, números y guiones bajos");
            lore.add("");
            lore.add("§c⚠ Si sales sin ingresar un nombre,");
            lore.add("§cel personaje no será guardado");
            meta.setLore(lore);
            paper.setItemMeta(meta);
        }

        // Usar Paper's Anvil API - No hacer cast, simplemente abrir el inventario
        net.kyori.adventure.text.Component title = net.kyori.adventure.text.Component.text("Nombre del Personaje");

        org.bukkit.inventory.Inventory anvilInv =
            org.bukkit.Bukkit.createInventory(null, org.bukkit.event.inventory.InventoryType.ANVIL, title);

        anvilInv.setItem(0, paper);

        player.openInventory(anvilInv);

        // El listener AnvilInputListener manejará la entrada del usuario
    }

    public String getRaceId() {
        return raceId;
    }

    public String getClassId() {
        return classId;
    }

    public Player getPlayer() {
        return player;
    }
}
