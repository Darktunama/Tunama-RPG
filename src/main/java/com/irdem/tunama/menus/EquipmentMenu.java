package com.irdem.tunama.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.PlayerData;
import java.util.ArrayList;
import java.util.List;

public class EquipmentMenu implements InventoryHolder {
    private Inventory inventory;
    private Player player;
    private PlayerData playerData;
    private TunamaRPG plugin;

    /** Materiales para slots vacíos según tipo de objeto - Iconos personalizados */
    private static final Material RING_EMPTY = Material.ENDER_EYE; // Ojo de ender para anillos
    private static final Material NECKLACE_EMPTY = Material.CHAIN; // Cadena para collar
    private static final Material AMULET_EMPTY = Material.TOTEM_OF_UNDYING; // Totem para amuletos
    private static final Material WINGS_EMPTY = Material.ELYTRA; // Elytra para alas

    /** 8 slots de equipo (0-6 + 7 alas) */
    private static final int[] EQUIPMENT_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7};

    public EquipmentMenu(TunamaRPG plugin, Player player, PlayerData playerData) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = playerData;
        // 1 fila con exactamente 7 slots de equipo
        this.inventory = Bukkit.createInventory(this, 9, "§8◆ §6§lEquipo §8◆");
        setupItems();
    }

    private void setupItems() {
        // 7 slots de equipo (0-6) + alas (8)
        inventory.setItem(0, createEquipmentSlot("§6Anillo I", playerData.getRing1(), RING_EMPTY));
        inventory.setItem(1, createEquipmentSlot("§6Anillo II", playerData.getRing2(), RING_EMPTY));
        inventory.setItem(2, createEquipmentSlot("§bCollar", playerData.getNecklace(), NECKLACE_EMPTY));
        inventory.setItem(3, createEquipmentSlot("§6Anillo III", playerData.getRing3(), RING_EMPTY));
        inventory.setItem(4, createEquipmentSlot("§6Anillo IV", playerData.getRing4(), RING_EMPTY));
        inventory.setItem(5, createEquipmentSlot("§dAmuleto I", playerData.getAmulet1(), AMULET_EMPTY));
        inventory.setItem(6, createEquipmentSlot("§dAmuleto II", playerData.getAmulet2(), AMULET_EMPTY));

        // Slot 7: Alas
        inventory.setItem(7, createEquipmentSlot("§fAlas", playerData.getWings(), WINGS_EMPTY));

        // Botón de volver en slot 8
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c✖ Volver");
            List<String> backLore = new ArrayList<>();
            backLore.add("§7Volver al menú principal");
            backMeta.setLore(backLore);
            backButton.setItemMeta(backMeta);
        }
        inventory.setItem(8, backButton);
    }

    private ItemStack createEquipmentSlot(String slotName, String equippedItemId, Material emptyMaterial) {
        boolean empty = equippedItemId == null || equippedItemId.isEmpty();
        ItemStack item;

        if (empty) {
            // Slot vacío: usar material específico según tipo (anillo, collar, amuleto)
            item = new ItemStack(emptyMaterial);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // Nombre con formato bonito según el tipo de slot
                String displayName = getEmptySlotDisplayName(slotName, emptyMaterial);
                meta.setDisplayName(displayName);

                List<String> lore = new ArrayList<>();
                lore.add("§8┃ Slot vacío");
                lore.add("§8┃");
                lore.add("§7┃ Arrastra un objeto aquí");
                lore.add("§7┃ para equiparlo");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        } else {
            // Slot con objeto equipado: cargar el objeto real
            item = createItemFromId(equippedItemId);
            if (item == null) {
                item = new ItemStack(emptyMaterial);
            }
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add("§8▸ Equipado en: §f" + slotName);
                lore.add("");
                lore.add("§e⚡ Clic izquierdo §7▸ Desequipar");
                lore.add("§e⚡ Clic derecho §7▸ Ver detalles");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    /**
     * Genera el nombre visual para un slot vacío con iconos personalizados
     */
    private String getEmptySlotDisplayName(String slotName, Material emptyMaterial) {
        if (emptyMaterial == RING_EMPTY) {
            return "§8◎ " + slotName + " §8◎";
        } else if (emptyMaterial == NECKLACE_EMPTY) {
            return "§8⛓ " + slotName + " §8⛓";
        } else if (emptyMaterial == AMULET_EMPTY) {
            return "§8✦ " + slotName + " §8✦";
        } else if (emptyMaterial == WINGS_EMPTY) {
            return "§8✧ " + slotName + " §8✧";
        }
        return "§8" + slotName;
    }

    private ItemStack createItemFromId(String itemId) {
        if (itemId == null || itemId.isEmpty()) return null;

        // Primero intentar obtener el objeto del ItemManager
        com.irdem.tunama.data.RPGItem rpgItem = plugin.getItemManager().getItem(itemId);
        if (rpgItem != null) {
            return rpgItem.toItemStack(1);
        }

        // Fallback: intentar como material directo
        String materialName = itemId.toUpperCase().replace(" ", "_");
        try {
            Material m = Material.valueOf(materialName);
            ItemStack item = new ItemStack(m);
            ItemMeta meta = item.getItemMeta();
            if (meta != null && !itemId.equals(materialName)) {
                meta.setDisplayName(itemId);
                item.setItemMeta(meta);
            }
            return item;
        } catch (IllegalArgumentException e) {
            String lower = itemId.toLowerCase();
            Material m = Material.PAPER;
            if (lower.contains("anillo") || lower.contains("ring")) m = Material.GOLD_NUGGET;
            else if (lower.contains("collar") || lower.contains("necklace")) m = Material.AMETHYST_SHARD;
            else if (lower.contains("amuleto") || lower.contains("amulet")) m = Material.EMERALD;
            ItemStack item = new ItemStack(m);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(itemId);
                item.setItemMeta(meta);
            }
            return item;
        }
    }

    public void refreshInventory() {
        setupItems();
    }

    public boolean isEquipmentSlot(int slot) {
        for (int s : EQUIPMENT_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    public int getEquipmentSlotIndex(int slot) {
        for (int i = 0; i < EQUIPMENT_SLOTS.length; i++) {
            if (EQUIPMENT_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    public String getEquipmentFieldName(int slotIndex) {
        switch (slotIndex) {
            case 0: return "ring1";
            case 1: return "ring2";
            case 2: return "necklace";
            case 3: return "ring3";
            case 4: return "ring4";
            case 5: return "amulet1";
            case 6: return "amulet2";
            case 7: return "wings";
            default: return null;
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerData getPlayerData() {
        return playerData;
    }

    public TunamaRPG getPlugin() {
        return plugin;
    }
}
