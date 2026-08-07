package dev.dcbridge.dcbridge.admin;

import dev.dcbridge.dcbridge.config.BotConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MessageToolsListener implements Listener {
    private final BotConfig config;

    public MessageToolsListener(JavaPlugin plugin, BotConfig config) {
        this.config = config;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (!player.getUniqueId().toString().equalsIgnoreCase(config.getAuthorizedUserId())) {
            return;
        }
        if (!message.contains("remove") && !message.contains("delete")) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage("Admin message tool acknowledged.");
    }
}
