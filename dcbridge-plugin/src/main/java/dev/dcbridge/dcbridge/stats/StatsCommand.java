package dev.dcbridge.dcbridge.stats;

import dev.dcbridge.dcbridge.config.BotConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class StatsCommand implements CommandExecutor {
    public StatsCommand(JavaPlugin plugin, BotConfig config, StatsUpdater statsUpdater) {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("Usage: /status setup|remove [channel]");
            return true;
        }
        if (args[0].equalsIgnoreCase("remove")) {
            sender.sendMessage("Status embed removal requested.");
            return true;
        }
        sender.sendMessage("Status embed setup requested.");
        return true;
    }
}
