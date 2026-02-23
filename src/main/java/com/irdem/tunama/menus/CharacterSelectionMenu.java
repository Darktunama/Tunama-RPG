package com.irdem.tunama.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.CharacterManager;
import com.irdem.tunama.data.CharacterManager.CharacterInfo;

import java.util.ArrayList;
import java.util.List;

public class CharacterSelectionMenu implements InventoryHolder {

    private TunamaRPG plugin;
    private Inventory inventory;

    public CharacterSelectionMenu(TunamaRPG plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 54, "§6§lMis Personajes");
    }

    public void open(Player player) {
        CharacterManager charManager = plugin.getDatabaseManager().getCharacterManager();
        int maxCharacters = charManager.getMaxCharacters(player);
        int activeSlot = charManager.getActiveSlot(player.getUniqueId());
        List<CharacterInfo> characters = charManager.getCharacters(player.getUniqueId());

        // Limpiar inventario
        inventory.clear();

        // Mostrar personajes existentes y slots vacíos
        for (int slot = 1; slot <= maxCharacters && slot <= 20; slot++) {
            final int currentSlot = slot;
            CharacterInfo character = characters.stream()
                .filter(c -> c.slot == currentSlot)
                .findFirst()
                .orElse(null);

            int inventorySlot = getInventorySlot(slot);
            if (inventorySlot >= 0 && inventorySlot < 54) {
                ItemStack item;
                if (character != null && !character.isEmpty()) {
                    // Personaje existente
                    item = createCharacterItem(character, activeSlot == slot);
                } else {
                    // Slot vacío
                    item = createEmptySlotItem(slot);
                }
                inventory.setItem(inventorySlot, item);
            }
        }

        // Botón de información en el centro inferior
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§6§lInformación");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Personajes: §f" + characters.size() + "§7/§f" + maxCharacters);
        infoLore.add("§7Slot activo: §f" + activeSlot);
        infoLore.add("");
        infoLore.add("§eHaz clic en un personaje para cambiarlo");
        infoLore.add("§eHaz clic en un slot vacío para crear uno nuevo");
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(49, infoItem);

        // Botón de volver en parte inferior izquierda
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c✖ Volver");
        List<String> backLore = new ArrayList<>();
        backLore.add("§7Volver al menú principal");
        backMeta.setLore(backLore);
        backButton.setItemMeta(backMeta);
        inventory.setItem(45, backButton);

        player.openInventory(inventory);
    }

    private ItemStack createCharacterItem(CharacterInfo character, boolean isActive) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        String displayName = isActive ? "§a§l✓ " + character.username + " (Activo)" : "§f" + character.username;
        meta.setDisplayName(displayName);

        List<String> lore = new ArrayList<>();
        lore.add("§7Slot: §f" + character.slot);
        lore.add("§7Raza: §f" + character.race);
        lore.add("§7Clase: §f" + character.playerClass);
        lore.add("§7Nivel: §f" + character.level);
        lore.add("§7Experiencia: §f" + character.experience);
        lore.add("");
        if (isActive) {
            lore.add("§a§lPersonaje activo");
        } else {
            lore.add("§eHaz clic para cambiar a este personaje");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createEmptySlotItem(int slot) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aSlot " + slot + " §8(Disponible)");
        List<String> lore = new ArrayList<>();
        lore.add("§7Este espacio está libre para");
        lore.add("§7crear un nuevo personaje");
        lore.add("");
        lore.add("§e§l▶ Haz clic para crear un personaje");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private int getInventorySlot(int characterSlot) {
        // Distribuir los personajes en el inventario (3 filas de 7 = 21 slots)
        // Fila 1: slots 10-16 (personajes 1-7)
        // Fila 2: slots 19-25 (personajes 8-14)
        // Fila 3: slots 28-34 (personajes 15-20, solo hasta 20)

        if (characterSlot >= 1 && characterSlot <= 7) {
            return 10 + (characterSlot - 1);
        } else if (characterSlot >= 8 && characterSlot <= 14) {
            return 19 + (characterSlot - 8);
        } else if (characterSlot >= 15 && characterSlot <= 20) {
            return 28 + (characterSlot - 15);
        }
        return -1;
    }

    public static int getCharacterSlotFromInventory(int inventorySlot) {
        // Fila 1: slots 10-16
        if (inventorySlot >= 10 && inventorySlot <= 16) {
            return 1 + (inventorySlot - 10);
        }
        // Fila 2: slots 19-25
        if (inventorySlot >= 19 && inventorySlot <= 25) {
            return 8 + (inventorySlot - 19);
        }
        // Fila 3: slots 28-34
        if (inventorySlot >= 28 && inventorySlot <= 34) {
            return 15 + (inventorySlot - 28);
        }
        return -1;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
