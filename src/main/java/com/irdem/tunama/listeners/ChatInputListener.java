package com.irdem.tunama.listeners;

import com.irdem.tunama.TunamaRPG;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener para capturar nombres de personajes y datos de clanes ingresados por chat
 */
public class ChatInputListener implements Listener {

    private final TunamaRPG plugin;

    // Mapa para rastrear qué jugadores están esperando ingresar un nombre
    private final Map<UUID, PendingCharacter> pendingCharacters;

    // Mapas para rastrear la creación de clanes
    private final Map<UUID, ClanCreationStep> clanCreationSteps;
    private final Map<UUID, String> pendingClanNames;

    public enum ClanCreationStep {
        AWAITING_NAME,
        AWAITING_TAG
    }

    public ChatInputListener(TunamaRPG plugin) {
        this.plugin = plugin;
        this.pendingCharacters = new HashMap<>();
        this.clanCreationSteps = new HashMap<>();
        this.pendingClanNames = new HashMap<>();
    }

    /**
     * Registra que un jugador está esperando ingresar un nombre de personaje
     */
    public void registerPendingCharacter(Player player, String raceId, String classId) {
        pendingCharacters.put(player.getUniqueId(), new PendingCharacter(raceId, classId));
        player.sendMessage("");
        player.sendMessage("§e§l⚔ CREACIÓN DE PERSONAJE ⚔");
        player.sendMessage("");
        player.sendMessage("§7Escribe el nombre de tu personaje en el chat:");
        player.sendMessage("");
        player.sendMessage("§7Requisitos:");
        player.sendMessage("§7• Mínimo 3 caracteres");
        player.sendMessage("§7• Máximo 16 caracteres");
        player.sendMessage("§7• Solo letras, números y guiones bajos");
        player.sendMessage("");
    }

    /**
     * Inicia el proceso de creación de un clan pidiendo el nombre
     */
    public void startClanCreation(Player player) {
        clanCreationSteps.put(player.getUniqueId(), ClanCreationStep.AWAITING_NAME);
        player.sendMessage("");
        player.sendMessage("§e§l⚔ CREACIÓN DE CLAN ⚔");
        player.sendMessage("");
        player.sendMessage("§7Escribe el nombre de tu clan en el chat:");
        player.sendMessage("§7• Máximo 16 caracteres");
        player.sendMessage("§7• No puede estar en uso");
        player.sendMessage("");
    }

    /**
     * Cancela el proceso de creación de clan para un jugador
     */
    public void cancelClanCreation(UUID playerId) {
        clanCreationSteps.remove(playerId);
        pendingClanNames.remove(playerId);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Verificar si el jugador está creando un clan
        if (clanCreationSteps.containsKey(playerId)) {
            event.setCancelled(true);
            String input;
            // Para el paso del tag, usar LegacyComponentSerializer para preservar códigos de color &
            if (clanCreationSteps.get(playerId) == ClanCreationStep.AWAITING_TAG) {
                input = LegacyComponentSerializer.legacySection().serialize(event.message());
                // Normalizar § a & para almacenamiento consistente
                input = input.replace("§", "&").trim();
            } else {
                input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
            }
            handleClanCreationInput(player, input);
            return;
        }

        // Verificar si el jugador tiene una acción de oro pendiente
        if (com.irdem.tunama.menus.clan.ClanBankMenu.hasPendingGoldAction(playerId)) {
            event.setCancelled(true);
            String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
            handleGoldInput(player, input);
            return;
        }

        // Verificar si el jugador tiene un personaje pendiente
        if (pendingCharacters.containsKey(playerId)) {
            event.setCancelled(true);
            String characterName = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
            handleCharacterCreationInput(player, characterName);
            return;
        }
    }

    /**
     * Maneja el input del chat durante la creación de personaje
     */
    private void handleCharacterCreationInput(Player player, String characterName) {
        PendingCharacter pending = pendingCharacters.get(player.getUniqueId());

        // Validar el nombre
        if (!plugin.getBannedNamesManager().isNameValid(characterName)) {
            String errorMsg = plugin.getBannedNamesManager().getErrorMessage(characterName);
            player.sendMessage(errorMsg);
            player.sendMessage("§ePor favor, intenta con otro nombre:");
            return;
        }

        // Nombre válido, crear el personaje (en el thread principal)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Guardar el personaje con el nombre elegido
            plugin.getDatabaseManager().saveNewPlayer(
                player.getUniqueId(),
                characterName,
                pending.raceId,
                pending.classId
            );

            // Cargar los datos del nuevo personaje
            com.irdem.tunama.data.PlayerData newPlayerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());

            if (newPlayerData != null) {
                // Cambiar el display name del jugador
                player.displayName(net.kyori.adventure.text.Component.text(characterName));
                player.playerListName(net.kyori.adventure.text.Component.text(characterName));

                // Actualizar la vida máxima del jugador según la raza
                updatePlayerMaxHealth(player, newPlayerData);

                // Actualizar las barras de experiencia, vida y mana
                plugin.getPlayerBarsManager().updateBars(player);

                player.sendMessage("§a✓ Personaje creado exitosamente: §f" + characterName);
                player.sendMessage("§e¡Bienvenido a TunamaRPG, " + characterName + "!");
            } else {
                player.sendMessage("§c✗ Error al cargar el personaje recién creado");
            }

            // Limpiar el registro
            pendingCharacters.remove(player.getUniqueId());
        });
    }

    /**
     * Maneja el input del chat durante la creación de clan
     */
    private void handleClanCreationInput(Player player, String input) {
        UUID playerId = player.getUniqueId();
        ClanCreationStep step = clanCreationSteps.get(playerId);

        if (step == null) return;

        switch (step) {
            case AWAITING_NAME:
                handleClanNameInput(player, input);
                break;
            case AWAITING_TAG:
                handleClanTagInput(player, input);
                break;
        }
    }

    /**
     * Procesa el nombre del clan
     */
    private void handleClanNameInput(Player player, String name) {
        UUID playerId = player.getUniqueId();

        // Validar longitud
        if (name.length() > 16) {
            player.sendMessage("§c✗ El nombre es demasiado largo (máximo 16 caracteres)");
            return;
        }

        if (name.length() < 1) {
            player.sendMessage("§c✗ El nombre no puede estar vacío");
            return;
        }

        // Verificar si el nombre ya está en uso
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (com.irdem.tunama.data.Clan clan : plugin.getClanManager().getTopClansByGold(9999)) {
                if (clan.getName().equalsIgnoreCase(name)) {
                    player.sendMessage("§c✗ Ya existe un clan con ese nombre");
                    return;
                }
            }

            // Nombre válido
            pendingClanNames.put(playerId, name);
            clanCreationSteps.put(playerId, ClanCreationStep.AWAITING_TAG);

            player.sendMessage("");
            player.sendMessage("§a✓ Nombre aceptado: §f" + name);
            player.sendMessage("");
            player.sendMessage("§7Ahora escribe el TAG de tu clan:");
            player.sendMessage("§7• Mínimo 1 carácter, máximo 4 §8(sin contar códigos de color)");
            player.sendMessage("§7• Solo letras y números");
            player.sendMessage("§7• No puede estar en uso");
            player.sendMessage("");
            player.sendMessage("§e§l¡COLORES DISPONIBLES!");
            player.sendMessage("§7Usa §f& §7seguido de: §00§11§22§33§44§55§66§77§88§99§aa§bb§cc§dd§ee§ff");
            player.sendMessage("§7Ejemplo: §f&aRPG §7= §aRPG");
            player.sendMessage("");
        });
    }

    /**
     * Procesa el tag del clan y crea el clan
     */
    private void handleClanTagInput(Player player, String tag) {
        UUID playerId = player.getUniqueId();

        // Eliminar códigos de color para contar caracteres visibles
        String visibleTag = tag.replaceAll("&[0-9a-fA-Fk-oK-OrR]", "");

        // Validar longitud de caracteres visibles
        if (visibleTag.length() < 1 || visibleTag.length() > 4) {
            player.sendMessage("§c✗ El tag debe tener entre 1 y 4 caracteres visibles");
            return;
        }

        // Validar formato (letras, números, y códigos de color &X)
        if (!tag.matches("[a-zA-Z0-9&]+")) {
            player.sendMessage("§c✗ El tag solo puede contener letras, números y códigos de color (&)");
            return;
        }

        // Validar que & solo preceda a códigos de color válidos
        if (tag.contains("&") && !tag.matches("([a-zA-Z0-9]|&[0-9a-fA-Fk-oK-OrR])+")) {
            player.sendMessage("§c✗ Código de color inválido. Usa &0-9 o &a-f");
            return;
        }

        String clanName = pendingClanNames.get(playerId);
        if (clanName == null) {
            player.sendMessage("§c✗ Error: nombre de clan no encontrado. Reinicia el proceso con /clan crear");
            cancelClanCreation(playerId);
            return;
        }

        // Crear el clan (en el thread principal)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Verificar si ya tiene clan
            if (plugin.getClanManager().hasPlayerClan(playerId)) {
                player.sendMessage("§c✗ Ya tienes un clan");
                cancelClanCreation(playerId);
                return;
            }

            // Verificar si el tag ya existe
            if (plugin.getClanManager().getClanByTag(tag) != null) {
                player.sendMessage("§c✗ Ya existe un clan con ese tag");
                return;
            }

            // Crear el clan
            try {
                com.irdem.tunama.data.Clan newClan = plugin.getClanManager().createClan(tag, clanName, playerId);

                player.sendMessage("");
                player.sendMessage("§a§l✓ CLAN CREADO EXITOSAMENTE");
                player.sendMessage("");
                player.sendMessage("§7Nombre: §f" + clanName);
                player.sendMessage("§7Tag: " + newClan.getFormattedTag());
                player.sendMessage("§7Líder: §e" + player.getName());
                player.sendMessage("");
                player.sendMessage("§7Usa §f/clan §7para administrar tu clan");
                player.sendMessage("");

                // Registrar en los logs
                plugin.getClanManager().addLog(new com.irdem.tunama.data.ClanLog(
                    newClan.getId(),
                    playerId,
                    player.getName(),
                    "CLAN_CREADO",
                    0,
                    null
                ));

            } catch (Exception e) {
                player.sendMessage("§c✗ Error al crear el clan: " + e.getMessage());
                plugin.getLogger().severe("Error al crear clan: " + e.getMessage());
                e.printStackTrace();
            }

            // Limpiar el proceso
            cancelClanCreation(playerId);
        });
    }

    /**
     * Maneja el input del chat para depósito/retiro de oro
     */
    private void handleGoldInput(Player player, String input) {
        UUID playerId = player.getUniqueId();

        // Verificar si quiere cancelar
        if (input.equalsIgnoreCase("cancelar")) {
            com.irdem.tunama.menus.clan.ClanBankMenu.removePendingGoldAction(playerId);
            player.sendMessage("§e✓ Acción cancelada");
            return;
        }

        // Obtener la acción pendiente
        com.irdem.tunama.menus.clan.ClanBankMenu.PendingGoldAction action =
            com.irdem.tunama.menus.clan.ClanBankMenu.getPendingGoldAction(playerId);

        if (action == null) {
            return;
        }

        // Parsear la cantidad
        long amount;
        try {
            amount = Long.parseLong(input);
        } catch (NumberFormatException e) {
            player.sendMessage("§c✗ Cantidad inválida. Escribe un número válido o 'cancelar'");
            return;
        }

        // Ejecutar en el thread principal
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (action.type == com.irdem.tunama.menus.clan.ClanBankMenu.PendingGoldAction.ActionType.DEPOSIT) {
                processDeposit(player, action.clan, amount);
            } else {
                processWithdraw(player, action.clan, amount);
            }

            // Limpiar la acción pendiente
            com.irdem.tunama.menus.clan.ClanBankMenu.removePendingGoldAction(playerId);
        });
    }

    /**
     * Procesa un depósito de oro
     */
    private void processDeposit(Player player, com.irdem.tunama.data.Clan clan, long amount) {
        if (amount <= 0) {
            player.sendMessage("§c✗ La cantidad debe ser mayor a 0");
            return;
        }

        // Verificar si el sistema de economía está habilitado
        if (!plugin.getEconomyManager().isEnabled()) {
            player.sendMessage("§c✗ El sistema de economía no está disponible");
            return;
        }

        // Verificar que el jugador tiene el dinero
        if (!plugin.getEconomyManager().has(player, amount)) {
            player.sendMessage("§c✗ No tienes suficiente dinero");
            player.sendMessage("§7Tu balance: §6" + plugin.getEconomyManager().format(plugin.getEconomyManager().getBalance(player)));
            return;
        }

        // Retirar dinero del jugador
        if (!plugin.getEconomyManager().withdraw(player, amount)) {
            player.sendMessage("§c✗ Error al procesar la transacción");
            return;
        }

        try {
            // Añadir al clan
            clan.addGold(amount);
            plugin.getClanManager().updateClanGold(clan.getId(), clan.getGold());

            // Crear log
            plugin.getClanManager().addLog(new com.irdem.tunama.data.ClanLog(
                clan.getId(),
                player.getUniqueId(),
                player.getName(),
                "DEPOSITO_ORO",
                amount,
                null
            ));

            player.sendMessage("");
            player.sendMessage("§a§l✓ DEPÓSITO EXITOSO");
            player.sendMessage("");
            player.sendMessage("§7Cantidad depositada: §6" + plugin.getEconomyManager().format(amount));
            player.sendMessage("§7Nuevo balance del clan: §6" + plugin.getEconomyManager().format(clan.getGold()));
            player.sendMessage("§7Tu balance: §6" + plugin.getEconomyManager().format(plugin.getEconomyManager().getBalance(player)));
            player.sendMessage("");
        } catch (java.sql.SQLException e) {
            // Revertir la transacción económica si falla la base de datos
            plugin.getEconomyManager().deposit(player, amount);
            player.sendMessage("§c✗ Error al guardar el depósito en la base de datos");
            plugin.getLogger().severe("Error al procesar depósito de oro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Procesa un retiro de oro
     */
    private void processWithdraw(Player player, com.irdem.tunama.data.Clan clan, long amount) {
        if (amount <= 0) {
            player.sendMessage("§c✗ La cantidad debe ser mayor a 0");
            return;
        }

        // Verificar si el sistema de economía está habilitado
        if (!plugin.getEconomyManager().isEnabled()) {
            player.sendMessage("§c✗ El sistema de economía no está disponible");
            return;
        }

        // Verificar que el clan tiene suficiente oro
        if (clan.getGold() < amount) {
            player.sendMessage("§c✗ El clan no tiene suficiente oro");
            player.sendMessage("§7Oro disponible: §6" + plugin.getEconomyManager().format(clan.getGold()));
            return;
        }

        try {
            // Retirar del clan
            clan.removeGold(amount);
            plugin.getClanManager().updateClanGold(clan.getId(), clan.getGold());

            // Dar dinero al jugador
            if (!plugin.getEconomyManager().deposit(player, amount)) {
                // Revertir si falla
                clan.addGold(amount);
                plugin.getClanManager().updateClanGold(clan.getId(), clan.getGold());
                player.sendMessage("§c✗ Error al procesar la transacción");
                return;
            }

            // Crear log
            plugin.getClanManager().addLog(new com.irdem.tunama.data.ClanLog(
                clan.getId(),
                player.getUniqueId(),
                player.getName(),
                "RETIRO_ORO",
                amount,
                null
            ));

            player.sendMessage("");
            player.sendMessage("§a§l✓ RETIRO EXITOSO");
            player.sendMessage("");
            player.sendMessage("§7Cantidad retirada: §6" + plugin.getEconomyManager().format(amount));
            player.sendMessage("§7Nuevo balance del clan: §6" + plugin.getEconomyManager().format(clan.getGold()));
            player.sendMessage("§7Tu balance: §6" + plugin.getEconomyManager().format(plugin.getEconomyManager().getBalance(player)));
            player.sendMessage("");
        } catch (java.sql.SQLException e) {
            player.sendMessage("§c✗ Error al guardar el retiro en la base de datos");
            plugin.getLogger().severe("Error al procesar retiro de oro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Actualiza el MAX_HEALTH del jugador basado en sus estadísticas
     */
    private void updatePlayerMaxHealth(Player player, com.irdem.tunama.data.PlayerData playerData) {
        // Obtener el multiplicador de vida de la raza
        com.irdem.tunama.data.Race race = playerData.getRace() != null ?
            plugin.getRaceManager().getRace(playerData.getRace().toLowerCase()) : null;
        double lifeMult = race != null ? race.getLifeMultiplier() : 1.0;

        // Calcular la vida total (base health * multiplicador de raza)
        int totalHealth = (int)(playerData.getStats().getHealth() * lifeMult);
        totalHealth = Math.max(20, totalHealth); // Mínimo 20 de vida (10 corazones)

        // Actualizar el atributo MAX_HEALTH del jugador
        org.bukkit.attribute.AttributeInstance maxHealthAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(totalHealth);
            player.setHealth(totalHealth); // Curar al jugador completamente
        }
    }

    /**
     * Clase interna para almacenar información de personajes pendientes
     */
    private static class PendingCharacter {
        final String raceId;
        final String classId;

        PendingCharacter(String raceId, String classId) {
            this.raceId = raceId;
            this.classId = classId;
        }
    }
}
