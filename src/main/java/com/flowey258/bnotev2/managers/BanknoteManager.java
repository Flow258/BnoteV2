package com.flowey258.bnotev2.managers;

import com.flowey258.bnotev2.BnoteV2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BanknoteManager {

    private final BnoteV2 plugin;
    private final NamespacedKey amountKey;
    private final NamespacedKey creatorKey;
    private final NamespacedKey dateKey;
    private final DecimalFormat decimalFormat;

    public BanknoteManager(BnoteV2 plugin) {
        this.plugin = plugin;
        this.amountKey = new NamespacedKey(plugin, "banknote_amount");
        this.creatorKey = new NamespacedKey(plugin, "banknote_creator");
        this.dateKey = new NamespacedKey(plugin, "banknote_date");

        int minDecimals = plugin.getConfig().getInt("settings.minimum-float-amount", 0);
        int maxDecimals = plugin.getConfig().getInt("settings.maximum-float-amount", 2);

        StringBuilder pattern = new StringBuilder("#,##0");
        if (maxDecimals > 0) {
            pattern.append(".");
            pattern.append("0".repeat(minDecimals));
            if (maxDecimals > minDecimals) {
                pattern.append("#".repeat(maxDecimals - minDecimals));
            }
        }
        this.decimalFormat = new DecimalFormat(pattern.toString());
    }

    public ItemStack createBanknote(double amount, String creator) {
        // Check if we should use generic creator for stacking
        boolean stackSameValue = plugin.getConfig().getBoolean("note.stack-same-value", true);
        String actualCreator = stackSameValue ? "Server" : creator;

        // Get material from config
        String materialName = plugin.getConfig().getString("note.material", "PAPER");
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.PAPER;
            plugin.getLogger().warning("Invalid material '" + materialName + "', using PAPER");
        }

        ItemStack note = new ItemStack(material, 1);
        ItemMeta meta = note.getItemMeta();

        // Set display name (also disable italic)
        String name = plugin.getConfig().getString("note.name", "&7&l[&eBANKNOTE&7&l]");
        Component nameComponent = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(name)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
        meta.displayName(nameComponent);

        // Set lore
        List<String> loreTemplate = plugin.getConfig().getStringList("note.lore");
        List<Component> lore = new ArrayList<>();

        String formattedAmount = decimalFormat.format(amount);
        String dateFormat = plugin.getConfig().getString("settings.date-format", "MMM dd, yyyy");
        String date = new SimpleDateFormat(dateFormat).format(new Date());

        for (String line : loreTemplate) {
            line = line.replace("[money]", formattedAmount)
                    .replace("[creator]", actualCreator)
                    .replace("[date]", date);

            // Create component with italic set to false
            Component lineComponent = LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(line)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);

            lore.add(lineComponent);
        }
        meta.lore(lore);

        // Set custom model data
        int customModelData = plugin.getConfig().getInt("note.custom-model-data", 0);
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }

        // Set glowing effect
        if (plugin.getConfig().getBoolean("note.glowing", true)) {
            meta.setEnchantmentGlintOverride(true);
        }

        // Hide all flags
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        // Store data in PDC
        meta.getPersistentDataContainer().set(amountKey, PersistentDataType.DOUBLE, amount);
        meta.getPersistentDataContainer().set(creatorKey, PersistentDataType.STRING, actualCreator);
        meta.getPersistentDataContainer().set(dateKey, PersistentDataType.STRING, date);

        note.setItemMeta(meta);
        return note;
    }

    public boolean canStack(ItemStack item1, ItemStack item2) {
        if (!isBanknote(item1) || !isBanknote(item2)) {
            return false;
        }

        // Check if stacking is enabled
        if (!plugin.getConfig().getBoolean("note.stack-same-value", true)) {
            return false;
        }

        // Check if amounts are the same
        double amount1 = getBanknoteAmount(item1);
        double amount2 = getBanknoteAmount(item2);

        return Math.abs(amount1 - amount2) < 0.01; // Allow small floating point differences
    }

    public boolean isBanknote(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(amountKey, PersistentDataType.DOUBLE);
    }

    public double getBanknoteAmount(ItemStack item) {
        if (!isBanknote(item)) {
            return 0.0;
        }
        Double amount = item.getItemMeta().getPersistentDataContainer().get(amountKey, PersistentDataType.DOUBLE);
        return amount != null ? amount : 0.0;
    }

    public String getBanknoteCreator(ItemStack item) {
        if (!isBanknote(item)) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(creatorKey, PersistentDataType.STRING);
    }

    public String formatMoney(double amount) {
        return decimalFormat.format(amount);
    }

    public void playSound(Player player, String soundKey) {
        String soundName = plugin.getConfig().getString("settings.sounds." + soundKey, "");
        if (soundName.isEmpty()) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + soundName);
        }
    }

    public Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public Component colorizePAPI(String text, Player player) {
        if (plugin.isPlaceholderAPIEnabled() && player != null) {
            try {
                Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                java.lang.reflect.Method setPlaceholders = papiClass.getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class);
                text = (String) setPlaceholders.invoke(null, player, text);
            } catch (Exception e) {
                // PlaceholderAPI not available or error, continue without it
            }
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public String getMessage(String path) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String message = plugin.getConfig().getString("messages." + path, "&cMessage not found: " + path);
        return prefix + message;
    }

    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return message;
    }
}