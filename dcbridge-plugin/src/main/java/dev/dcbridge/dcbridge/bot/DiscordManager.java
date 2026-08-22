package dev.dcbridge.dcbridge.bot;

import dev.dcbridge.dcbridge.admin.SetupListener;
import dev.dcbridge.dcbridge.config.BotConfig;
import dev.dcbridge.dcbridge.whitelist.WhitelistManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;

public class DiscordManager {
    private final JavaPlugin plugin;
    private final BotConfig config;
    private final WhitelistManager whitelistManager;
    private JDA jda;
    private SetupListener setupListener;

    public DiscordManager(JavaPlugin plugin, BotConfig config, WhitelistManager whitelistManager) {
        this.plugin = plugin;
        this.config = config;
        this.whitelistManager = whitelistManager;
        this.setupListener = new SetupListener(plugin, config);
    }

    public SetupListener getSetupListener() {
        return setupListener;
    }

    public void start() {
        if (config.getDiscordToken().isBlank()) {
            plugin.getLogger().warning("Discord token is empty. Bot will not start.");
            return;
        }
        try {
            JDABuilder builder = JDABuilder.createDefault(
                    config.getDiscordToken(),
                    EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
            );
            builder.addEventListeners(new DiscordListener(plugin, config, whitelistManager, this));
            builder.addEventListeners(setupListener);
            jda = builder.build();
            DiscordManagerHolder.setJda(jda);
            // Build() is non-blocking — the bot connects in the background and logs its
            // tag in the ReadyEvent. We must never block the server's main thread here:
            // with a bad token the login would never complete and the server would hang.
            plugin.getLogger().info("Discord bot is starting in the background...");
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to start Discord bot: " + ex.getMessage());
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdownNow();
        }
        DiscordManagerHolder.setJda(null);
    }

    public JDA getJda() {
        return jda;
    }
}
