package com.irdem.tunama.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.Race;
import com.irdem.tunama.data.RPGClass;
import com.irdem.tunama.data.Subclass;
import com.irdem.tunama.data.PlayerData;
import com.irdem.tunama.menus.MainMenuGUI;
import java.util.ArrayList;
import java.util.List;

public class RPGCommand implements CommandExecutor, TabCompleter {

    private TunamaRPG plugin;

    public RPGCommand(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Comandos que pueden usar consola y admins
        if (args.length > 0) {
            String subcommand = args[0].toLowerCase();

            // Comandos disponibles para consola
            switch (subcommand) {
                case "reload":
                    if (sender.hasPermission("rpg.reload") || sender.isOp() || !(sender instanceof Player)) {
                        reloadConfigs(sender);
                    } else {
                        sender.sendMessage("§cNo tienes permisos para usar este comando");
                    }
                    return true;

                case "experiencia":
                case "exp":
                    if (args.length >= 3) {
                        if (sender.hasPermission("rpg.admin") || sender.isOp() || !(sender instanceof Player)) {
                            giveExperience(sender, args[1], args[2]);
                        } else {
                            sender.sendMessage("§c✗ No tienes permisos para usar este comando");
                        }
                    } else {
                        sender.sendMessage("§cUso: /rpg experiencia <jugador> <cantidad>");
                    }
                    return true;

                case "objeto":
                case "item":
                    if (sender.hasPermission("rpg.admin") || sender.isOp() || !(sender instanceof Player)) {
                        handleGiveItem(sender, args);
                    } else {
                        sender.sendMessage("§c✗ No tienes permisos para usar este comando");
                    }
                    return true;

                case "estadisticas":
                case "stats":
                    if (args.length >= 3) {
                        if (sender.hasPermission("rpg.admin") || sender.isOp() || !(sender instanceof Player)) {
                            giveStatPoints(sender, args[1], args[2]);
                        } else {
                            sender.sendMessage("§c✗ No tienes permisos para usar este comando");
                        }
                    } else {
                        sender.sendMessage("§cUso: /rpg estadisticas <jugador> <cantidad>");
                    }
                    return true;
            }
        }

        // Resto de comandos requieren ser jugador
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores!");
            sender.sendMessage("§7Comandos de consola: /rpg reload, /rpg exp, /rpg objeto, /rpg estadisticas");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Obtener o crear datos del jugador
            PlayerData playerData = plugin.getDatabaseManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());

            if (playerData == null) {
                player.sendMessage("§c✗ Error: No se pudo obtener tus datos. Contacta al administrador.");
                return true;
            }

            // Si no tiene raza seleccionada, abrir menú de razas
            if (playerData.getRace() == null || playerData.getRace().isEmpty()) {
                com.irdem.tunama.menus.RaceMenu raceMenu = new com.irdem.tunama.menus.RaceMenu(plugin);
                raceMenu.open(player);
                return true;
            }

            // Si no tiene clase seleccionada, abrir menú de clases
            if (playerData.getPlayerClass() == null || playerData.getPlayerClass().isEmpty()) {
                com.irdem.tunama.menus.ClassMenu classMenu = new com.irdem.tunama.menus.ClassMenu(plugin, playerData.getRace());
                classMenu.open(player);
                return true;
            }

            MainMenuGUI mainMenu = new MainMenuGUI(plugin, player, playerData);
            mainMenu.open();
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "razas":
                showRaces(player);
                break;
            case "clases":
                showClasses(player);
                break;
            case "info":
                if (args.length > 1) {
                    showInfo(player, args[1]);
                } else {
                    player.sendMessage("§c/rpg info <raza|clase|subclase>");
                }
                break;
            case "help":
                sendHelp(player);
                break;
            case "renacer":
                handleRenacer(player);
                break;
            case "personajes":
                handlePersonajes(player);
                break;
            case "confirmar-renacer":
                confirmRenacer(player);
                break;
            default:
                player.sendMessage("§cComando desconocido. Usa /rpg help para ver los comandos disponibles");
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6========== TunamaRPG ==========");
        player.sendMessage("§f/rpg razas §7- Ver todas las razas disponibles");
        player.sendMessage("§f/rpg clases §7- Ver todas las clases disponibles");
        player.sendMessage("§f/rpg info <raza|clase|subclase> §7- Ver información detallada");
        player.sendMessage("§f/rpg renacer §7- Eliminar tu personaje actual");
        player.sendMessage("§f/rpg personajes §7- Gestionar tus personajes");
        if (player.hasPermission("rpg.admin") || player.isOp()) {
            player.sendMessage("§f/rpg experiencia <jugador> <cantidad> §7- Dar experiencia");
            player.sendMessage("§f/rpg estadisticas <jugador> <cantidad> §7- Dar puntos de estadística");
            player.sendMessage("§f/rpg objeto <jugador> <id> [cantidad] §7- Dar objeto");
        }
        if (player.hasPermission("rpg.reload") || player.isOp()) {
            player.sendMessage("§f/rpg reload §7- Recargar configuraciones");
        }
        player.sendMessage("§f/rpg help §7- Ver esta ayuda");
        player.sendMessage("§6================================");
    }

    private void handleRenacer(Player player) {
        PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());

        if (playerData == null || playerData.getRace() == null || playerData.getRace().isEmpty()) {
            player.sendMessage("§c✗ No tienes ningún personaje para renacer");
            return;
        }

        // Confirmar eliminación
        player.sendMessage("§6§l⚠ ADVERTENCIA ⚠");
        player.sendMessage("§e¿Estás seguro que quieres eliminar tu personaje actual?");
        player.sendMessage("§7Personaje: §f" + player.getName());
        player.sendMessage("§7Raza: §f" + playerData.getRace());
        player.sendMessage("§7Clase: §f" + playerData.getPlayerClass());
        player.sendMessage("§7Nivel: §f" + playerData.getLevel());
        player.sendMessage("");
        player.sendMessage("§cEsta acción NO se puede deshacer.");

        // Enviar comando clickable usando Adventure API (Paper)
        net.kyori.adventure.text.Component message = net.kyori.adventure.text.Component.text("§a[✔ Click aquí para confirmar] ")
            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/rpg confirmar-renacer"))
            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                net.kyori.adventure.text.Component.text("§7Click para ejecutar:\n§f/rpg confirmar-renacer")
            ))
            .append(net.kyori.adventure.text.Component.text("§7o escribe §f/rpg confirmar-renacer"));

        player.sendMessage(message);
    }

    private void confirmRenacer(Player player) {
        com.irdem.tunama.data.CharacterManager charManager = plugin.getDatabaseManager().getCharacterManager();
        int activeSlot = charManager.getActiveSlot(player.getUniqueId());

        // Verificar que el personaje existe
        if (!charManager.characterExists(player.getUniqueId(), activeSlot)) {
            player.sendMessage("§c✗ No tienes ningún personaje en el slot actual");
            return;
        }

        // Eliminar el personaje
        boolean deleted = charManager.deleteCharacter(player.getUniqueId(), activeSlot);

        if (deleted) {
            player.sendMessage("§a✓ Tu personaje ha sido eliminado exitosamente");
            player.sendMessage("§7Abre el menú con §f/rpg §7para crear uno nuevo o cambiar de personaje");

            // Cerrar inventario si está abierto
            player.closeInventory();

            // Abrir menú de personajes para que pueda crear uno nuevo o cambiar
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                com.irdem.tunama.menus.CharacterSelectionMenu menu = new com.irdem.tunama.menus.CharacterSelectionMenu(plugin);
                menu.open(player);
            }, 20L); // 1 segundo de delay
        } else {
            player.sendMessage("§c✗ Error al eliminar el personaje. Contacta a un administrador.");
        }
    }

    private void handlePersonajes(Player player) {
        // Abrir menú de selección de personajes
        com.irdem.tunama.menus.CharacterSelectionMenu menu = new com.irdem.tunama.menus.CharacterSelectionMenu(plugin);
        menu.open(player);
    }

    private void giveExperience(CommandSender sender, String playerName, String amountStr) {
        try {
            long amount = Long.parseLong(amountStr);
            
            org.bukkit.OfflinePlayer targetPlayer = plugin.getServer().getOfflinePlayer(playerName);
            if (targetPlayer == null || !targetPlayer.hasPlayedBefore()) {
                sender.sendMessage("§c✗ Jugador no encontrado: " + playerName);
                return;
            }
            
            PlayerData playerData = plugin.getDatabaseManager().getPlayerData(targetPlayer.getUniqueId());
            if (playerData == null) {
                sender.sendMessage("§c✗ No se pudieron cargar los datos del jugador");
                return;
            }
            
            // Guardar nivel y experiencia anteriores
            int oldLevel = playerData.getLevel();
            long oldExperience = playerData.getExperience();
            
            // Agregar experiencia
            playerData.addExperience(amount);
            long newExperience = playerData.getExperience();
            
            // Calcular nuevo nivel basado en la experiencia total
            int newLevel = plugin.getExperienceManager().calculateLevel(newExperience);
            
            // Actualizar nivel si es mayor
            if (newLevel > oldLevel) {
                int levelsGained = newLevel - oldLevel;
                int statPointsGained = levelsGained * 5; // 5 puntos por cada nivel subido
                playerData.setLevel(newLevel);
                playerData.addStatPoints(statPointsGained);
                sender.sendMessage("§a✓ Se agregaron §f" + amount + " §ade experiencia a §f" + playerName);
                sender.sendMessage("§6✓ ¡" + playerName + " subió de nivel! §fNivel " + oldLevel + " §7→ §fNivel " + newLevel);
                sender.sendMessage("§e✓ Has recibido §f" + statPointsGained + " §epuntos de estadística para distribuir");
            } else {
                sender.sendMessage("§a✓ Se agregaron §f" + amount + " §ade experiencia a §f" + playerName);
            }
            
            // Guardar cambios en la base de datos
            plugin.getDatabaseManager().updatePlayerData(playerData);
            
            // Notificar al jugador si está en línea
            if (targetPlayer.isOnline()) {
                org.bukkit.entity.Player onlinePlayer = targetPlayer.getPlayer();
                onlinePlayer.sendMessage("§a✓ Has recibido §f" + amount + " §ade experiencia");
                if (newLevel > oldLevel) {
                    int levelsGained = newLevel - oldLevel;
                    int statPointsGained = levelsGained * 5; // 5 puntos por cada nivel subido
                    onlinePlayer.sendMessage("§6§l¡FELICIDADES! §r§6Has subido al nivel §f" + newLevel + "§6!");
                    onlinePlayer.sendMessage("§e✓ Has recibido §f" + statPointsGained + " §epuntos de estadística para distribuir");
                    // Opcional: reproducir sonido de nivel
                    onlinePlayer.playSound(onlinePlayer.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c✗ Cantidad inválida: " + amountStr);
        }
    }

    private void showRaces(Player player) {
        player.sendMessage("§6========== Razas Disponibles ==========");
        for (java.util.Map.Entry<String, com.irdem.tunama.data.Race> entry : plugin.getRaceManager().getAllRaces().entrySet()) {
            Race race = entry.getValue();
            player.sendMessage("§f" + race.getName() + " §7(" + race.getId() + ")");
            player.sendMessage("  §8→ " + race.getDescription());
        }
        player.sendMessage("§6========================================");
        player.sendMessage("§fUsa /rpg info <raza> para más detalles");
    }

    private void showClasses(Player player) {
        player.sendMessage("§6========== Clases Disponibles ==========");
        for (java.util.Map.Entry<String, com.irdem.tunama.data.RPGClass> entry : plugin.getClassManager().getAllClasses().entrySet()) {
            RPGClass rpgClass = entry.getValue();
            player.sendMessage("§f" + rpgClass.getName() + " §7(" + rpgClass.getId() + ")");
            player.sendMessage("  §8→ " + rpgClass.getDescription());
        }
        player.sendMessage("§6=========================================");
        player.sendMessage("§fUsa /rpg info <clase> para más detalles");
    }

    private void showInfo(Player player, String name) {
        name = name.toLowerCase();

        // Buscar en razas
        if (plugin.getRaceManager().isValidRace(name)) {
            Race race = plugin.getRaceManager().getRace(name);
            player.sendMessage("§6========== " + race.getName() + " ==========");
            player.sendMessage("§7" + race.getDescription());
            player.sendMessage("§aVentajas: §f" + race.getAdvantages());
            player.sendMessage("§cDesventajas: §f" + race.getDisadvantages());
            player.sendMessage("§6==================================");
            return;
        }

        // Buscar en clases
        if (plugin.getClassManager().isValidClass(name)) {
            RPGClass rpgClass = plugin.getClassManager().getClass(name);
            player.sendMessage("§6========== " + rpgClass.getName() + " ==========");
            player.sendMessage("§7" + rpgClass.getDescription());
            player.sendMessage("§aVentajas: §f" + rpgClass.getAdvantages());
            player.sendMessage("§cDesventajas: §f" + rpgClass.getDisadvantages());
            java.util.List<String> subclassNames = new java.util.ArrayList<>();
            for (String subId : rpgClass.getSubclasses()) {
                Subclass sub = plugin.getSubclassManager().getSubclass(subId);
                subclassNames.add(sub != null ? sub.getName() : subId);
            }
            player.sendMessage("§9Subclases: §f" + String.join(", ", subclassNames));
            player.sendMessage("§6==================================");
            return;
        }

        // Buscar en subclases
        if (plugin.getSubclassManager().isValidSubclass(name)) {
            Subclass subclass = plugin.getSubclassManager().getSubclass(name);
            RPGClass parentClass = subclass.getParentClass() != null ?
                plugin.getClassManager().getClass(subclass.getParentClass().toLowerCase()) : null;
            String parentDisplay = parentClass != null ? parentClass.getName() : subclass.getParentClass();
            player.sendMessage("§6========== " + subclass.getName() + " ==========");
            player.sendMessage("§7" + subclass.getDescription());
            player.sendMessage("§9Clase: §f" + parentDisplay);
            player.sendMessage("§aVentajas: §f" + subclass.getAdvantages());
            player.sendMessage("§cDesventajas: §f" + subclass.getDisadvantages());
            player.sendMessage("§6==================================");
            return;
        }

        player.sendMessage("§cNo se encontró información para: " + name);
    }

    private void reloadConfigs(CommandSender sender) {
        try {
            plugin.getRaceManager().loadRaces();
            plugin.getClassManager().loadClasses();
            plugin.getSubclassManager().loadSubclasses();
            plugin.getMissionManager().loadMissions();
            plugin.getAchievementManager().loadAchievements();
            plugin.getAbilityManager().loadAbilities();
            plugin.getExperienceManager().reload();
            
            sender.sendMessage("§a✓ Todas las configuraciones recargadas exitosamente");
            sender.sendMessage("§7Razas: §f" + plugin.getRaceManager().getAllRaces().size());
            sender.sendMessage("§7Clases: §f" + plugin.getClassManager().getAllClasses().size());
            sender.sendMessage("§7Subclases: §f" + plugin.getSubclassManager().getAllSubclasses().size());
            sender.sendMessage("§7Misiones: §f" + plugin.getMissionManager().getAllMissions().size());
            sender.sendMessage("§7Logros: §f" + plugin.getAchievementManager().getAllAchievements().size());
            sender.sendMessage("§7Habilidades: §f" + plugin.getAbilityManager().getAllAbilities().size());
            sender.sendMessage("§7Archivos de experiencia: §fRecargados");
            plugin.getItemManager().reload();
            sender.sendMessage("§7Objetos: §f" + plugin.getItemManager().getAllItemIds().size());
        } catch (Exception e) {
            sender.sendMessage("§c✗ Error al recargar las configuraciones");
            sender.sendMessage("§c" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGiveItem(CommandSender sender, String[] args) {
        // /rpg objeto <jugador> <id_objeto> [cantidad]
        if (args.length < 3) {
            sender.sendMessage("§cUso: /rpg objeto <jugador> <id_objeto> [cantidad]");
            sender.sendMessage("§7Objetos disponibles:");
            for (String itemId : plugin.getItemManager().getAllItemIds()) {
                com.irdem.tunama.data.RPGItem item = plugin.getItemManager().getItem(itemId);
                sender.sendMessage("  §f- " + itemId + " §7(" + item.getName() + "§7)");
            }
            return;
        }

        String playerName = args[1];
        String itemId = args[2];
        int amount = 1;

        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1) amount = 1;
                if (amount > 64) amount = 64;
            } catch (NumberFormatException e) {
                sender.sendMessage("§c✗ Cantidad inválida: " + args[3]);
                return;
            }
        }

        // Verificar que el objeto existe
        if (!plugin.getItemManager().itemExists(itemId)) {
            sender.sendMessage("§c✗ Objeto no encontrado: " + itemId);
            sender.sendMessage("§7Usa /rpg objeto para ver la lista de objetos disponibles");
            return;
        }

        // Buscar al jugador
        Player targetPlayer = plugin.getServer().getPlayer(playerName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("§c✗ Jugador no encontrado o no está conectado: " + playerName);
            return;
        }

        // Crear y dar los objetos (cada uno es único, no se stackean)
        com.irdem.tunama.data.RPGItem rpgItem = plugin.getItemManager().getItem(itemId);
        if (rpgItem == null) {
            sender.sendMessage("§c✗ Error al crear el objeto");
            return;
        }

        int given = 0;
        int dropped = 0;
        for (int i = 0; i < amount; i++) {
            org.bukkit.inventory.ItemStack itemStack = rpgItem.toItemStack(1);
            java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> leftover = targetPlayer.getInventory().addItem(itemStack);
            if (leftover.isEmpty()) {
                given++;
            } else {
                // Si no cabe en inventario, soltar en el suelo
                for (org.bukkit.inventory.ItemStack drop : leftover.values()) {
                    targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), drop);
                }
                dropped++;
            }
        }

        sender.sendMessage("§a✓ Has dado §f" + amount + "x " + rpgItem.getName() + " §aa §f" + targetPlayer.getName());
        targetPlayer.sendMessage("§a✓ Has recibido §f" + amount + "x " + rpgItem.getName());
        if (dropped > 0) {
            sender.sendMessage("§e⚠ " + dropped + " objeto(s) fueron soltados al suelo por falta de espacio.");
            targetPlayer.sendMessage("§e⚠ " + dropped + " objeto(s) fueron soltados al suelo por falta de espacio.");
        }
    }

    private void giveStatPoints(CommandSender sender, String playerName, String amountStr) {
        try {
            int amount = Integer.parseInt(amountStr);

            org.bukkit.OfflinePlayer targetPlayer = plugin.getServer().getOfflinePlayer(playerName);
            if (targetPlayer == null || !targetPlayer.hasPlayedBefore()) {
                sender.sendMessage("§c✗ Jugador no encontrado: " + playerName);
                return;
            }

            PlayerData playerData = plugin.getDatabaseManager().getPlayerData(targetPlayer.getUniqueId());
            if (playerData == null) {
                sender.sendMessage("§c✗ No se pudieron cargar los datos del jugador");
                return;
            }

            playerData.addStatPoints(amount);
            plugin.getDatabaseManager().updatePlayerData(playerData);

            sender.sendMessage("§a✓ Se dieron §f" + amount + " §apuntos de estadística a §f" + playerName);
            sender.sendMessage("§7Puntos totales disponibles: §f" + playerData.getStatPoints());

            // Notificar al jugador si está en línea
            if (targetPlayer.isOnline()) {
                Player onlinePlayer = targetPlayer.getPlayer();
                onlinePlayer.sendMessage("§a✓ Has recibido §f" + amount + " §apuntos de estadística");
                onlinePlayer.sendMessage("§7Usa /estadisticas para distribuirlos");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c✗ Cantidad inválida: " + amountStr);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Subcomandos básicos para todos los jugadores
            List<String> subcommands = new ArrayList<>();
            subcommands.add("help");
            subcommands.add("razas");
            subcommands.add("clases");
            subcommands.add("info");
            subcommands.add("personajes");
            subcommands.add("renacer");

            // Subcomandos admin
            if (sender.hasPermission("rpg.admin") || sender.isOp() || !(sender instanceof Player)) {
                subcommands.add("experiencia");
                subcommands.add("estadisticas");
                subcommands.add("objeto");
                subcommands.add("reload");
            }

            String partial = args[0].toLowerCase();
            for (String sub : subcommands) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String subcommand = args[0].toLowerCase();

            if ((subcommand.equals("experiencia") || subcommand.equals("exp") || subcommand.equals("objeto") || subcommand.equals("item") || subcommand.equals("estadisticas") || subcommand.equals("stats"))
                    && (sender.hasPermission("rpg.admin") || sender.isOp() || !(sender instanceof Player))) {
                // Tab-complete con nombres de jugadores online
                String partial = args[1].toLowerCase();
                for (Player online : plugin.getServer().getOnlinePlayers()) {
                    if (online.getName().toLowerCase().startsWith(partial)) {
                        completions.add(online.getName());
                    }
                }
            } else if (subcommand.equals("info")) {
                // Tab-complete con nombres de razas, clases y subclases
                String partial = args[1].toLowerCase();
                for (String raceId : plugin.getRaceManager().getAllRaces().keySet()) {
                    if (raceId.startsWith(partial)) {
                        completions.add(raceId);
                    }
                }
                for (String classId : plugin.getClassManager().getAllClasses().keySet()) {
                    if (classId.startsWith(partial)) {
                        completions.add(classId);
                    }
                }
                for (String subclassId : plugin.getSubclassManager().getAllSubclasses().keySet()) {
                    if (subclassId.startsWith(partial)) {
                        completions.add(subclassId);
                    }
                }
            }
        } else if (args.length == 3) {
            String subcommand = args[0].toLowerCase();

            if ((subcommand.equals("objeto") || subcommand.equals("item"))
                    && (sender.hasPermission("rpg.admin") || sender.isOp() || !(sender instanceof Player))) {
                // Tab-complete con IDs de objetos RPG
                String partial = args[2].toLowerCase();
                for (String itemId : plugin.getItemManager().getAllItemIds()) {
                    if (itemId.toLowerCase().startsWith(partial)) {
                        completions.add(itemId);
                    }
                }
            }
        }

        return completions;
    }
}
