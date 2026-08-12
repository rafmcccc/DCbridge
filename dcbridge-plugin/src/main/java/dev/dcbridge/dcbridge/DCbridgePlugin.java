package dev.dcbridge.dcbridge;

import dev.dcbridge.dcbridge.admin.MessageToolsListener;
import dev.dcbridge.dcbridge.bot.DiscordManager;
import dev.dcbridge.dcbridge.config.BotConfig;
import dev.dcbridge.dcbridge.whitelist.WhitelistListener;
import dev.dcbridge.dcbridge.whitelist.WhitelistManager;
import dev.dcbridge.dcbridge.whitelist.WhitelistStore;
import org.bukkit.plugin.java.JavaPlugin;

public class DCbridgePlugin extends JavaPlugin {
    private BotConfig botConfig;
    private WhitelistStore whitelistStore;
    private WhitelistManager whitelistManager;
    private DiscordManager discordManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        this.botConfig = new BotConfig(this);
        this.whitelistStore = new WhitelistStore(this, botConfig);
        this.whitelistManager = new WhitelistManager(this, botConfig, whitelistStore);
        this.discordManager = new DiscordManager(this, botConfig, whitelistManager);

        getServer().getPluginManager().registerEvents(new WhitelistListener(this, botConfig, whitelistManager), this);

        getCommand("dcbridge-setup").setExecutor(new dev.dcbridge.dcbridge.admin.WhitelistSetupCmd(this, botConfig, discordManager));
        getCommand("wl-remove").setExecutor(new MessageToolsListener(this, botConfig, whitelistManager));

        discordManager.start();

        getLogger().info("DCbridge plugin enabled. Discord bot starting...");
    }

    @Override
    public void onDisable() {
        if (discordManager != null) {
            discordManager.shutdown();
        }
        if (whitelistStore != null) {
            whitelistStore.close();
        }
        getLogger().info("DCbridge plugin disabled.");
    }

    public BotConfig getBotConfig() {
        return botConfig;
    }

    public WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }
}
