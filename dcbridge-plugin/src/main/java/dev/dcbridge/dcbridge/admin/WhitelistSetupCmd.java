package dev.dcbridge.dcbridge.admin;

import dev.dcbridge.dcbridge.bot.DiscordManager;
import dev.dcbridge.dcbridge.config.BotConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class WhitelistSetupCmd implements CommandExecutor {
    public WhitelistSetupCmd(JavaPlugin plugin, BotConfig config, DiscordManager discordManager) {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("Whitelist setup command is available; Discord embed is managed by the bot.");
        return true;
    }
}
