package com.irdem.tunama.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.menus.EquipmentMenu;
import com.irdem.tunama.data.PlayerData;

public class EquipmentMenuListener implements Listener {

    private TunamaRPG plugin;

    public EquipmentMenuListener(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        InventoryHolder holder = event.getInventory().getHolder();

        if (!(holder instanceof EquipmentMenu)) {
            return;
        }

        EquipmentMenu equipmentMenu = (EquipmentMenu) holder;
        int slot = event.getRawSlot();

        // Si el click es fuera del inventario, cancelar
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        // Slot 8: Botón Volver
        if (slot == 8) {
            event.setCancelled(true);
            PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
            if (playerData != null) {
                player.closeInventory();
                com.irdem.tunama.menus.MainMenuGUI mainMenu = new com.irdem.tunama.menus.MainMenuGUI(plugin, player, playerData);
                mainMenu.open();
            }
            return;
        }

        // Inventario del jugador (slot >= 9): manejar shift-click para auto-equipar
        if (slot >= 9) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType().isAir()) {
                    return;
                }

                // Obtener el ID del item
                String itemId = getItemId(clickedItem);
                if (itemId == null || itemId.isEmpty()) {
                    player.sendMessage("§c✗ Este objeto no se puede equipar en ninguna ranura.");
                    return;
                }

                // Verificar que sea un item RPG con tipo
                com.irdem.tunama.data.RPGItem rpgItem = plugin.getItemManager().getItem(itemId);
                if (rpgItem == null || rpgItem.getType() == null) {
                    player.sendMessage("§c✗ Este objeto no se puede equipar en ninguna ranura.");
                    return;
                }

                String itemType = rpgItem.getType().toLowerCase();

                // Determinar qué slots corresponden al tipo del item
                PlayerData playerData = equipmentMenu.getPlayerData();
                if (playerData == null) {
                    playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
                }
                if (playerData == null) {
                    player.sendMessage("§c✗ Error: No se pudieron cargar tus datos.");
                    return;
                }

                // Verificar requisitos
                if (!rpgItem.meetsRequirements(playerData)) {
                    player.sendMessage("§c✗ No cumples los requisitos para equipar este objeto:");
                    for (String unmet : rpgItem.getUnmetRequirements(playerData)) {
                        player.sendMessage(unmet);
                    }
                    return;
                }

                // Buscar primer slot libre del tipo correcto
                String targetField = findFirstEmptySlotForType(itemType, playerData);
                if (targetField == null) {
                    player.sendMessage("§e⚠ No hay ranuras libres de tipo §f" + getSlotTypeSpanish(itemType) + "§e. Desequipa una primero.");
                    return;
                }

                // Equipar el item
                setEquipmentField(playerData, targetField, itemId);

                // Quitar el item del inventario del jugador
                if (clickedItem.getAmount() > 1) {
                    clickedItem.setAmount(clickedItem.getAmount() - 1);
                } else {
                    event.getClickedInventory().setItem(event.getSlot(), null);
                }

                // Guardar y refrescar
                plugin.getDatabaseManager().updatePlayerData(playerData);
                equipmentMenu.refreshInventory();

                String displayName = rpgItem.getName() != null ? rpgItem.getName() : itemId;
                player.sendMessage("§a✓ Item equipado: §f" + displayName + " §7→ §f" + getSlotTypeSpanish(itemType));
            }
            return;
        }

        // Si es una ranura de equipo (0-7)
        if (equipmentMenu.isEquipmentSlot(slot)) {
            event.setCancelled(true);
            
            ItemStack clickedItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();

            int equipmentIndex = equipmentMenu.getEquipmentSlotIndex(slot);
            String fieldName = equipmentMenu.getEquipmentFieldName(equipmentIndex);
            
            if (fieldName == null) {
                return;
            }

            PlayerData playerData = equipmentMenu.getPlayerData();
            if (playerData == null) {
                playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
                if (playerData == null) {
                    player.sendMessage("§c✗ Error: No se pudieron cargar tus datos.");
                    return;
                }
            }

            // Verificar si hay un item en el cursor (arrastrando para equipar)
            if (cursorItem != null && !cursorItem.getType().isAir()) {
                // Obtener el ID del item que se quiere equipar
                String itemId = getItemId(cursorItem);
                if (itemId == null || itemId.isEmpty()) {
                    player.sendMessage("§c✗ Este item no puede ser equipado.");
                    return;
                }

                // Verificar que el item sea un objeto RPG válido (no vanilla)
                com.irdem.tunama.data.RPGItem rpgItem = plugin.getItemManager().getItem(itemId);
                if (rpgItem == null) {
                    player.sendMessage("§c✗ Solo se pueden equipar objetos RPG, no objetos vanilla.");
                    return;
                }

                // Verificar que el tipo del item coincida con el slot
                if (rpgItem != null) {
                    String expectedType = getExpectedSlotType(fieldName);
                    String itemType = rpgItem.getType();
                    if (expectedType != null && itemType != null && !itemType.equalsIgnoreCase(expectedType)) {
                        player.sendMessage("§c✗ Este objeto es de tipo §f" + getSlotTypeSpanish(itemType) + "§c y no se puede equipar en un slot de §f" + getSlotTypeSpanish(expectedType));
                        return;
                    }

                    // Verificar requisitos de clase, nivel y stats
                    if (!rpgItem.meetsRequirements(playerData)) {
                        player.sendMessage("§c✗ No cumples los requisitos para equipar este objeto:");
                        for (String unmet : rpgItem.getUnmetRequirements(playerData)) {
                            player.sendMessage(unmet);
                        }
                        return;
                    }
                }

                // Si hay un item equipado, desequiparlo primero
                String currentEquipped = getEquippedItemId(playerData, fieldName);
                if (currentEquipped != null && !currentEquipped.isEmpty()) {
                    ItemStack itemToReturn = createItemFromId(currentEquipped);
                    if (itemToReturn != null) {
                        // Intentar agregar al inventario
                        if (player.getInventory().firstEmpty() != -1) {
                            player.getInventory().addItem(itemToReturn);
                        } else {
                            // Si no hay espacio, soltar en el mundo
                            player.getWorld().dropItemNaturally(player.getLocation(), itemToReturn);
                            player.sendMessage("§e⚠ Tu inventario está lleno. El item anterior fue soltado en el suelo.");
                        }
                    }
                }

                // Equipar el nuevo item
                setEquipmentField(playerData, fieldName, itemId);
                // Crear una copia del item para el cursor (reducir cantidad)
                ItemStack newCursor = cursorItem.clone();
                newCursor.setAmount(cursorItem.getAmount() - 1);
                if (newCursor.getAmount() <= 0) {
                    player.setItemOnCursor(null);
                } else {
                    player.setItemOnCursor(newCursor);
                }
                // Mostrar nombre bonito del item
                String displayName = rpgItem != null ? rpgItem.getName() : itemId;
                player.sendMessage("§a✓ Item equipado: §f" + displayName);
            }
            // Si hay un item equipado y no hay nada en el cursor (click para desequipar)
            else if (clickedItem != null && !clickedItem.getType().isAir()) {
                String equippedId = getEquippedItemId(playerData, fieldName);
                if (equippedId != null && !equippedId.isEmpty()) {
                    // Desequipar: agregar el item al inventario del jugador
                    ItemStack itemToReturn = createItemFromId(equippedId);
                    if (itemToReturn != null) {
                        // Intentar agregar al inventario
                        if (player.getInventory().firstEmpty() != -1) {
                            player.getInventory().addItem(itemToReturn);
                        } else {
                            // Si no hay espacio, soltar en el mundo
                            player.getWorld().dropItemNaturally(player.getLocation(), itemToReturn);
                            player.sendMessage("§e⚠ Tu inventario está lleno. El item fue soltado en el suelo.");
                        }
                    }
                    setEquipmentField(playerData, fieldName, null);
                    // Mostrar nombre bonito del item desequipado
                    com.irdem.tunama.data.RPGItem unequipRpgItem = plugin.getItemManager().getItem(equippedId);
                    String unequipName = unequipRpgItem != null ? unequipRpgItem.getName() : equippedId;
                    player.sendMessage("§a✓ Item desequipado: §f" + unequipName);
                }
            }

            // Guardar cambios en la base de datos
            plugin.getDatabaseManager().updatePlayerData(playerData);
            
            // Refrescar el menú
            equipmentMenu.refreshInventory();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        InventoryHolder holder = event.getInventory().getHolder();

        if (!(holder instanceof EquipmentMenu)) {
            return;
        }

        EquipmentMenu equipmentMenu = (EquipmentMenu) holder;

        // Cancelar arrastrar sobre slots de equipo (0-6) o vacíos (7-8)
        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot <= 8) {
                event.setCancelled(true);
                if (equipmentMenu.isEquipmentSlot(slot)) {
                    player.sendMessage("§eℹ Usa click izquierdo para equipar en las ranuras de equipo.");
                }
                break;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // El GUI de equipo solo tiene 9 slots (7 equipo + 2 vacíos). El "Inventario"
        // es el inventario del jugador (vanilla); no se copia en nuestro holder,
        // así que no hay nada que sincronizar al cerrar.
    }

    private String getItemId(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        // Primero intentar obtener el ID del lore (objetos RPG tienen "§8ID: xxx")
        if (meta != null && meta.hasLore()) {
            for (String loreLine : meta.getLore()) {
                if (loreLine != null && loreLine.startsWith("§8ID: ")) {
                    return loreLine.substring(6); // Retornar el ID limpio
                }
            }
        }

        // Si no tiene ID en el lore, usar el nombre del display o material
        String itemId;
        if (meta != null && meta.hasDisplayName()) {
            itemId = meta.getDisplayName();
        } else {
            itemId = formatMaterialName(item.getType().name());
        }

        return itemId;
    }

    /**
     * Retorna el tipo de item esperado para un campo de equipo
     */
    private String getExpectedSlotType(String fieldName) {
        switch (fieldName) {
            case "ring1":
            case "ring2":
            case "ring3":
            case "ring4":
                return "ring";
            case "necklace":
                return "necklace";
            case "amulet1":
            case "amulet2":
                return "amulet";
            case "wings":
                return "wings";
            default:
                return null;
        }
    }

    /**
     * Traduce el tipo de slot al español
     */
    private String getSlotTypeSpanish(String type) {
        if (type == null) return "desconocido";
        switch (type.toLowerCase()) {
            case "ring": return "Anillo";
            case "necklace": return "Collar";
            case "amulet": return "Amuleto";
            case "wings": return "Alas";
            default: return type;
        }
    }

    /**
     * Busca el primer slot vacío del tipo correcto para auto-equipar con shift-click
     */
    private String findFirstEmptySlotForType(String itemType, PlayerData playerData) {
        switch (itemType) {
            case "ring":
                if (isEmpty(playerData.getRing1())) return "ring1";
                if (isEmpty(playerData.getRing2())) return "ring2";
                if (isEmpty(playerData.getRing3())) return "ring3";
                if (isEmpty(playerData.getRing4())) return "ring4";
                return null;
            case "necklace":
                if (isEmpty(playerData.getNecklace())) return "necklace";
                return null;
            case "amulet":
                if (isEmpty(playerData.getAmulet1())) return "amulet1";
                if (isEmpty(playerData.getAmulet2())) return "amulet2";
                return null;
            case "wings":
                if (isEmpty(playerData.getWings())) return "wings";
                return null;
            default:
                return null;
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private String formatMaterialName(String materialName) {
        // Convertir MATERIAL_NAME a Material Name
        return materialName.replace("_", " ").toLowerCase();
    }

    private String getEquippedItemId(PlayerData playerData, String fieldName) {
        switch (fieldName) {
            case "ring1": return playerData.getRing1();
            case "ring2": return playerData.getRing2();
            case "ring3": return playerData.getRing3();
            case "ring4": return playerData.getRing4();
            case "necklace": return playerData.getNecklace();
            case "amulet1": return playerData.getAmulet1();
            case "amulet2": return playerData.getAmulet2();
            case "wings": return playerData.getWings();
            default: return null;
        }
    }

    private void setEquipmentField(PlayerData playerData, String fieldName, String value) {
        switch (fieldName) {
            case "ring1": playerData.setRing1(value); break;
            case "ring2": playerData.setRing2(value); break;
            case "ring3": playerData.setRing3(value); break;
            case "ring4": playerData.setRing4(value); break;
            case "necklace": playerData.setNecklace(value); break;
            case "amulet1": playerData.setAmulet1(value); break;
            case "amulet2": playerData.setAmulet2(value); break;
            case "wings": playerData.setWings(value); break;
        }
    }

    private ItemStack createItemFromId(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }

        // Primero intentar obtener el objeto del ItemManager
        com.irdem.tunama.data.RPGItem rpgItem = plugin.getItemManager().getItem(itemId);
        if (rpgItem != null) {
            return rpgItem.toItemStack(1);
        }

        // Fallback: Intentar parsear como Material primero (si está en formato MATERIAL_NAME)
        String materialName = itemId.toUpperCase().replace(" ", "_");
        try {
            org.bukkit.Material material = org.bukkit.Material.valueOf(materialName);
            ItemStack item = new ItemStack(material);
            // Si el itemId original tenía formato diferente, es un nombre personalizado
            if (!itemId.equals(materialName) && !itemId.equals(material.name())) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(itemId);
                    item.setItemMeta(meta);
                }
            }
            return item;
        } catch (IllegalArgumentException e) {
            // Si no es un material válido, crear un item genérico con el nombre
            // Intentar detectar el tipo de item basado en el nombre
            org.bukkit.Material material = org.bukkit.Material.PAPER;

            // Detectar tipo de item basado en palabras clave
            String lowerId = itemId.toLowerCase();
            if (lowerId.contains("anillo") || lowerId.contains("ring")) {
                material = org.bukkit.Material.GOLD_NUGGET;
            } else if (lowerId.contains("collar") || lowerId.contains("necklace")) {
                material = org.bukkit.Material.AMETHYST_SHARD;
            } else if (lowerId.contains("amuleto") || lowerId.contains("amulet")) {
                material = org.bukkit.Material.EMERALD;
            }

            ItemStack item = new ItemStack(material);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(itemId);
                item.setItemMeta(meta);
            }
            return item;
        }
    }
}
