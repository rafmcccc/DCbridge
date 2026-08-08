package dev.dcbridge.dcbridge.stats;

import dev.dcbridge.dcbridge.config.BotConfig;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class StatsUpdater {
    private final JavaPlugin plugin;
    private final BotConfig config;
    private BukkitTask task;

    // Cached ping measured async so JDA event thread never blocks on TCP
    private final AtomicInteger lastPingMs = new AtomicInteger(-1);

    public StatsUpdater(JavaPlugin plugin, BotConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        if (task != null) {
            return;
        }
        long interval = 20L * config.getStatsIntervalSeconds();
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateStats, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Returns the last measured TCP ping in ms, or -1 if the server was unreachable. */
    public int getLastPingMs() {
        return lastPingMs.get();
    }

    private void updateStats() {
        // Measure ping here on the async thread and cache the result
        int ping = measurePing(config.getJavaIp(), config.getJavaPort());
        lastPingMs.set(ping);

        Map<String, Object> payload = new HashMap<>();
        payload.put("online", Bukkit.getOnlinePlayers().size());
        payload.put("max", Bukkit.getMaxPlayers());
        payload.put("version", Bukkit.getVersion());
        payload.put("tps", Bukkit.getTPS()[0]);
        payload.put("ping", ping);
        payload.put("timestamp", System.currentTimeMillis());

        File file = new File(plugin.getDataFolder(), config.getStatsFileName());
        try {
            Files.writeString(file.toPath(), payload.toString());
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to write stats file: " + ex.getMessage());
        }
    }

    private int measurePing(String host, int port) {
        try (Socket socket = new Socket()) {
            long start = System.currentTimeMillis();
            socket.connect(new InetSocketAddress(host, port), 1500);
            socket.getOutputStream().write(0);
            return (int) (System.currentTimeMillis() - start);
        } catch (Exception ex) {
            return -1;
        }
    }
}
