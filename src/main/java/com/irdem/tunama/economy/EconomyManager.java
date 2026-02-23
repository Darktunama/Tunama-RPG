package com.irdem.tunama.economy;

import com.irdem.tunama.TunamaRPG;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Gestor de economía usando Vault API para integración con EssentialsX
 */
public class EconomyManager {

    private final TunamaRPG plugin;
    private Economy economy = null;
    private boolean enabled = false;

    public EconomyManager(TunamaRPG plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicializa la integración con Vault
     * @return true si se configuró correctamente, false si Vault no está disponible
     */
    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault no encontrado. Sistema de economía desactivado.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp =
            plugin.getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            plugin.getLogger().warning("No se encontró proveedor de economía. Sistema de economía desactivado.");
            return false;
        }

        economy = rsp.getProvider();
        enabled = true;

        String providerName = economy.getName();
        plugin.getLogger().info("Sistema de economía activado usando: " + providerName);

        return true;
    }

    /**
     * Retira dinero del jugador
     * @param player Jugador
     * @param amount Cantidad a retirar
     * @return true si la transacción fue exitosa
     */
    public boolean withdraw(Player player, double amount) {
        if (!enabled || economy == null) {
            plugin.getLogger().warning("Intento de retiro sin economía activa");
            return false;
        }

        if (amount <= 0) {
            return false;
        }

        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /**
     * Deposita dinero al jugador
     * @param player Jugador
     * @param amount Cantidad a depositar
     * @return true si la transacción fue exitosa
     */
    public boolean deposit(Player player, double amount) {
        if (!enabled || economy == null) {
            plugin.getLogger().warning("Intento de depósito sin economía activa");
            return false;
        }

        if (amount <= 0) {
            return false;
        }

        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    /**
     * Obtiene el balance del jugador
     * @param player Jugador
     * @return Balance del jugador
     */
    public double getBalance(Player player) {
        if (!enabled || economy == null) {
            return 0;
        }

        return economy.getBalance(player);
    }

    /**
     * Verifica si el jugador tiene suficiente dinero
     * @param player Jugador
     * @param amount Cantidad a verificar
     * @return true si el jugador tiene al menos esa cantidad
     */
    public boolean has(Player player, double amount) {
        if (!enabled || economy == null) {
            return false;
        }

        return economy.has(player, amount);
    }

    /**
     * Verifica si el sistema de economía está habilitado
     * @return true si está habilitado
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Formatea una cantidad a string con el formato de la economía
     * @param amount Cantidad
     * @return String formateado
     */
    public String format(double amount) {
        if (!enabled || economy == null) {
            return String.format("%.2f", amount);
        }

        return economy.format(amount);
    }
}
