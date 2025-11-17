package com.flowey258.bnotev2;

import com.flowey258.bnotev2.commands.BanknoteCommand;
import com.flowey258.bnotev2.integrations.CoreProtectIntegration;
import com.flowey258.bnotev2.integrations.PlaceholderAPIIntegration;
import com.flowey258.bnotev2.listeners.BanknoteListener;
import com.flowey258.bnotev2.managers.BanknoteManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class BnoteV2 extends JavaPlugin {

    private static BnoteV2 instance;
    private Economy economy;
    private BanknoteManager banknoteManager;
    private CoreProtectIntegration coreProtectIntegration;
    private boolean placeholderAPIEnabled = false;

    @Override
    public void onEnable() {
        instance = this;

        // ASCII art banner
        getLogger().info("╔═══════════════════════════════════╗");
        getLogger().info("║        BnoteV2 by flowey258       ║");
        getLogger().info("║          Version 2.0.0            ║");
        getLogger().info("╚═══════════════════════════════════╝");

        // Save default config
        saveDefaultConfig();

        // Setup Vault economy
        if (!setupEconomy()) {
            getLogger().severe("╔═══════════════════════════════════╗");
            getLogger().severe("║  Vault or economy plugin missing! ║");
            getLogger().severe("║      Disabling BnoteV2...         ║");
            getLogger().severe("╚═══════════════════════════════════╝");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        banknoteManager = new BanknoteManager(this);

        // Setup integrations
        setupIntegrations();

        // Register commands
        BanknoteCommand banknoteCommand = new BanknoteCommand(this);
        getCommand("banknote").setExecutor(banknoteCommand);
        getCommand("banknote").setTabCompleter(banknoteCommand);

        // Register listeners
        getServer().getPluginManager().registerEvents(new BanknoteListener(this), this);

        // Success message with integration status
        getLogger().info("╔═══════════════════════════════════╗");
        getLogger().info("║    BnoteV2 Enabled Successfully   ║");
        getLogger().info("╠═══════════════════════════════════╣");
        getLogger().info("║ Economy: " + economy.getName());
        getLogger().info("║ PlaceholderAPI: " + (placeholderAPIEnabled ? "✓" : "✗"));
        getLogger().info("║ CoreProtect: " + (coreProtectIntegration != null ? "✓" : "✗"));
        getLogger().info("╚═══════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        getLogger().info("╔═══════════════════════════════════╗");
        getLogger().info("║       BnoteV2 Disabled            ║");
        getLogger().info("║   Thanks for using BnoteV2!       ║");
        getLogger().info("╚═══════════════════════════════════╝");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void setupIntegrations() {
        // PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            if (getConfig().getBoolean("integrations.placeholderapi.enabled", true)) {
                new PlaceholderAPIIntegration(this).register();
                placeholderAPIEnabled = true;
                getLogger().info("✓ PlaceholderAPI integration enabled");
            }
        }

        // CoreProtect
        if (getServer().getPluginManager().getPlugin("CoreProtect") != null) {
            if (getConfig().getBoolean("integrations.coreprotect.enabled", true)) {
                coreProtectIntegration = new CoreProtectIntegration(this);
                getLogger().info("✓ CoreProtect integration enabled");
            }
        }

        // GriefPrevention check
        if (getServer().getPluginManager().getPlugin("GriefPrevention") != null) {
            if (getConfig().getBoolean("settings.griefprevention-protection", true)) {
                getLogger().info("✓ GriefPrevention integration enabled");
            }
        }

        // PvPManager check
        if (getServer().getPluginManager().getPlugin("PvPManager") != null) {
            if (getConfig().getBoolean("settings.combat-restriction", false)) {
                getLogger().info("✓ PvPManager integration enabled");
            }
        }
    }

    public static BnoteV2 getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public BanknoteManager getBanknoteManager() {
        return banknoteManager;
    }

    public CoreProtectIntegration getCoreProtectIntegration() {
        return coreProtectIntegration;
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }

    public void reloadConfiguration() {
        reloadConfig();
        banknoteManager = new BanknoteManager(this);
        getLogger().info("Configuration reloaded successfully!");
    }
}