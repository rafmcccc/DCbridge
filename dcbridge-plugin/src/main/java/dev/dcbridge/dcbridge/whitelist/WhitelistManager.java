package dev.dcbridge.dcbridge.whitelist;

import dev.dcbridge.dcbridge.config.BotConfig;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class WhitelistManager {
    private final JavaPlugin plugin;
    private final BotConfig config;
    private final WhitelistStore store;

    public WhitelistManager(JavaPlugin plugin, BotConfig config, WhitelistStore store) {
        this.plugin = plugin;
        this.config = config;
        this.store = store;
    }

    public boolean isAllowed(String username) {
        return store.isActiveUsername(username);
    }

    public void applyWhitelist(String username) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
            offlinePlayer.setWhitelisted(true);
        });
    }

    public String submitRequest(String userId, String guildId, String username, String platform) {
        String requestId = UUID.randomUUID().toString();
        store.putPendingRequest(requestId, userId, guildId, username, platform);
        return requestId;
    }

    public void handleApproval(String userId, String guildId, String username, String platform, String requestId) {
        store.addWhitelistedPlayer(userId, guildId, username, platform, requestId);
        store.updateRequestStatus(requestId, "approved", userId);
        applyWhitelist(username);
    }

    public void handleCancellation(String requestId, String handledBy) {
        store.updateRequestStatus(requestId, "cancelled", handledBy);
    }

    public void setQueueMessageId(String requestId, String messageId) {
        store.setQueueMessageId(requestId, messageId);
    }

    public void setLogMessageId(String requestId, String messageId) {
        store.setLogMessageId(requestId, messageId);
    }

    public String normalizeName(String rawName) {
        String prefix = config.getGeyserPrefix();
        if (prefix != null && !prefix.isEmpty() && rawName.startsWith(prefix)) {
            return rawName.substring(prefix.length());
        }
        return rawName;
    }

    public boolean shouldAllowJoin(String rawName) {
        String normalized = normalizeName(rawName);
        if (store.isActiveUsername(normalized)) {
            return true;
        }
        return false;
    }

    public void sendDenyMessage(Player player) {
        if (player == null) {
            return;
        }
        String message = config.getDenyMessage().replace('&', '§');
        player.sendMessage(message);
    }

    public void sendAllowMessage(Player player) {
        if (player == null) {
            return;
        }
        String message = config.getAllowMessage().replace('&', '§');
        player.sendMessage(message);
    }
}
