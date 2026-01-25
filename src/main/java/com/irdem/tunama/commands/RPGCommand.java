package com.irdem.tunama.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.irdem.tunama.TunameRPG;
import com.irdem.tunama.data.Race;
import com.irdem.tunama.data.RPGClass;
import com.irdem.tunama.data.Subclass;

public class RPGCommand implements CommandExecutor {

    private TunameRPG plugin;

    public RPGCommand(TunameRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser usado por jugadores!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
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
            default:
                player.sendMessage("§cComando desconocido. Usa /rpg help para ver los comandos disponibles");
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6========== TunameRPG ==========");
        player.sendMessage("§f/rpg razas §7- Ver todas las razas disponibles");
        player.sendMessage("§f/rpg clases §7- Ver todas las clases disponibles");
        player.sendMessage("§f/rpg info <raza|clase|subclase> §7- Ver información detallada");
        player.sendMessage("§f/rpg help §7- Ver esta ayuda");
        player.sendMessage("§6================================");
    }

    private void showRaces(Player player) {
        player.sendMessage("§6========== Razas Disponibles ==========");
        for (java.util.Map.Entry<String, com.irdem.tunama.data.Race> entry : plugin.getRaceManager().getAllRaces().entrySet()) {
            Race race = entry.getValue();
            player.sendMessage("§f" + race.getName() + " §7(" + entry.getKey() + ")");
            player.sendMessage("  §8→ " + race.getDescription());
        }
        player.sendMessage("§6========================================");
        player.sendMessage("§fUsa /rpg info <raza> para más detalles");
    }

    private void showClasses(Player player) {
        player.sendMessage("§6========== Clases Disponibles ==========");
        for (java.util.Map.Entry<String, com.irdem.tunama.data.RPGClass> entry : plugin.getClassManager().getAllClasses().entrySet()) {
            RPGClass rpgClass = entry.getValue();
            player.sendMessage("§f" + rpgClass.getName() + " §7(" + entry.getKey() + ")");
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
            player.sendMessage("§9Subclases: §f" + String.join(", ", rpgClass.getSubclasses()));
            player.sendMessage("§6==================================");
            return;
        }

        // Buscar en subclases
        if (plugin.getSubclassManager().isValidSubclass(name)) {
            Subclass subclass = plugin.getSubclassManager().getSubclass(name);
            player.sendMessage("§6========== " + subclass.getName() + " ==========");
            player.sendMessage("§7" + subclass.getDescription());
            player.sendMessage("§9Clase: §f" + subclass.getParentClass());
            player.sendMessage("§aVentajas: §f" + subclass.getAdvantages());
            player.sendMessage("§cDesventajas: §f" + subclass.getDisadvantages());
            player.sendMessage("§6==================================");
            return;
        }

        player.sendMessage("§cNo se encontró información para: " + name);
    }
}
