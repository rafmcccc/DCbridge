package dev.dcbridge.dcbridge;

import dev.dcbridge.dcbridge.admin.MessageToolsListener;
import dev.dcbridge.dcbridge.bot.DiscordManager;
import dev.dcbridge.dcbridge.checkhacks.CheckHacksListener;
import dev.dcbridge.dcbridge.bot.Presence;
import dev.dcbridge.dcbridge.config.BotConfig;
import dev.dcbridge.dcbridge.stats.StatsCommand;
import dev.dcbridge.dcbridge.stats.StatsUpdater;
import dev.dcbridge.dcbridge.whitelist.WhitelistListener;
import dev.dcbridge.dcbridge.whitelist.WhitelistManager;
import dev.dcbridge.dcbridge.whitelist.WhitelistStore;
import org.bukkit.plugin.java.JavaPlugin;

public class DCbridgePlugin extends JavaPlugin {
    private BotConfig botConfig;
    private WhitelistStore whitelistStore;
    private WhitelistManager whitelistManager;
    private StatsUpdater statsUpdater;
    private DiscordManager discordManager;
    private Presence presence;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        this.botConfig = new BotConfig(this);
        this.whitelistStore = new WhitelistStore(this, botConfig);
        this.whitelistManager = new WhitelistManager(this, botConfig, whitelistStore);
        // StatsUpdater must be created before DiscordManager so the cached ping is available
        this.statsUpdater = new StatsUpdater(this, botConfig);
        this.discordManager = new DiscordManager(this, botConfig, whitelistManager, statsUpdater);
        this.presence = new Presence(this, botConfig);

        getServer().getPluginManager().registerEvents(new WhitelistListener(this, botConfig, whitelistManager), this);
        getServer().getPluginManager().registerEvents(new CheckHacksListener(this, botConfig), this);

        getCommand("whitelist-setup").setExecutor(new dev.dcbridge.dcbridge.admin.WhitelistSetupCmd(this, botConfig, discordManager));
        getCommand("status").setExecutor(new StatsCommand(this, botConfig, statsUpdater));
        getCommand("wl-remove").setExecutor(new MessageToolsListener(this, botConfig, whitelistManager));

        discordManager.start();
        statsUpdater.start();
        presence.start();

        getLogger().info("DCbridge plugin enabled. Discord bot starting...");
    }

    @Override
    public void onDisable() {
        if (presence != null) {
            presence.stop();
        }
        if (statsUpdater != null) {
            statsUpdater.stop();
        }
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
