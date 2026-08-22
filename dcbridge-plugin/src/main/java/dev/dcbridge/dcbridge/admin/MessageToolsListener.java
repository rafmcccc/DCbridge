package dev.dcbridge.dcbridge.admin;

import dev.dcbridge.dcbridge.config.BotConfig;
import dev.dcbridge.dcbridge.whitelist.WhitelistManager;
import dev.dcbridge.dcbridge.whitelist.WhitelistStore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * /wl-remove <username>
 *
 * Looks up the active whitelisted entry by username, revokes it from the DB,
 * removes the player from Bukkit's whitelist, and marks the linked request as revoked.
 * Only the authorized user (set in config) or ops can run this.
 */
public class MessageToolsListener implements CommandExecutor {
    private final JavaPlugin plugin;
    private final BotConfig config;
    private final WhitelistManager whitelistManager;

    public MessageToolsListener(JavaPlugin plugin, BotConfig config, WhitelistManager whitelistManager) {
        this.plugin = plugin;
        this.config = config;
        this.whitelistManager = whitelistManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!isAuthorized(sender)) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§eUsage: /wl-remove <username>");
            return true;
        }
        String username = args[0];
        WhitelistStore.WhitelistedPlayerRow row = whitelistManager.getActiveWhitelistedByUsername(username);
        if (row == null) {
            sender.sendMessage("§cNo active whitelisted player found with username: " + username);
            return true;
        }
        // Revoke via requestId so the request status is also updated
        String requestId = row.requestId();
        if (requestId != null && !requestId.isBlank()) {
            whitelistManager.revokeWhitelist(requestId, sender.getName());
        } else {
            // No linked request (manually added entry) — revoke directly
            whitelistManager.revokeDirectly(row.id(), row.username());
        }
        sender.sendMessage("§aRevoked whitelist for " + username + ".");
        plugin.getLogger().info(sender.getName() + " revoked whitelist for " + username + " via /wl-remove.");
        return true;
    }

    private boolean isAuthorized(CommandSender sender) {
        // Note: only OPs can run this. The Discord authorized-user-id is a Discord
        // snowflake, not a Minecraft UUID, so it is not checked here.
        return sender.isOp();
    }
}
