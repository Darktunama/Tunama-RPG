package com.irdem.tunama.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.PlayerData;
import com.irdem.tunama.menus.StatsMenu;

public class StatsCommand implements CommandExecutor {

    private TunamaRPG plugin;

    public StatsCommand(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c✗ Este comando solo puede ser usado por jugadores!");
            return true;
        }

        Player player = (Player) sender;

        PlayerData playerData = plugin.getDatabaseManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());

        if (playerData == null) {
            player.sendMessage("§c✗ Error: No se pudo obtener tus datos. Contacta al administrador.");
            return true;
        }

        StatsMenu statsMenu = new StatsMenu(plugin, player, playerData);
        statsMenu.open();

        return true;
    }
}
