package dev.dcbridge.dcbridge.checkhacks;

import dev.dcbridge.dcbridge.config.BotConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CheckHacksListener implements Listener {
    private final JavaPlugin plugin;
    private final BotConfig config;
    private final Map<UUID, Long> lastAlertAt = new HashMap<>();
    private final Map<UUID, Integer> suspiciousEvents = new HashMap<>();

    public CheckHacksListener(JavaPlugin plugin, BotConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        if (!config.isCheckHacksEnabled()) {
            return;
        }

        String reason = "Sign translation / text exploit attempt";
        String details = "sign lines=" + String.join(" | ", event.getLines());
        sendAlert(player, reason, details);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!config.isCheckHacksEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission("dcbridge.checkhacks.bypass") || player.isOp()) {
            return;
        }

        if (event.getFrom().getWorld() == null || event.getTo() == null || !event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            return;
        }

        double distance = event.getFrom().distance(event.getTo());
        if (distance <= 0.0) {
            return;
        }

        if (distance > config.getCheckHacksThreshold()) {
            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();
            long last = lastAlertAt.getOrDefault(uuid, 0L);
            int count = suspiciousEvents.getOrDefault(uuid, 0) + 1;
            suspiciousEvents.put(uuid, count);

            if (now - last >= config.getCheckHacksCooldownMs()) {
                String reason = "Suspicious movement speed";
                String details = "distance=" + String.format("%.2f", distance);
                sendAlert(player, reason, details);
                lastAlertAt.put(uuid, now);
                suspiciousEvents.put(uuid, 0);
            }
        }
    }

    private void sendAlert(Player player, String reason, String details) {
        if (!config.isCheckHacksEnabled()) {
            return;
        }
        String webhookUrl = config.getCheckHacksWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        CheckHacksWebhookUtil.send(webhookUrl, config.getCheckHacksAlertTemplate(), player.getName(), reason, details);
        plugin.getLogger().info("[CheckHacks] " + player.getName() + " -> " + reason + " | " + details);
    }
}
