package dev.dcbridge.dcbridge.bot;

import dev.dcbridge.dcbridge.config.BotConfig;
import net.dv8tion.jda.api.entities.Activity;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

public class Presence {
    private final JavaPlugin plugin;
    private final BotConfig config;
    private BukkitTask task;

    public Presence(JavaPlugin plugin, BotConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (plugin.getServer().getOnlinePlayers().isEmpty()) {
                updateActivity(config.getOfflineText(), Activity.ActivityType.valueOf(config.getOfflineActivityType().toUpperCase()));
                return;
            }
            int online = Bukkit.getOnlinePlayers().size();
            int max = Bukkit.getMaxPlayers();
            String text = config.getPresenceFormat()
                    .replace("{emoji}", "🌐")
                    .replace("{name}", config.getServerName())
                    .replace("{online}", String.valueOf(online))
                    .replace("{max}", String.valueOf(max))
                    .replace("{ping}", "0");
            updateActivity(text, Activity.ActivityType.WATCHING);
        }, 20L * config.getPresenceIntervalSeconds(), 20L * config.getPresenceIntervalSeconds());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void updateActivity(String text, Activity.ActivityType type) {
        if (plugin.getServer().getPluginManager().getPlugin("DCbridge") == null) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (DiscordManagerHolder.getJda() != null) {
                DiscordManagerHolder.getJda().getPresence().setActivity(Activity.of(type, text));
            }
        });
    }
}
