package com.flowey258.bnotev2.integrations;

import com.flowey258.bnotev2.BnoteV2;
import com.flowey258.bnotev2.managers.BanknoteManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIIntegration extends PlaceholderExpansion {

    private final BnoteV2 plugin;
    private final BanknoteManager manager;

    public PlaceholderAPIIntegration(BnoteV2 plugin) {
        this.plugin = plugin;
        this.manager = plugin.getBanknoteManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "bnotev2";
    }

    @Override
    public @NotNull String getAuthor() {
        return "flowey258";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) {
            return "";
        }

        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null) {
            return "";
        }

        return switch (params.toLowerCase()) {
            case "balance" -> manager.formatMoney(plugin.getEconomy().getBalance(onlinePlayer));
            case "notes_held" -> String.valueOf(countBanknotesInInventory(onlinePlayer));
            case "notes_value" -> manager.formatMoney(calculateTotalBanknoteValue(onlinePlayer));
            case "notes_value_raw" -> String.valueOf(calculateTotalBanknoteValue(onlinePlayer));
            case "balance_raw" -> String.valueOf(plugin.getEconomy().getBalance(onlinePlayer));
            case "formatted_balance" -> "$" + manager.formatMoney(plugin.getEconomy().getBalance(onlinePlayer));
            case "formatted_notes_value" ->
                    "$" + manager.formatMoney(calculateTotalBanknoteValue(onlinePlayer));
            default -> null;
        };
    }

    private int countBanknotesInInventory(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (manager.isBanknote(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private double calculateTotalBanknoteValue(Player player) {
        double total = 0.0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (manager.isBanknote(item)) {
                total += manager.getBanknoteAmount(item) * item.getAmount();
            }
        }
        return total;
    }
}