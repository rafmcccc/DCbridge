package dev.dcbridge.dcbridge.stats;

import dev.dcbridge.dcbridge.config.BotConfig;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class StatsUpdater {
    private final JavaPlugin plugin;
    private final BotConfig config;
    private BukkitTask task;

    public StatsUpdater(JavaPlugin plugin, BotConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateStats, 20L * config.getStatsIntervalSeconds(), 20L * config.getStatsIntervalSeconds());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void updateStats() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("online", Bukkit.getOnlinePlayers().size());
        payload.put("max", Bukkit.getMaxPlayers());
        payload.put("version", Bukkit.getVersion());
        payload.put("tps", Bukkit.getTPS()[0]);
        payload.put("timestamp", System.currentTimeMillis());
        File file = new File(plugin.getDataFolder(), config.getStatsFileName());
        try {
            Files.writeString(file.toPath(), payload.toString());
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to write stats file: " + ex.getMessage());
        }
    }
}
