package com.irdem.tunama.listeners;

import com.irdem.tunama.TunamaRPG;
import com.irdem.tunama.data.Clan;
import com.irdem.tunama.menus.clan.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Listener para manejar clicks en menús de clanes
 */
public class ClanMenuListener implements Listener {
    private final TunamaRPG plugin;

    public ClanMenuListener(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        InventoryHolder holder = event.getInventory().getHolder();

        // Menú cuando no tienes clan
        if (holder instanceof ClanNoClanMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            if (slot == 11) { // Crear Clan
                player.closeInventory();
                player.performCommand("clan crear");
            } else if (slot == 15) { // Top Clanes
                ClanTopMenu topMenu = new ClanTopMenu(plugin);
                topMenu.open(player);
            }
        }

        // Menú principal del clan
        else if (holder instanceof ClanMainMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            ClanMainMenu menu = (ClanMainMenu) holder;
            Clan clan = menu.getClan();

            if (slot == 20) { // Miembros
                ClanMembersMenu membersMenu = new ClanMembersMenu(plugin, clan);
                membersMenu.open(player);
            } else if (slot == 22) { // Banco
                ClanBankMenu bankMenu = new ClanBankMenu(plugin, clan, player);
                bankMenu.open(player);
            } else if (slot == 24) { // Alianzas
                ClanAlliesMenu alliesMenu = new ClanAlliesMenu(plugin, clan, player);
                alliesMenu.open(player);
            } else if (slot == 30) { // Guerras
                ClanWarsMenu warsMenu = new ClanWarsMenu(plugin, clan, player);
                warsMenu.open(player);
            } else if (slot == 32) { // Gestión
                Clan.ClanRank rank = clan.getMemberRank(player.getUniqueId());
                if (rank == Clan.ClanRank.LEADER || rank == Clan.ClanRank.COMMANDER) {
                    ClanManageMenu manageMenu = new ClanManageMenu(plugin, clan, player);
                    manageMenu.open(player);
                }
            } else if (slot == 49) { // Salir del clan
                if (!clan.getLeaderId().equals(player.getUniqueId())) {
                    player.closeInventory();
                    player.performCommand("clan salir");
                }
            }
        }

        // Menú de miembros
        else if (holder instanceof ClanMembersMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            ClanMembersMenu menu = (ClanMembersMenu) holder;

            if (slot == 45) { // Página anterior
                if (menu.getCurrentPage() > 0) {
                    menu.open(player, menu.getCurrentPage() - 1);
                }
            } else if (slot == 53) { // Página siguiente
                menu.open(player, menu.getCurrentPage() + 1);
            } else if (slot == 48) { // Volver
                ClanMainMenu mainMenu = new ClanMainMenu(plugin, menu.getClan());
                mainMenu.open(player);
            }
        }

        // Menú top de clanes
        else if (holder instanceof ClanTopMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            ClanTopMenu menu = (ClanTopMenu) holder;

            if (slot == 3) { // Top Oro
                menu.open(player, ClanTopMenu.TopType.GOLD);
            } else if (slot == 5) { // Top Kills
                menu.open(player, ClanTopMenu.TopType.KILLS);
            } else if (slot == 49) { // Volver
                Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
                if (clan != null) {
                    ClanMainMenu mainMenu = new ClanMainMenu(plugin, clan);
                    mainMenu.open(player);
                } else {
                    ClanNoClanMenu noClanMenu = new ClanNoClanMenu(plugin);
                    noClanMenu.open(player);
                }
            }
        }

        // Menú del banco
        else if (holder instanceof ClanBankMenu) {
            ClanBankMenu menu = (ClanBankMenu) holder;
            int slot = event.getRawSlot();

            // Slots 0-44 son el banco, permitir TODAS las interacciones
            if (slot >= 0 && slot < 45) {
                // No cancelar el evento, permitir todas las acciones
                return;
            }

            // Si el click es en el inventario del jugador (slots 54+)
            // Permitir TODAS las interacciones para poder mover items al banco
            if (slot >= 54) {
                return;
            }

            // Resto de slots son botones (45-53)
            event.setCancelled(true);

            if (slot == 45) { // Página Anterior
                menu.saveBank();
                menu.open(player, menu.getCurrentPage() - 1);
            } else if (slot == 46) { // Depositar oro
                player.closeInventory();
                ClanBankMenu.PendingGoldAction action = new ClanBankMenu.PendingGoldAction(
                    menu.getClan(),
                    ClanBankMenu.PendingGoldAction.ActionType.DEPOSIT
                );
                ClanBankMenu.addPendingGoldAction(player.getUniqueId(), action);

                player.sendMessage("");
                player.sendMessage("§6§l⚔ DEPOSITAR ORO ⚔");
                player.sendMessage("");
                player.sendMessage("§7Escribe la cantidad de oro a depositar:");
                player.sendMessage("§7Escribe §f'cancelar' §7para cancelar");
                player.sendMessage("");
            } else if (slot == 48) { // Volver
                menu.saveBank();
                ClanMainMenu mainMenu = new ClanMainMenu(plugin, menu.getClan());
                mainMenu.open(player);
            } else if (slot == 50) { // Retirar oro
                player.closeInventory();
                ClanBankMenu.PendingGoldAction action = new ClanBankMenu.PendingGoldAction(
                    menu.getClan(),
                    ClanBankMenu.PendingGoldAction.ActionType.WITHDRAW
                );
                ClanBankMenu.addPendingGoldAction(player.getUniqueId(), action);

                player.sendMessage("");
                player.sendMessage("§6§l⚔ RETIRAR ORO ⚔");
                player.sendMessage("");
                player.sendMessage("§7Escribe la cantidad de oro a retirar:");
                player.sendMessage("§7Oro disponible: §6" + menu.getClan().getGold());
                player.sendMessage("§7Escribe §f'cancelar' §7para cancelar");
                player.sendMessage("");
            } else if (slot == 53) { // Página Siguiente
                menu.saveBank();
                menu.open(player, menu.getCurrentPage() + 1);
            }
        }

        // Menú de gestión
        else if (holder instanceof ClanManageMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            ClanManageMenu menu = (ClanManageMenu) holder;

            if (slot == 10) { // Expulsar miembros
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    ClanKickMemberMenu kickMenu = new ClanKickMemberMenu(plugin, menu.getClan(), player);
                    kickMenu.open(player);
                }, 1L);
            } else if (slot == 12) { // Ver logs
                player.closeInventory();
                showLogsPage(player, menu.getClan(), 0);
            } else if (slot == 14) { // Gestión de alianzas
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    ClanAlliesMenu alliesMenu = new ClanAlliesMenu(plugin, menu.getClan(), player);
                    alliesMenu.open(player);
                }, 1L);
            } else if (slot == 16) { // Gestión de guerras
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    ClanWarsMenu warsMenu = new ClanWarsMenu(plugin, menu.getClan(), player);
                    warsMenu.open(player);
                }, 1L);
            } else if (slot == 18) { // Volver
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    ClanMainMenu mainMenu = new ClanMainMenu(plugin, menu.getClan());
                    mainMenu.open(player);
                }, 1L);
            } else if (slot == 20) { // Gestión de miembros (solo líder)
                if (menu.getClan().getLeaderId().equals(player.getUniqueId())) {
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        ClanMemberManageMenu memberManageMenu = new ClanMemberManageMenu(plugin, menu.getClan(), player);
                        memberManageMenu.open(player);
                    }, 1L);
                }
            } else if (slot == 22) { // Disolver
                player.closeInventory();
                player.performCommand("clan disolver");
            } else if (slot == 24) { // Transferir liderazgo (solo líder)
                if (menu.getClan().getLeaderId().equals(player.getUniqueId())) {
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        ClanTransferLeadershipMenu transferMenu = new ClanTransferLeadershipMenu(plugin, menu.getClan(), player);
                        transferMenu.open(player);
                    }, 1L);
                }
            }
        }

        // Menú de administración
        else if (holder instanceof ClanAdminMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            ClanAdminMenu menu = (ClanAdminMenu) holder;

            if (slot == 10) { // Expulsar jugadores (admin)
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    ClanKickMemberMenu kickMenu = new ClanKickMemberMenu(plugin, menu.getClan(), player);
                    kickMenu.open(player);
                }, 1L);
            } else if (slot == 11) { // Cambiar líder (admin)
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    ClanTransferLeadershipMenu transferMenu = new ClanTransferLeadershipMenu(plugin, menu.getClan(), player);
                    transferMenu.open(player);
                }, 1L);
            } else if (slot == 13) { // Banco
                ClanBankMenu bankMenu = new ClanBankMenu(plugin, menu.getClan(), player);
                bankMenu.open(player);
            } else if (slot == 15) { // Logs
                player.closeInventory();
                try {
                    var logs = plugin.getClanManager().getLogs(menu.getClan().getId(), 20);
                    player.sendMessage("");
                    player.sendMessage("§c§l⚔ ADMIN LOGS - " + menu.getClan().getName() + " ⚔");
                    player.sendMessage("");
                    for (var log : logs) {
                        String action = log.getAction().replace("_", " ");
                        player.sendMessage("§7• §f" + log.getPlayerName() + " §7- " + action);
                    }
                    player.sendMessage("");
                } catch (Exception e) {
                    player.sendMessage("§c✗ Error al cargar logs");
                }
            } else if (slot == 16) { // Eliminar clan (admin)
                player.closeInventory();
                String clanTag = menu.getClan().getTag();
                try {
                    // Notificar a todos los miembros
                    for (UUID memberId : menu.getClan().getMembers().keySet()) {
                        Player member = Bukkit.getPlayer(memberId);
                        if (member != null && member.isOnline()) {
                            member.sendMessage("");
                            member.sendMessage("§c§l⚠ TU CLAN HA SIDO ELIMINADO POR UN ADMINISTRADOR");
                            member.sendMessage("§7El clan §f" + menu.getClan().getName() + " §7ha sido eliminado");
                            member.sendMessage("");
                        }
                    }
                    plugin.getClanManager().deleteClan(clanTag);
                    player.sendMessage("§a✓ Clan §f" + clanTag + " §aeliminado correctamente (admin)");
                } catch (Exception e) {
                    player.sendMessage("§c✗ Error al eliminar clan: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // Menú de alianzas
        else if (holder instanceof ClanAlliesMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            ClanAlliesMenu menu = (ClanAlliesMenu) holder;

            if (slot == 49) { // Volver
                ClanMainMenu mainMenu = new ClanMainMenu(plugin, menu.getClan());
                mainMenu.open(player);
            } else if (slot >= 9 && slot < 45) { // Click en un aliado
                // Verificar si el jugador tiene permisos (líder o comandante)
                Clan.ClanRank rank = menu.getClan().getMemberRank(player.getUniqueId());
                if (rank == Clan.ClanRank.LEADER || rank == Clan.ClanRank.COMMANDER) {
                    String allyTag = menu.getAllyTagAtSlot(slot);
                    if (allyTag != null) {
                        player.closeInventory();
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.performCommand("clan alianza retirar " + allyTag);
                        });
                    }
                } else {
                    player.sendMessage("§c✗ Solo el líder y comandantes pueden cancelar alianzas");
                }
            }
        }

        // Menú de guerras
        else if (holder instanceof ClanWarsMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            ClanWarsMenu menu = (ClanWarsMenu) holder;

            if (slot == 49) { // Volver
                ClanMainMenu mainMenu = new ClanMainMenu(plugin, menu.getClan());
                mainMenu.open(player);
            } else if (slot >= 9 && slot < 45) { // Click en un enemigo
                // Verificar si el jugador tiene permisos (líder o comandante)
                Clan.ClanRank rank = menu.getClan().getMemberRank(player.getUniqueId());
                if (rank == Clan.ClanRank.LEADER || rank == Clan.ClanRank.COMMANDER) {
                    String enemyTag = menu.getEnemyTagAtSlot(slot);
                    if (enemyTag != null) {
                        player.closeInventory();
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.performCommand("clan guerra paz " + enemyTag);
                        });
                    }
                } else {
                    player.sendMessage("§c✗ Solo el líder y comandantes pueden proponer paz");
                }
            }
        }

        // Menú de expulsar miembros
        else if (holder instanceof ClanKickMemberMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            ClanKickMemberMenu menu = (ClanKickMemberMenu) holder;

            if (slot == 49) { // Volver
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    ClanManageMenu manageMenu = new ClanManageMenu(plugin, menu.getClan(), player);
                    manageMenu.open(player);
                }, 1L);
            } else if (slot >= 9 && slot < 45) { // Click en un miembro para expulsar
                UUID memberToKick = menu.getMemberUuidAtSlot(slot);
                if (memberToKick != null) {
                    org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberToKick);
                    String memberName = offlinePlayer.getName() != null ? offlinePlayer.getName() : memberToKick.toString();

                    player.closeInventory();

                    // Ejecutar el comando de expulsar
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.performCommand("clan expulsar " + memberName);
                    });
                }
            }
        }

        // Menú de gestión de rangos de miembros
        else if (holder instanceof ClanMemberManageMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            ClanMemberManageMenu menu = (ClanMemberManageMenu) holder;

            if (slot == 45) { // Página anterior
                if (menu.getCurrentPage() > 0) {
                    menu.open(player, menu.getCurrentPage() - 1);
                }
            } else if (slot == 48) { // Volver
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    ClanManageMenu manageMenu = new ClanManageMenu(plugin, menu.getClan(), player);
                    manageMenu.open(player);
                }, 1L);
            } else if (slot == 53) { // Página siguiente
                menu.open(player, menu.getCurrentPage() + 1);
            } else if (slot >= 0 && slot < 45) { // Click en un miembro para gestionar rango
                UUID memberUuid = menu.getMemberUuidAtSlot(slot);
                if (memberUuid != null) {
                    Clan.ClanRank currentRank = menu.getClan().getMemberRank(memberUuid);
                    Player member = Bukkit.getPlayer(memberUuid);
                    String memberName = member != null ? member.getName() : memberUuid.toString().substring(0, 8);

                    if (event.getClick() == org.bukkit.event.inventory.ClickType.LEFT) {
                        // Promover a comandante
                        if (currentRank != Clan.ClanRank.COMMANDER) {
                            try {
                                plugin.getClanManager().updateMemberRank(menu.getClan().getId(), memberUuid, Clan.ClanRank.COMMANDER);
                                player.sendMessage("§a✓ Has promovido a §f" + memberName + " §aa comandante");

                                // Notificar al miembro si está online
                                if (member != null && member.isOnline()) {
                                    member.sendMessage("§a✓ Has sido promovido a §eComandante §ade " + menu.getClan().getName());
                                }

                                // Refrescar menú
                                menu.open(player, menu.getCurrentPage());
                            } catch (Exception e) {
                                player.sendMessage("§c✗ Error al promover al miembro");
                                e.printStackTrace();
                            }
                        }
                    } else if (event.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) {
                        // Degradar de comandante
                        if (currentRank == Clan.ClanRank.COMMANDER) {
                            try {
                                plugin.getClanManager().updateMemberRank(menu.getClan().getId(), memberUuid, Clan.ClanRank.MEMBER);
                                player.sendMessage("§c✓ Has degradado a §f" + memberName + " §ca miembro");

                                // Notificar al miembro si está online
                                if (member != null && member.isOnline()) {
                                    member.sendMessage("§c✗ Has sido degradado a §7Miembro §cde " + menu.getClan().getName());
                                }

                                // Refrescar menú
                                menu.open(player, menu.getCurrentPage());
                            } catch (Exception e) {
                                player.sendMessage("§c✗ Error al degradar al miembro");
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

        // Menú de transferir liderazgo
        else if (holder instanceof ClanTransferLeadershipMenu) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            ClanTransferLeadershipMenu menu = (ClanTransferLeadershipMenu) holder;

            if (slot == 49) { // Volver
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    ClanManageMenu manageMenu = new ClanManageMenu(plugin, menu.getClan(), player);
                    manageMenu.open(player);
                }, 1L);
            } else if (slot >= 9 && slot < 45) { // Click en un comandante para transferir
                if (event.getClick() == org.bukkit.event.inventory.ClickType.LEFT) {
                    UUID commanderUuid = menu.getCommanderUuidAtSlot(slot);
                    if (commanderUuid != null) {
                        Player commander = Bukkit.getPlayer(commanderUuid);
                        String commanderName = commander != null ? commander.getName() : commanderUuid.toString().substring(0, 8);

                        player.closeInventory();

                        // Crear transferencia pendiente
                        ClanTransferLeadershipMenu.PendingTransfer transfer = new ClanTransferLeadershipMenu.PendingTransfer(
                            menu.getClan(),
                            commanderUuid,
                            commanderName
                        );
                        ClanTransferLeadershipMenu.addPendingTransfer(player.getUniqueId(), transfer);

                        // Enviar mensaje de confirmación con botones clickeables
                        player.sendMessage("");
                        player.sendMessage("§6§l⚔ TRANSFERIR LIDERAZGO ⚔");
                        player.sendMessage("");
                        player.sendMessage("§7¿Estás seguro de entregar el clan a §f" + commanderName + "§7?");
                        player.sendMessage("");

                        // Crear componentes de texto clickeables
                        net.kyori.adventure.text.Component yesButton = net.kyori.adventure.text.Component.text("§a§l✓ SÍ")
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/clan transferir confirmar"));

                        net.kyori.adventure.text.Component noButton = net.kyori.adventure.text.Component.text("§c§l✗ NO")
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/clan transferir cancelar"));

                        net.kyori.adventure.text.Component message = net.kyori.adventure.text.Component.empty()
                            .append(yesButton)
                            .append(net.kyori.adventure.text.Component.text("     "))
                            .append(noButton);

                        player.sendMessage(message);
                        player.sendMessage("");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        InventoryHolder holder = event.getInventory().getHolder();

        // Guardar banco al cerrar y liberar el lock
        if (holder instanceof ClanBankMenu) {
            ClanBankMenu menu = (ClanBankMenu) holder;
            menu.saveBank();
            Player player = (Player) event.getPlayer();
            ClanBankMenu.releaseBankLock(menu.getClan().getId(), player.getUniqueId());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        InventoryHolder holder = event.getInventory().getHolder();

        // Permitir arrastrar items en el banco
        if (holder instanceof ClanBankMenu) {
            // Verificar que ningún slot arrastrado sea en los botones (45-53)
            for (int slot : event.getRawSlots()) {
                if (slot >= 45 && slot < 54) {
                    // Arrastrar sobre los botones - cancelar
                    event.setCancelled(true);
                    return;
                }
            }

            // Permitir arrastrar en banco (0-44) y desde inventario del jugador (54+)
            // No cancelar el evento
        }
    }

    private void showLogsPage(Player player, Clan clan, int page) {
        try {
            int perPage = 10;
            int totalLogs = plugin.getClanManager().getLogsCount(clan.getId());
            int totalPages = (int) Math.ceil((double) totalLogs / perPage);

            if (totalPages == 0) totalPages = 1;

            var logs = plugin.getClanManager().getLogsPaginated(clan.getId(), page, perPage);

            player.sendMessage("");
            player.sendMessage("§6§l⚔ LOGS DEL CLAN ⚔ §7(Página " + (page + 1) + "/" + totalPages + ")");
            player.sendMessage("");

            if (logs.isEmpty()) {
                player.sendMessage("§7No hay logs registrados");
            } else {
                for (var log : logs) {
                    String action = log.getAction().replace("_", " ");
                    // Formato de fecha
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM HH:mm");
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
            net.kyori.adventure.text.Component navigation = net.kyori.adventure.text.Component.empty();

            if (page > 0) {
                navigation = navigation.append(
                    net.kyori.adventure.text.Component.text("§a§l« Anterior ")
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/clan logs " + clan.getTag() + " " + (page - 1)))
                );
            }

            navigation = navigation.append(
                net.kyori.adventure.text.Component.text("§7[" + (page + 1) + "/" + totalPages + "]")
            );

            if (page < totalPages - 1) {
                navigation = navigation.append(
                    net.kyori.adventure.text.Component.text(" §a§lSiguiente »")
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/clan logs " + clan.getTag() + " " + (page + 1)))
                );
            }

            player.sendMessage(navigation);
            player.sendMessage("");

        } catch (Exception e) {
            player.sendMessage("§c✗ Error al cargar logs");
            e.printStackTrace();
        }
    }
}
