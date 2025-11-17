package com.flowey258.bnotev2.listeners;

import com.flowey258.bnotev2.BnoteV2;
import com.flowey258.bnotev2.managers.BanknoteManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BanknoteListener implements Listener {

    private final BnoteV2 plugin;
    private final BanknoteManager manager;
    private final Economy economy;

    public BanknoteListener(BnoteV2 plugin) {
        this.plugin = plugin;
        this.manager = plugin.getBanknoteManager();
        this.economy = plugin.getEconomy();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!event.getAction().isRightClick()) {
            return;
        }

        if (!plugin.getConfig().getBoolean("settings.allow-right-click-to-deposit", true)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!manager.isBanknote(item)) {
            return;
        }

        event.setCancelled(true);

        // Check if shift-clicking to deposit all
        if (player.isSneaking() && plugin.getConfig().getBoolean("settings.allow-shift-click-deposit-all", true)) {
            redeemAllBanknotes(player);
            return;
        }

        // Redeem single banknote
        redeemBanknote(player, item);
    }

    private void redeemBanknote(Player player, ItemStack item) {
        double amount = manager.getBanknoteAmount(item);

        if (amount <= 0) {
            player.sendMessage(manager.colorize(manager.getMessage("invalid-note")));
            item.setAmount(0);
            manager.playSound(player, "error");
            return;
        }

        economy.depositPlayer(player, amount);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        player.sendMessage(manager.colorize(manager.getMessage("note-redeemed", "[money]", manager.formatMoney(amount))));
        manager.playSound(player, "redeem");

        // Log to CoreProtect
        if (plugin.getCoreProtectIntegration() != null) {
            plugin.getCoreProtectIntegration().logRedemption(player, amount);
        }
    }

    private void redeemAllBanknotes(Player player) {
        List<ItemStack> banknotes = new ArrayList<>();
        double totalAmount = 0.0;
        int count = 0;

        // Find all banknotes in inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (manager.isBanknote(item)) {
                double amount = manager.getBanknoteAmount(item);
                if (amount > 0) {
                    totalAmount += amount * item.getAmount();
                    count += item.getAmount();
                    banknotes.add(item);
                }
            }
        }

        if (count == 0) {
            player.sendMessage(manager.colorize(manager.getMessage("nothing-in-hand")));
            manager.playSound(player, "error");
            return;
        }

        // Remove all banknotes
        for (ItemStack note : banknotes) {
            note.setAmount(0);
        }

        // Deposit money
        economy.depositPlayer(player, totalAmount);

        if (count == 1) {
            player.sendMessage(manager.colorize(manager.getMessage("note-redeemed", "[money]", manager.formatMoney(totalAmount))));
        } else {
            player.sendMessage(manager.colorize(manager.getMessage("notes-redeemed-multiple",
                    "[count]", String.valueOf(count),
                    "[money]", manager.formatMoney(totalAmount))));
        }

        manager.playSound(player, "redeem");

        // Log to CoreProtect
        if (plugin.getCoreProtectIntegration() != null) {
            plugin.getCoreProtectIntegration().logRedemption(player, totalAmount);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCraft(PrepareItemCraftEvent event) {
        if (!plugin.getConfig().getBoolean("settings.prevent-crafting", true)) {
            return;
        }

        // Check if any ingredient is a banknote
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (manager.isBanknote(item)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("settings.keep-on-death", false)) {
            return;
        }

        Player player = event.getPlayer();
        List<ItemStack> banknotes = new ArrayList<>();

        // Find all banknotes in drops
        event.getDrops().removeIf(item -> {
            if (manager.isBanknote(item)) {
                banknotes.add(item);
                return true;
            }
            return false;
        });

        // Return banknotes to player on respawn
        if (!banknotes.isEmpty()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (ItemStack note : banknotes) {
                    player.getInventory().addItem(note);
                }
            }, 1L);
        }
    }
}