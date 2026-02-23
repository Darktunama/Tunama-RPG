package com.irdem.tunama.commands;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.Clan;
import com.irdem.tunama.managers.ClanManager;
import com.irdem.tunama.menus.clan.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;

public class ClanCommand implements CommandExecutor, TabCompleter {
    private final TunamaRPG plugin;
    private final ClanManager clanManager;

    // Almacena confirmaciones pendientes para disolución de clanes
    private final Map<UUID, String> pendingDissolutions;
    private final Map<UUID, String> pendingLeave;

    public ClanCommand(TunamaRPG plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.pendingDissolutions = new HashMap<>();
        this.pendingLeave = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores");
            return true;
        }

        Player player = (Player) sender;

        // /clan - Abrir menú principal
        if (args.length == 0) {
            openMainMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "crear":
            case "create":
                handleCreate(player, args);
                break;

            case "disolver":
            case "delete":
                handleDissolve(player);
                break;

            case "miembros":
            case "members":
                handleMembers(player);
                break;

            case "top":
                handleTop(player);
                break;

            case "banco":
            case "bank":
                handleBank(player);
                break;

            case "gestion":
            case "gestión":
            case "manage":
                handleManage(player);
                break;

            case "alianza":
            case "ally":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /clan alianza <solicitar|retirar> <tag>");
                    return true;
                }

                if (args.length >= 3) {
                    // Subcomandos con tag
                    String subcommand = args[1].toLowerCase();
                    String targetTag = args[2];

                    switch (subcommand) {
                        case "solicitar":
                            handleAlliance(player, targetTag);
                            break;
                        case "retirar":
                        case "cancelar":
                            handleRemoveAlliance(player, targetTag);
                            break;
                        default:
                            player.sendMessage("§cUso: /clan alianza <solicitar|retirar> <tag>");
                            break;
                    }
                } else {
                    // Retrocompatibilidad: solo con tag = solicitar alianza
                    handleAlliance(player, args[1]);
                }
                break;

            case "invitar":
            case "invite":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /clan invitar <jugador>");
                    return true;
                }
                handleInvite(player, args[1]);
                break;

            case "expulsar":
            case "kick":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /clan expulsar <jugador>");
                    return true;
                }
                handleKick(player, args[1]);
                break;

            case "guerra":
            case "war":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /clan guerra <declarar|paz> <tag>");
                    return true;
                }

                if (args.length >= 3) {
                    // Subcomandos con tag
                    String subcommand = args[1].toLowerCase();
                    String targetTag = args[2];

                    switch (subcommand) {
                        case "declarar":
                            handleWar(player, targetTag);
                            break;
                        case "paz":
                        case "peace":
                            handlePeace(player, targetTag);
                            break;
                        default:
                            player.sendMessage("§cUso: /clan guerra <declarar|paz> <tag>");
                            break;
                    }
                } else {
                    // Retrocompatibilidad: solo con tag = declarar guerra
                    handleWar(player, args[1]);
                }
                break;

            case "admin":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /clan admin <tag>");
                    return true;
                }
                handleAdmin(player, args[1]);
                break;

            case "salir":
            case "leave":
                handleLeave(player);
                break;

            case "logs":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /clan logs <tag> [pagina]");
                    return true;
                }
                int logsPage = 0;
                if (args.length >= 3) {
                    try {
                        logsPage = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        logsPage = 0;
                    }
                }
                handleLogs(player, args[1], logsPage);
                break;

            case "confirmar_disolucion":
                confirmDissolution(player);
                break;

            case "cancelar_disolucion":
                cancelDissolution(player);
                break;

            case "confirmar_salida":
                confirmLeave(player);
                break;

            case "cancelar_salida":
                cancelLeave(player);
                break;

            case "aceptar_invitacion":
                acceptInvitation(player);
                break;

            case "rechazar_invitacion":
                denyInvitation(player);
                break;

            case "transferir":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /clan transferir <confirmar|cancelar>");
                    return true;
                }

                switch (args[1].toLowerCase()) {
                    case "confirmar":
                        confirmTransferLeadership(player);
                        break;
                    case "cancelar":
                        cancelTransferLeadership(player);
                        break;
                    default:
                        player.sendMessage("§cUso: /clan transferir <confirmar|cancelar>");
                        break;
                }
                break;

            case "help":
            case "ayuda":
                handleHelp(player);
                break;

            default:
                player.sendMessage("§cSubcomando desconocido. Usa /clan help");
                break;
        }

        return true;
    }

    private void openMainMenu(Player player) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());

        if (clan == null) {
            // No tiene clan - mostrar menú de creación
            ClanNoClanMenu menu = new ClanNoClanMenu(plugin);
            menu.open(player);
        } else {
            // Tiene clan - mostrar menú principal del clan
            ClanMainMenu menu = new ClanMainMenu(plugin, clan);
            menu.open(player);
        }
    }

    private void handleCreate(Player player, String[] args) {
        // Verificar si ya tiene clan
        if (clanManager.hasPlayerClan(player.getUniqueId())) {
            player.sendMessage("§c✗ Ya tienes un clan. Debes abandonar tu clan actual para crear uno nuevo.");
            player.sendMessage("§7Usa §f/clan salir §7para salir de tu clan");
            return;
        }

        // Iniciar proceso de creación mediante chat
        plugin.getChatInputListener().startClanCreation(player);
    }


    private void handleDissolve(Player player) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());

        if (clan == null) {
            player.sendMessage("§c✗ No tienes un clan");
            return;
        }

        if (!clan.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage("§c✗ Solo el líder puede disolver el clan");
            return;
        }

        // Mostrar confirmación
        player.sendMessage("");
        player.sendMessage("§c§l⚠ ADVERTENCIA ⚠");
        player.sendMessage("");
        player.sendMessage("§7¿Estás seguro de que quieres §cdisolver§7 el clan?");
        player.sendMessage("§7Clan: §f" + clan.getName() + " " + clan.getFormattedTag());
        player.sendMessage("§7Miembros: §f" + clan.getMemberCount());
        player.sendMessage("");
        player.sendMessage("§c§lEsta acción NO se puede deshacer");
        player.sendMessage("");

        Component yesButton = Component.text("  [SÍ, DISOLVER]  ")
            .color(NamedTextColor.GREEN)
            .decorate(TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/clan confirmar_disolucion"));

        Component noButton = Component.text("  [NO, CANCELAR]  ")
            .color(NamedTextColor.RED)
            .decorate(TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/clan cancelar_disolucion"));

        player.sendMessage(Component.empty()
            .append(yesButton)
            .append(Component.text("     "))
            .append(noButton));

        player.sendMessage("");

        pendingDissolutions.put(player.getUniqueId(), clan.getTag());
    }

    public void confirmDissolution(Player player) {
        String clanTag = pendingDissolutions.remove(player.getUniqueId());

        if (clanTag == null) {
            player.sendMessage("§c✗ No tienes ninguna disolución pendiente");
            return;
        }

        Clan clan = clanManager.getClanByTag(clanTag);
        if (clan == null) {
            player.sendMessage("§c✗ El clan ya no existe");
            return;
        }

        if (!clan.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage("§c✗ Solo el líder puede disolver el clan");
            return;
        }

        try {
            // Notificar a todos los miembros
            for (UUID memberId : clan.getMembers().keySet()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    member.sendMessage("");
                    member.sendMessage("§c§l⚠ TU CLAN HA SIDO DISUELTO");
                    member.sendMessage("§7El clan §f" + clan.getName() + " §7ha sido eliminado por su líder");
                    member.sendMessage("");
                }
            }

            clanManager.deleteClan(clanTag);

            player.sendMessage("§a✓ El clan ha sido disuelto correctamente");

        } catch (SQLException e) {
            player.sendMessage("§c✗ Error al disolver el clan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void cancelDissolution(Player player) {
        pendingDissolutions.remove(player.getUniqueId());
        player.sendMessage("§e⚠ Disolución del clan cancelada");
    }

    private void handleMembers(Player player) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());

        if (clan == null) {
            player.sendMessage("§c✗ No tienes un clan");
            return;
        }

        ClanMembersMenu menu = new ClanMembersMenu(plugin, clan);
        menu.open(player);
    }

    private void handleTop(Player player) {
        ClanTopMenu menu = new ClanTopMenu(plugin);
        menu.open(player);
    }

    private void handleBank(Player player) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());

        if (clan == null) {
            player.sendMessage("§c✗ No tienes un clan");
            return;
        }

        ClanBankMenu menu = new ClanBankMenu(plugin, clan, player);
        menu.open(player);
    }

    private void handleManage(Player player) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());

        if (clan == null) {
            player.sendMessage("§c✗ No tienes un clan");
            return;
        }

        Clan.ClanRank rank = clan.getMemberRank(player.getUniqueId());
        if (rank != Clan.ClanRank.LEADER && rank != Clan.ClanRank.COMMANDER) {
            player.sendMessage("§c✗ Solo el líder y los comandantes pueden gestionar el clan");
            return;
        }

        ClanManageMenu menu = new ClanManageMenu(plugin, clan, player);
        menu.open(player);
    }

    private void handleAlliance(Player player, String targetTag) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());

        if (clan == null) {
            player.sendMessage("§c✗ No tienes un clan");
            return;
        }

        if (!clan.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage("§c✗ Solo el líder puede gestionar alianzas");
            return;
        }

        Clan targetClan = clanManager.getClanByTag(targetTag);
        if (targetClan == null) {
            player.sendMessage("§c✗ No existe un clan con ese tag");
            return;
        }

        String targetActualTag = targetClan.getTag().toUpperCase();
        String ownActualTag = clan.getTag().toUpperCase();

        if (targetClan.getId() == clan.getId()) {
            player.sendMessage("§c✗ No puedes aliarte con tu propio clan");
            return;
        }

        if (clan.isAlly(targetActualTag)) {
            player.sendMessage("§e⚠ Ya sois aliados de ese clan");
            return;
        }

        if (clan.isEnemy(targetActualTag)) {
            player.sendMessage("§c✗ No puedes aliarte con un clan con el que estás en guerra");
            return;
        }

        try {
            // Bidireccional: ambos clanes se agregan como aliados
            clan.addAlly(targetActualTag);
            clanManager.addAllyToDatabase(clan.getId(), targetActualTag);

            targetClan.addAlly(ownActualTag);
            clanManager.addAllyToDatabase(targetClan.getId(), ownActualTag);

            player.sendMessage("§a✓ Alianza establecida con §f" + targetClan.getFormattedTag() + " " + targetClan.getName());

            // Notificar a TODOS los miembros de ambos clanes
            notifyClanMembers(clan, "§a✓ §fNueva alianza con " + targetClan.getFormattedTag() + " §f" + targetClan.getName(), player.getUniqueId());
            notifyClanMembers(targetClan, "§a✓ §f" + clan.getFormattedTag() + " §f" + clan.getName() + " §aha establecido una alianza con vuestro clan", null);

        } catch (SQLException e) {
            player.sendMessage("§c✗ Error al establecer alianza: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleInvite(Player player, String targetName) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());

        if (clan == null) {
            player.sendMessage("§c✗ No tienes un clan");
            return;
        }

        Clan.ClanRank rank = clan.getMemberRank(player.getUniqueId());
        if (rank != Clan.ClanRank.LEADER && rank != Clan.ClanRank.COMMANDER) {
            player.sendMessage("§c✗ Solo el líder y los comandantes pueden invitar miembros");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§c✗ El jugador no está conectado");
            return;
        }

        if (clanManager.hasPlayerClan(target.getUniqueId())) {
            player.sendMessage("§c✗ Ese jugador ya tiene un clan");
            return;
        }

        // Enviar invitación
        clanManager.addInvite(target.getUniqueId(), clan.getTag());

        target.sendMessage("");
        target.sendMessage("§e§l⚔ INVITACIÓN DE CLAN ⚔");
        target.sendMessage("");
        target.sendMessage("§f" + player.getName() + " §7te ha invitado al clan:");
        target.sendMessage("§f" + clan.getName() + " " + clan.getFormattedTag());
        target.sendMessage("");

        Component acceptButton = Component.text("  [ACEPTAR]  ")
            .color(NamedTextColor.GREEN)
            .decorate(TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/clan aceptar_invitacion"));

        Component denyButton = Component.text("  [RECHAZAR]  ")
            .color(NamedTextColor.RED)
            .decorate(TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/clan rechazar_invitacion"));

        target.sendMessage(Component.empty()
            .append(acceptButton)
            .append(Component.text("     "))
            .append(denyButton));

        target.sendMessage("");

        player.sendMessage("§a✓ Invitación enviada a §f" + target.getName());
    }

    private void handleKick(Player player, String targetName) {
        boolean isAdmin = player.hasPermission("rpg.admin");
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());

        // Si es admin y no tiene clan, buscar el clan del target
        if (clan == null && isAdmin) {
            // Buscar el jugador target y su clan
            Player targetPlayer = Bukkit.getPlayer(targetName);
            UUID targetId = null;
            if (targetPlayer != null) {
                targetId = targetPlayer.getUniqueId();
            } else {
                org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                if (offlineTarget.hasPlayedBefore()) {
                    targetId = offlineTarget.getUniqueId();
                }
            }
            if (targetId != null) {
                clan = clanManager.getPlayerClan(targetId);
            }
        }

        if (clan == null) {
            player.sendMessage("§c✗ No tienes un clan");
            return;
        }

        Clan.ClanRank kickerRank = clan.getMemberRank(player.getUniqueId());
        boolean isLeader = clan.getLeaderId().equals(player.getUniqueId());

        if (!isAdmin && !isLeader && kickerRank != Clan.ClanRank.COMMANDER) {
            player.sendMessage("§c✗ Solo el líder y los comandantes pueden expulsar miembros");
            return;
        }

        // Buscar al jugador por nombre en los miembros del clan
        UUID targetUuid = null;
        for (UUID memberId : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.getName().equalsIgnoreCase(targetName)) {
                targetUuid = memberId;
                break;
            }
            // También buscar offline players
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberId);
            if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(targetName)) {
                targetUuid = memberId;
                break;
            }
        }

        if (targetUuid == null) {
            player.sendMessage("§c✗ Ese jugador no es miembro del clan");
            return;
        }

        // No puede expulsarse a sí mismo
        if (targetUuid.equals(player.getUniqueId())) {
            player.sendMessage("§c✗ No puedes expulsarte a ti mismo");
            return;
        }

        // No puede expulsar al líder (a menos que sea admin)
        if (!isAdmin && targetUuid.equals(clan.getLeaderId())) {
            player.sendMessage("§c✗ No puedes expulsar al líder del clan");
            return;
        }

        // Los comandantes no pueden expulsar a otros comandantes (admins sí)
        Clan.ClanRank targetRank = clan.getMemberRank(targetUuid);
        if (!isAdmin && !isLeader && targetRank == Clan.ClanRank.COMMANDER) {
            player.sendMessage("§c✗ Solo el líder puede expulsar a comandantes");
            return;
        }

        try {
            // Remover de la base de datos
            clanManager.removeMemberFromDatabase(clan.getId(), targetUuid);

            // Remover del objeto clan en memoria
            clan.removeMember(targetUuid);

            // Remover del cache de clanes
            clanManager.getPlayerClanCache().remove(targetUuid);

            player.sendMessage("§a✓ Has expulsado a §f" + targetName + " §adel clan");

            // Notificar al expulsado si está online
            Player target = Bukkit.getPlayer(targetUuid);
            if (target != null && target.isOnline()) {
                target.sendMessage("§c✗ Has sido expulsado del clan §f" + clan.getName());
            }

        } catch (SQLException e) {
            player.sendMessage("§c✗ Error al expulsar al miembro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void acceptInvitation(Player player) {
        String clanTag = clanManager.getInvite(player.getUniqueId());

        if (clanTag == null) {
            player.sendMessage("§c✗ No tienes ninguna invitación pendiente");
            return;
        }

        if (clanManager.hasPlayerClan(player.getUniqueId())) {
            player.sendMessage("§c✗ Ya tienes un clan");
            clanManager.removeInvite(player.getUniqueId());
            return;
        }

        Clan clan = clanManager.getClanByTag(clanTag);
        if (clan == null) {
            player.sendMessage("§c✗ El clan ya no existe");
            clanManager.removeInvite(player.getUniqueId());
            return;
        }

        try {
            clanManager.addMemberToDatabase(clan.getId(), player.getUniqueId(), Clan.ClanRank.MEMBER);
            clan.addMember(player.getUniqueId(), Clan.ClanRank.MEMBER);
            clanManager.getPendingInvites().remove(player.getUniqueId());

            // Actualizar el cache de clanes del jugador
            clanManager.getPlayerClanCache().put(player.getUniqueId(), clan.getTag().toLowerCase());

            player.sendMessage("§a✓ Te has unido al clan §f" + clan.getName() + " " + clan.getFormattedTag());

            // Notificar al líder
            Player leader = Bukkit.getPlayer(clan.getLeaderId());
            if (leader != null && leader.isOnline()) {
                leader.sendMessage("§a✓ §f" + player.getName() + " §ase ha unido al clan");
            }

        } catch (SQLException e) {
            player.sendMessage("§c✗ Error al unirse al clan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void denyInvitation(Player player) {
        String clanTag = clanManager.getInvite(player.getUniqueId());

        if (clanTag == null) {
            player.sendMessage("§c✗ No tienes ninguna invitación pendiente");
            return;
        }

        clanManager.removeInvite(player.getUniqueId());
        player.sendMessage("§e⚠ Invitación rechazada");
    }

    private void handleWar(Player player, String targetTag) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());

        if (clan == null) {
            player.sendMessage("§c✗ No tienes un clan");
            return;
        }

        if (!clan.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage("§c✗ Solo el líder puede declarar guerras");
            return;
        }

        Clan targetClan = clanManager.getClanByTag(targetTag);
        if (targetClan == null) {
            player.sendMessage("§c✗ No existe un clan con ese tag");
            return;
        }

        String targetActualTag = targetClan.getTag().toUpperCase();
        String ownActualTag = clan.getTag().toUpperCase();

        if (targetClan.getId() == clan.getId()) {
            player.sendMessage("§c✗ No puedes declarar guerra a tu propio clan");
            return;
        }

        if (clan.isEnemy(targetActualTag)) {
            player.sendMessage("§e⚠ Ya estáis en guerra con ese clan");
            return;
        }

        // No puedes declarar guerra a un clan aliado
        if (clan.isAlly(targetActualTag)) {
            player.sendMessage("§c✗ No puedes declarar guerra a un clan aliado");
            return;
        }

        try {
            // Bidireccional: ambos clanes se agregan como enemigos
            clan.addEnemy(targetActualTag);
            clanManager.addEnemyToDatabase(clan.getId(), targetActualTag);

            targetClan.addEnemy(ownActualTag);
            clanManager.addEnemyToDatabase(targetClan.getId(), ownActualTag);

            player.sendMessage("§c⚔ Guerra declarada contra §f" + targetClan.getFormattedTag() + " " + targetClan.getName());

            // Notificar a TODOS los miembros de ambos clanes
            notifyClanMembers(clan, "§c⚔ §fGuerra declarada contra " + targetClan.getFormattedTag() + " §f" + targetClan.getName(), player.getUniqueId());
            notifyClanMembers(targetClan, "§c⚔ §f" + clan.getFormattedTag() + " §f" + clan.getName() + " §cha declarado la guerra a vuestro clan", null);

        } catch (SQLException e) {
            player.sendMessage("§c✗ Error al declarar guerra: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleRemoveAlliance(Player player, String targetTag) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());

        if (clan == null) {
            player.sendMessage("§c✗ No tienes un clan");
            return;
        }

        Clan.ClanRank rank = clan.getMemberRank(player.getUniqueId());
        if (rank != Clan.ClanRank.LEADER && rank != Clan.ClanRank.COMMANDER) {
            player.sendMessage("§c✗ Solo el líder y los comandantes pueden cancelar alianzas");
            return;
        }

        Clan targetClan = clanManager.getClanByTag(targetTag);
        String targetActualTag = targetClan != null ? targetClan.getTag().toUpperCase() : targetTag.toUpperCase();
        String ownActualTag = clan.getTag().toUpperCase();

        if (!clan.isAlly(targetActualTag)) {
            player.sendMessage("§c✗ No sois aliados de ese clan");
            return;
        }

        try {
            // Bidireccional: eliminar alianza de ambos clanes
            clan.removeAlly(targetActualTag);
            clanManager.removeAllyFromDatabase(clan.getId(), targetActualTag);

            if (targetClan != null) {
                targetClan.removeAlly(ownActualTag);
                clanManager.removeAllyFromDatabase(targetClan.getId(), ownActualTag);
            }

            player.sendMessage("§c✗ Alianza cancelada con §f" + (targetClan != null ? targetClan.getFormattedTag() + " " + targetClan.getName() : "[" + targetActualTag + "]"));

            // Notificar a TODOS los miembros de ambos clanes
            notifyClanMembers(clan, "§e⚠ §fAlianza cancelada con " + (targetClan != null ? targetClan.getFormattedTag() + " §f" + targetClan.getName() : "[" + targetActualTag + "]"), player.getUniqueId());
            if (targetClan != null) {
                notifyClanMembers(targetClan, "§e⚠ §f" + clan.getFormattedTag() + " §f" + clan.getName() + " §eha cancelado la alianza con vuestro clan", null);
            }

        } catch (SQLException e) {
            player.sendMessage("§c✗ Error al cancelar alianza: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePeace(Player player, String targetTag) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());

        if (clan == null) {
            player.sendMessage("§c✗ No tienes un clan");
            return;
        }

        Clan.ClanRank rank = clan.getMemberRank(player.getUniqueId());
        if (rank != Clan.ClanRank.LEADER && rank != Clan.ClanRank.COMMANDER) {
            player.sendMessage("§c✗ Solo el líder y los comandantes pueden proponer paz");
            return;
        }

        Clan targetClan = clanManager.getClanByTag(targetTag);
        String targetActualTag = targetClan != null ? targetClan.getTag().toUpperCase() : targetTag.toUpperCase();
        String ownActualTag = clan.getTag().toUpperCase();

        if (!clan.isEnemy(targetActualTag)) {
            player.sendMessage("§c✗ No estáis en guerra con ese clan");
            return;
        }

        try {
            // Bidireccional: eliminar guerra de ambos clanes
            clan.removeEnemy(targetActualTag);
            clanManager.removeEnemyFromDatabase(clan.getId(), targetActualTag);

            if (targetClan != null) {
                targetClan.removeEnemy(ownActualTag);
                clanManager.removeEnemyFromDatabase(targetClan.getId(), ownActualTag);
            }

            player.sendMessage("§a✓ Paz establecida con §f" + (targetClan != null ? targetClan.getFormattedTag() + " " + targetClan.getName() : "[" + targetActualTag + "]"));

            // Notificar a TODOS los miembros de ambos clanes
            notifyClanMembers(clan, "§a✓ §fPaz establecida con " + (targetClan != null ? targetClan.getFormattedTag() + " §f" + targetClan.getName() : "[" + targetActualTag + "]"), player.getUniqueId());
            if (targetClan != null) {
                notifyClanMembers(targetClan, "§a✓ §f" + clan.getFormattedTag() + " §f" + clan.getName() + " §aha propuesto paz y terminado la guerra con vuestro clan", null);
            }

        } catch (SQLException e) {
            player.sendMessage("§c✗ Error al establecer paz: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleAdmin(Player player, String targetTag) {
        if (!player.hasPermission("rpg.admin")) {
            player.sendMessage("§c✗ No tienes permiso para usar este comando");
            return;
        }

        Clan clan = clanManager.getClanByTag(targetTag);
        if (clan == null) {
            player.sendMessage("§c✗ No existe un clan con ese tag");
            return;
        }

        ClanAdminMenu menu = new ClanAdminMenu(plugin, clan);
        menu.open(player);
    }

    private void handleLeave(Player player) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());

        if (clan == null) {
            player.sendMessage("§c✗ No tienes un clan");
            return;
        }

        if (clan.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage("§c✗ El líder no puede salir del clan");
            player.sendMessage("§7Debes transferir el liderazgo o disolver el clan");
            return;
        }

        // Mostrar confirmación
        player.sendMessage("");
        player.sendMessage("§e§l⚠ SALIR DEL CLAN ⚠");
        player.sendMessage("");
        player.sendMessage("§7¿Estás seguro de que quieres salir del clan?");
        player.sendMessage("§7Clan: §f" + clan.getName() + " " + clan.getFormattedTag());
        player.sendMessage("");

        Component yesButton = Component.text("  [SÍ, SALIR]  ")
            .color(NamedTextColor.GREEN)
            .decorate(TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/clan confirmar_salida"));

        Component noButton = Component.text("  [NO, QUEDARME]  ")
            .color(NamedTextColor.RED)
            .decorate(TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/clan cancelar_salida"));

        player.sendMessage(Component.empty()
            .append(yesButton)
            .append(Component.text("     "))
            .append(noButton));

        player.sendMessage("");

        pendingLeave.put(player.getUniqueId(), clan.getTag());
    }

    public void confirmLeave(Player player) {
        String clanTag = pendingLeave.remove(player.getUniqueId());

        if (clanTag == null) {
            player.sendMessage("§c✗ No tienes ninguna salida pendiente");
            return;
        }

        Clan clan = clanManager.getClanByTag(clanTag);
        if (clan == null) {
            player.sendMessage("§c✗ El clan ya no existe");
            return;
        }

        try {
            clan.removeMember(player.getUniqueId());
            clanManager.removeMemberFromDatabase(clan.getId(), player.getUniqueId());

            player.sendMessage("§a✓ Has salido del clan §f" + clan.getName());

            // Notificar al líder
            Player leader = Bukkit.getPlayer(clan.getLeaderId());
            if (leader != null && leader.isOnline()) {
                leader.sendMessage("§e⚠ §f" + player.getName() + " §eha salido del clan");
            }

        } catch (SQLException e) {
            player.sendMessage("§c✗ Error al salir del clan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void cancelLeave(Player player) {
        pendingLeave.remove(player.getUniqueId());
        player.sendMessage("§e⚠ Salida del clan cancelada");
    }

    private void handleLogs(Player player, String targetTag, int page) {
        Clan playerClan = clanManager.getPlayerClan(player.getUniqueId());

        // Verificar que el jugador pertenece al clan o es admin
        Clan targetClan = clanManager.getClanByTag(targetTag);
        if (targetClan == null) {
            player.sendMessage("§c✗ No existe un clan con ese tag");
            return;
        }

        boolean isAdmin = player.hasPermission("rpg.admin");
        boolean isMember = playerClan != null && playerClan.getId() == targetClan.getId();

        if (!isAdmin && !isMember) {
            player.sendMessage("§c✗ Solo puedes ver los logs de tu propio clan");
            return;
        }

        try {
            int perPage = 10;
            int totalLogs = clanManager.getLogsCount(targetClan.getId());
            int totalPages = (int) Math.ceil((double) totalLogs / perPage);

            if (totalPages == 0) totalPages = 1;
            if (page < 0) page = 0;
            if (page >= totalPages) page = totalPages - 1;

            var logs = clanManager.getLogsPaginated(targetClan.getId(), page, perPage);

            player.sendMessage("");
            player.sendMessage("§6§l⚔ LOGS DEL CLAN ⚔ §7(Página " + (page + 1) + "/" + totalPages + ")");
            player.sendMessage("");

            if (logs.isEmpty()) {
                player.sendMessage("§7No hay logs registrados");
            } else {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM HH:mm");
                for (var log : logs) {
                    String action = log.getAction().replace("_", " ");
                    String date = sdf.format(new java.util.Date(log.getTimestamp()));

                    if (log.getAction().contains("ORO")) {
                        player.sendMessage("§8[" + date + "] §f" + log.getPlayerName() + " §7- " + action + ": §6" + log.getAmount());
                    } else if (log.getItem() != null) {
                        player.sendMessage("§8[" + date + "] §f" + log.getPlayerName() + " §7- " + action + ": §f" + log.getItem());
                    }
                }
            }

            player.sendMessage("");

            // Crear botones de navegación
            Component navigation = Component.empty();

            if (page > 0) {
                navigation = navigation.append(
                    Component.text("§a§l« Anterior ")
                        .clickEvent(ClickEvent.runCommand("/clan logs " + targetTag + " " + (page - 1)))
                );
            }

            navigation = navigation.append(
                Component.text("§7[" + (page + 1) + "/" + totalPages + "]")
            );

            if (page < totalPages - 1) {
                navigation = navigation.append(
                    Component.text(" §a§lSiguiente »")
                        .clickEvent(ClickEvent.runCommand("/clan logs " + targetTag + " " + (page + 1)))
                );
            }

            player.sendMessage(navigation);
            player.sendMessage("");

        } catch (Exception e) {
            player.sendMessage("§c✗ Error al cargar logs");
            e.printStackTrace();
        }
    }

    private void handleHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§e§l⚔ COMANDOS DE CLAN ⚔");
        player.sendMessage("");

        boolean hasClan = clanManager.hasPlayerClan(player.getUniqueId());
        Clan clan = hasClan ? clanManager.getPlayerClan(player.getUniqueId()) : null;
        boolean isLeader = clan != null && clan.getLeaderId().equals(player.getUniqueId());
        boolean isCommander = clan != null && clan.getMemberRank(player.getUniqueId()) == Clan.ClanRank.COMMANDER;
        boolean isAdmin = player.hasPermission("rpg.admin");

        player.sendMessage("§7• §f/clan §7- Abrir menú del clan");

        if (!hasClan) {
            player.sendMessage("§7• §f/clan crear §7- Crear un clan");
        }

        if (hasClan) {
            player.sendMessage("§7• §f/clan miembros §7- Ver miembros del clan");
            player.sendMessage("§7• §f/clan banco §7- Acceder al banco del clan");

            if (isLeader || isCommander) {
                player.sendMessage("§7• §f/clan gestion §7- Gestionar el clan");
                player.sendMessage("§7• §f/clan invitar <jugador> §7- Invitar a un jugador");
            }

            if (isLeader) {
                player.sendMessage("§7• §f/clan alianza <tag> §7- Establecer alianza");
                player.sendMessage("§7• §f/clan guerra <tag> §7- Declarar guerra");
                player.sendMessage("§7• §f/clan disolver §7- Disolver el clan");
            }

            if (!isLeader) {
                player.sendMessage("§7• §f/clan salir §7- Salir del clan");
            }
        }

        player.sendMessage("§7• §f/clan top §7- Ver ranking de clanes");

        if (isAdmin) {
            player.sendMessage("§c• §f/clan admin <tag> §c- Gestión administrativa");
        }

        player.sendMessage("");
    }

    private void confirmTransferLeadership(Player player) {
        ClanTransferLeadershipMenu.PendingTransfer transfer = ClanTransferLeadershipMenu.getPendingTransfer(player.getUniqueId());

        if (transfer == null) {
            player.sendMessage("§c✗ No hay ninguna transferencia pendiente");
            return;
        }

        Clan clan = transfer.getClan();

        // Verificar que el jugador sigue siendo el líder (o es admin)
        boolean isAdmin = player.hasPermission("rpg.admin");
        if (!isAdmin && !clan.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage("§c✗ No eres el líder del clan");
            ClanTransferLeadershipMenu.removePendingTransfer(player.getUniqueId());
            return;
        }

        try {
            // Transferir liderazgo
            plugin.getClanManager().transferLeadership(clan.getId(), transfer.getNewLeaderId());

            player.sendMessage("");
            player.sendMessage("§6✓ Has transferido el liderazgo de §f" + clan.getName() + " §6a §f" + transfer.getNewLeaderName());
            player.sendMessage("");

            // Notificar al nuevo líder
            Player newLeader = Bukkit.getPlayer(transfer.getNewLeaderId());
            if (newLeader != null && newLeader.isOnline()) {
                newLeader.sendMessage("");
                newLeader.sendMessage("§6§l⚔ NUEVO LÍDER ⚔");
                newLeader.sendMessage("");
                newLeader.sendMessage("§aAhora eres el líder de §f" + clan.getName());
                newLeader.sendMessage("");
            }

            // Notificar a todos los miembros del clan
            for (UUID memberId : clan.getMembers().keySet()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline() && !member.equals(player) && !member.equals(newLeader)) {
                    member.sendMessage("");
                    member.sendMessage("§6El liderazgo de §f" + clan.getName() + " §6ha sido transferido a §f" + transfer.getNewLeaderName());
                    member.sendMessage("");
                }
            }

            ClanTransferLeadershipMenu.removePendingTransfer(player.getUniqueId());
        } catch (Exception e) {
            player.sendMessage("§c✗ Error al transferir el liderazgo");
            e.printStackTrace();
            ClanTransferLeadershipMenu.removePendingTransfer(player.getUniqueId());
        }
    }

    private void cancelTransferLeadership(Player player) {
        ClanTransferLeadershipMenu.PendingTransfer transfer = ClanTransferLeadershipMenu.getPendingTransfer(player.getUniqueId());

        if (transfer == null) {
            player.sendMessage("§c✗ No hay ninguna transferencia pendiente");
            return;
        }

        ClanTransferLeadershipMenu.removePendingTransfer(player.getUniqueId());
        player.sendMessage("");
        player.sendMessage("§c✗ Transferencia de liderazgo cancelada");
        player.sendMessage("");

        // Reabrir menú de gestión
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ClanManageMenu manageMenu = new ClanManageMenu(plugin, transfer.getClan(), player);
            manageMenu.open(player);
        }, 5L);
    }

    /**
     * Notifica a todos los miembros online de un clan con un mensaje.
     * @param clan El clan cuyos miembros serán notificados
     * @param message El mensaje a enviar
     * @param excludeUuid UUID del jugador a excluir (puede ser null para notificar a todos)
     */
    private void notifyClanMembers(Clan clan, String message, UUID excludeUuid) {
        for (UUID memberId : clan.getMembers().keySet()) {
            if (excludeUuid != null && memberId.equals(excludeUuid)) {
                continue;
            }
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(message);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("crear", "disolver", "miembros", "top", "banco",
                "gestion", "alianza", "invitar", "guerra", "admin", "salir", "help");

            for (String sub : subcommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("invitar")) {
            // Autocompletar nombres de jugadores online
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
        }

        return completions;
    }
}
