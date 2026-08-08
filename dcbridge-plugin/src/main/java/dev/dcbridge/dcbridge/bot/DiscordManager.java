package dev.dcbridge.dcbridge.bot;

import dev.dcbridge.dcbridge.config.BotConfig;
import dev.dcbridge.dcbridge.stats.StatsUpdater;
import dev.dcbridge.dcbridge.whitelist.WhitelistManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;

public class DiscordManager {
    private final JavaPlugin plugin;
    private final BotConfig config;
    private final WhitelistManager whitelistManager;
    private final StatsUpdater statsUpdater;
    private JDA jda;

    public DiscordManager(JavaPlugin plugin, BotConfig config, WhitelistManager whitelistManager, StatsUpdater statsUpdater) {
        this.plugin = plugin;
        this.config = config;
        this.whitelistManager = whitelistManager;
        this.statsUpdater = statsUpdater;
    }

    public void start() {
        if (config.getDiscordToken().isBlank()) {
            plugin.getLogger().warning("Discord token is empty. Bot will not start.");
            return;
        }
        try {
            JDABuilder builder = JDABuilder.createDefault(
                    config.getDiscordToken(),
                    EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
            );
            builder.setMemberCachePolicy(MemberCachePolicy.ALL);
            builder.addEventListeners(new DiscordListener(plugin, config, whitelistManager, this));
            jda = builder.build();
            DiscordManagerHolder.setJda(jda);
            jda.awaitReady();
            plugin.getLogger().info("Discord bot connected as " + jda.getSelfUser().getAsTag());
            if (!config.getGuildId().isBlank()) {
                Guild guild = jda.getGuildById(config.getGuildId());
                if (guild != null) {
                    plugin.getLogger().info("Connected to guild " + guild.getName());
                }
            }
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

    /** Returns the last cached ping from StatsUpdater (measured async, never blocks). */
    public int getLastPingMs() {
        return statsUpdater.getLastPingMs();
    }
}
