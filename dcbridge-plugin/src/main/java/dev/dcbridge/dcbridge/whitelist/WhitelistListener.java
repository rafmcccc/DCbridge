package dev.dcbridge.dcbridge.whitelist;

import dev.dcbridge.dcbridge.config.BotConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class WhitelistListener implements Listener {
    private final JavaPlugin plugin;
    private final BotConfig config;
    private final WhitelistManager whitelistManager;

    public WhitelistListener(JavaPlugin plugin, BotConfig config, WhitelistManager whitelistManager) {
        this.plugin = plugin;
        this.config = config;
        this.whitelistManager = whitelistManager;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        String rawName = event.getPlayer().getName();
        if (whitelistManager.shouldAllowJoin(rawName)) {
            event.allow();
            whitelistManager.sendAllowMessage(event.getPlayer());
            return;
        }

        if ("notify".equalsIgnoreCase(config.getWhitelistMode())) {
            whitelistManager.sendDenyMessage(event.getPlayer());
            return;
        }

        String normalized = whitelistManager.normalizeName(rawName);
        plugin.getLogger().warning("Denied login for " + rawName + " (normalized: " + normalized + ") — not found in the whitelist database.");
        event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, config.getDenyMessage().replace('&', '§'));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // The server's native whitelist entry was created from a name lookup
        // (placeholder UUID) at approval time. Once the real profile is known,
        // re-assert it so vanilla whitelist stays matched on later logins.
        Player player = event.getPlayer();
        if (whitelistManager.shouldAllowJoin(player.getName())) {
            whitelistManager.applyWhitelist(player.getName());
        }
    }
}
