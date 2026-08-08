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

    // Patterns associated with sign-based exploits: colour/format codes injected via
    // packets, excessively long lines, or null characters used in translation exploits.
    private static final java.util.regex.Pattern SIGN_EXPLOIT_PATTERN =
            java.util.regex.Pattern.compile("[§\u00a7\u0000]|\\{\"translate\"", java.util.regex.Pattern.CASE_INSENSITIVE);

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onSignChange(SignChangeEvent event) {
        if (!config.isCheckHacksEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        // Bypass for ops and privileged players
        if (player.hasPermission("dcbridge.checkhacks.bypass") || player.isOp()) {
            return;
        }

        String[] lines = event.getLines();
        boolean suspicious = false;
        for (String line : lines) {
            if (line == null) continue;
            // Flag: contains formatting/colour codes or JSON translation component
            if (SIGN_EXPLOIT_PATTERN.matcher(line).find()) {
                suspicious = true;
                break;
            }
            // Flag: single line far exceeds the Vanilla 15-char sign limit (packet hack)
            if (line.length() > 64) {
                suspicious = true;
                break;
            }
        }

        if (!suspicious) {
            return;
        }

        String reason = "Sign text exploit attempt";
        String details = "lines=" + String.join(" | ", lines);
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