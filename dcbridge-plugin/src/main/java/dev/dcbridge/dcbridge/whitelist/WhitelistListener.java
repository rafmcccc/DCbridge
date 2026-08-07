package dev.dcbridge.dcbridge.whitelist;

import dev.dcbridge.dcbridge.config.BotConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
            whitelistManager.sendAllowMessage(event.getPlayer());
            return;
        }

        if ("notify".equalsIgnoreCase(config.getWhitelistMode())) {
            whitelistManager.sendDenyMessage(event.getPlayer());
            return;
        }

        event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, config.getDenyMessage().replace('&', '§'));
    }
}
