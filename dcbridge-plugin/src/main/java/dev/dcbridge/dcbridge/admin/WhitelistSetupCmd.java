package dev.dcbridge.dcbridge.admin;

import dev.dcbridge.dcbridge.bot.DiscordManager;
import dev.dcbridge.dcbridge.config.BotConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bukkit command /dcbridge-setup — only used for emergency token updates from the server console.
 * The real interactive setup wizard runs as the /whitelist-setup Discord slash command.
 *
 * Usage (console only):
 *   /dcbridge-setup token <BOT_TOKEN>
 */
public class WhitelistSetupCmd implements CommandExecutor {
    private final JavaPlugin plugin;
    private final BotConfig config;

    public WhitelistSetupCmd(JavaPlugin plugin, BotConfig config, DiscordManager discordManager) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            sender.sendMessage("Only the console or operators can run this command.");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("token")) {
            String token = args[1];
            plugin.getConfig().set("discord.token", token);
            plugin.saveConfig();
            plugin.reloadConfig();
            sender.sendMessage("[DCbridge] Bot token saved. Restart the server or reload the plugin for it to take effect.");
            return true;
        }

        sender.sendMessage("[DCbridge] Usage: /dcbridge-setup token <BOT_TOKEN>");
        sender.sendMessage("[DCbridge] For full Discord channel/role setup, use the /whitelist-setup slash command in your Discord server.");
        return true;
    }
}
