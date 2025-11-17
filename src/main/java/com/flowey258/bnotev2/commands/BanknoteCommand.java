package com.flowey258.bnotev2.commands;

import com.flowey258.bnotev2.BnoteV2;
import com.flowey258.bnotev2.managers.BanknoteManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BanknoteCommand implements CommandExecutor, TabCompleter {

    private final BnoteV2 plugin;
    private final BanknoteManager manager;
    private final Economy economy;

    public BanknoteCommand(BnoteV2 plugin) {
        this.plugin = plugin;
        this.manager = plugin.getBanknoteManager();
        this.economy = plugin.getEconomy();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(manager.colorize(manager.getMessage("usage.main")));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "withdraw", "w" -> {
                return handleWithdraw(sender, args);
            }
            case "give", "g" -> {
                return handleGive(sender, args);
            }
            case "reload", "rl" -> {
                return handleReload(sender);
            }
            default -> {
                sender.sendMessage(manager.colorize(manager.getMessage("usage.main")));
                return true;
            }
        }
    }

    private boolean handleWithdraw(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(manager.colorize("&cOnly players can withdraw banknotes!"));
            return true;
        }

        if (!player.hasPermission("bnotev2.withdraw")) {
            player.sendMessage(manager.colorize(manager.getMessage("insufficient-permissions")));
            manager.playSound(player, "error");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(manager.colorize(manager.getMessage("usage.withdraw")));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(manager.colorize(manager.getMessage("invalid-number")));
            manager.playSound(player, "error");
            return true;
        }

        double minAmount = plugin.getConfig().getDouble("settings.minimum-withdraw-amount", 10.0);
        double maxAmount = plugin.getConfig().getDouble("settings.maximum-withdraw-amount", 9999999999999999999.0);

        if (amount < minAmount) {
            player.sendMessage(manager.colorize(manager.getMessage("less-than-minimum", "[money]", manager.formatMoney(minAmount))));
            manager.playSound(player, "error");
            return true;
        }

        if (amount > maxAmount) {
            player.sendMessage(manager.colorize(manager.getMessage("more-than-maximum", "[money]", manager.formatMoney(maxAmount))));
            manager.playSound(player, "error");
            return true;
        }

        if (economy.getBalance(player) < amount) {
            player.sendMessage(manager.colorize(manager.getMessage("insufficient-funds", "[money]", manager.formatMoney(amount))));
            manager.playSound(player, "error");
            return true;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(manager.colorize(manager.getMessage("inventory-full")));
            manager.playSound(player, "error");
            return true;
        }

        economy.withdrawPlayer(player, amount);
        ItemStack note = manager.createBanknote(amount, player.getName());

        // Try to stack with existing notes
        boolean added = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && manager.canStack(item, note)) {
                int newAmount = item.getAmount() + 1;
                if (newAmount <= item.getMaxStackSize()) {
                    item.setAmount(newAmount);
                    added = true;
                    break;
                }
            }
        }

        // If couldn't stack, add as new item
        if (!added) {
            player.getInventory().addItem(note);
        }

        player.sendMessage(manager.colorize(manager.getMessage("note-created", "[money]", manager.formatMoney(amount))));
        manager.playSound(player, "create");

        // Log to CoreProtect
        if (plugin.getCoreProtectIntegration() != null) {
            plugin.getCoreProtectIntegration().logWithdrawal(player, amount);
        }

        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bnotev2.give")) {
            sender.sendMessage(manager.colorize(manager.getMessage("insufficient-permissions")));
            if (sender instanceof Player player) {
                manager.playSound(player, "error");
            }
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(manager.colorize(manager.getMessage("usage.give")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(manager.colorize(manager.getMessage("target-not-found", "[player]", args[1])));
            if (sender instanceof Player player) {
                manager.playSound(player, "error");
            }
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(manager.colorize(manager.getMessage("invalid-number")));
            if (sender instanceof Player player) {
                manager.playSound(player, "error");
            }
            return true;
        }

        double minAmount = plugin.getConfig().getDouble("settings.minimum-withdraw-amount", 10.0);
        double maxAmount = plugin.getConfig().getDouble("settings.maximum-withdraw-amount", 9999999999999999999.0);

        if (amount < minAmount || amount > maxAmount) {
            sender.sendMessage(manager.colorize("&cAmount must be between $" + manager.formatMoney(minAmount) + " and $" + manager.formatMoney(maxAmount)));
            if (sender instanceof Player player) {
                manager.playSound(player, "error");
            }
            return true;
        }

        if (target.getInventory().firstEmpty() == -1) {
            sender.sendMessage(manager.colorize(manager.getMessage("inventory-full")));
            if (sender instanceof Player player) {
                manager.playSound(player, "error");
            }
            return true;
        }

        String creatorName = sender instanceof Player ? sender.getName() :
                plugin.getConfig().getString("settings.console-name", "&cServer");

        ItemStack note = manager.createBanknote(amount, creatorName);

        // Try to stack with existing notes
        boolean added = false;
        for (ItemStack item : target.getInventory().getContents()) {
            if (item != null && manager.canStack(item, note)) {
                int newAmount = item.getAmount() + 1;
                if (newAmount <= item.getMaxStackSize()) {
                    item.setAmount(newAmount);
                    added = true;
                    break;
                }
            }
        }

        // If couldn't stack, add as new item
        if (!added) {
            if (target.getInventory().firstEmpty() == -1) {
                sender.sendMessage(manager.colorize(manager.getMessage("inventory-full")));
                if (sender instanceof Player player) {
                    manager.playSound(player, "error");
                }
                return true;
            }
            target.getInventory().addItem(note);
        }

        sender.sendMessage(manager.colorize(manager.getMessage("note-given", "[money]", manager.formatMoney(amount), "[player]", target.getName())));
        target.sendMessage(manager.colorize(manager.getMessage("note-received", "[money]", manager.formatMoney(amount), "[player]", creatorName)));

        manager.playSound(target, "create");
        if (sender instanceof Player player) {
            manager.playSound(player, "create");
        }

        // Log to CoreProtect
        if (plugin.getCoreProtectIntegration() != null && sender instanceof Player giver) {
            plugin.getCoreProtectIntegration().logGive(giver, target, amount);
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("bnotev2.reload")) {
            sender.sendMessage(manager.colorize(manager.getMessage("insufficient-permissions")));
            if (sender instanceof Player player) {
                manager.playSound(player, "error");
            }
            return true;
        }

        plugin.reloadConfiguration();
        sender.sendMessage(manager.colorize(manager.getMessage("reloaded")));

        if (sender instanceof Player player) {
            manager.playSound(player, "redeem");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            if (sender.hasPermission("bnotev2.withdraw")) subcommands.add("withdraw");
            if (sender.hasPermission("bnotev2.give")) subcommands.add("give");
            if (sender.hasPermission("bnotev2.reload")) subcommands.add("reload");

            return subcommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("withdraw")) {
                return Arrays.asList("10", "100", "1000", "10000");
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Arrays.asList("10", "100", "1000", "10000");
        }

        return completions;
    }
}