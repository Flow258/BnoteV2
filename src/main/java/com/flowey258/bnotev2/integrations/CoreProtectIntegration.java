package com.flowey258.bnotev2.integrations;

import com.flowey258.bnotev2.BnoteV2;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class CoreProtectIntegration {

    private final BnoteV2 plugin;
    private Object coreProtectAPI;
    private Method logContainerMethod;
    private final boolean logWithdrawals;
    private final boolean logRedemptions;
    private final boolean logGives;

    public CoreProtectIntegration(BnoteV2 plugin) {
        this.plugin = plugin;

        this.logWithdrawals = plugin.getConfig().getBoolean("integrations.coreprotect.log-withdrawals", true);
        this.logRedemptions = plugin.getConfig().getBoolean("integrations.coreprotect.log-redemptions", true);
        this.logGives = plugin.getConfig().getBoolean("integrations.coreprotect.log-gives", true);

        // Try to hook into CoreProtect using reflection
        try {
            Plugin coreProtectPlugin = plugin.getServer().getPluginManager().getPlugin("CoreProtect");
            if (coreProtectPlugin != null && coreProtectPlugin.isEnabled()) {
                // Get the API class
                Class<?> coreProtectClass = Class.forName("net.coreprotect.CoreProtect");

                // Get the API instance
                Method getAPIMethod = coreProtectClass.getMethod("getInstance");
                Object coreProtectInstance = getAPIMethod.invoke(null);

                // Get the CoreProtectAPI
                Method getCoreProtectAPI = coreProtectInstance.getClass().getMethod("getAPI");
                this.coreProtectAPI = getCoreProtectAPI.invoke(coreProtectInstance);

                // Check if API is enabled
                Method isEnabledMethod = coreProtectAPI.getClass().getMethod("isEnabled");
                boolean apiEnabled = (boolean) isEnabledMethod.invoke(coreProtectAPI);

                if (!apiEnabled) {
                    this.coreProtectAPI = null;
                    plugin.getLogger().warning("CoreProtect API is not enabled");
                    return;
                }

                // Get the logContainerTransaction method
                // public boolean logContainerTransaction(String user, Location location)
                this.logContainerMethod = coreProtectAPI.getClass().getMethod(
                        "logContainerTransaction",
                        String.class,
                        Location.class
                );

                plugin.getLogger().info("Successfully hooked into CoreProtect API");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hook into CoreProtect: " + e.getMessage());
            this.coreProtectAPI = null;
        }
    }

    public void logWithdrawal(Player player, double amount) {
        if (!logWithdrawals || coreProtectAPI == null) {
            return;
        }
        logTransaction(player, "Withdrew $" + amount + " as banknote");
    }

    public void logRedemption(Player player, double amount) {
        if (!logRedemptions || coreProtectAPI == null) {
            return;
        }
        logTransaction(player, "Redeemed banknote for $" + amount);
    }

    public void logGive(Player giver, Player receiver, double amount) {
        if (!logGives || coreProtectAPI == null) {
            return;
        }
        logTransaction(giver, "Gave $" + amount + " banknote to " + receiver.getName());
        logTransaction(receiver, "Received $" + amount + " banknote from " + giver.getName());
    }

    private void logTransaction(Player player, String action) {
        if (coreProtectAPI == null || logContainerMethod == null) {
            return;
        }

        try {
            // Use CoreProtect's API to log the transaction
            String user = "#" + player.getName(); // Prefix with # to indicate custom action
            Location location = player.getLocation();

            // Log the transaction
            logContainerMethod.invoke(coreProtectAPI, user, location);

            // Additional custom logging could be added here if needed
            plugin.getLogger().fine("CoreProtect: Logged " + action + " for " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to log transaction to CoreProtect: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return coreProtectAPI != null;
    }
}