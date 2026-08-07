package dev.dcbridge.dcbridge.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class BotConfig {
    private final JavaPlugin plugin;
    private final FileConfiguration config;

    public BotConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public String getDiscordToken() {
        return config.getString("discord.token", "");
    }

    public String getClientId() {
        return config.getString("discord.client-id", "");
    }

    public String getGuildId() {
        return config.getString("discord.guild-id", "");
    }

    public String getWhitelistChannelId() {
        return config.getString("discord.channels.whitelist", "");
    }

    public String getWhitelistLogChannelId() {
        return config.getString("discord.channels.whitelist-log", "");
    }

    public String getWhitelistQueueChannelId() {
        return config.getString("discord.channels.whitelist-queue", "");
    }

    public String getWhitelistRoleId() {
        return config.getString("discord.roles.whitelist", "");
    }

    public String getWhitelistAdminRoleId() {
        return config.getString("discord.roles.whitelist-admin", "");
    }

    public String getAuthorizedUserId() {
        return config.getString("discord.authorized-user-id", "");
    }

    public String getServerName() {
        return config.getString("server.name", "DCbridge");
    }

    public String getJavaIp() {
        return config.getString("server.java-ip", "");
    }

    public String getBedrockIp() {
        return config.getString("server.bedrock-ip", "");
    }

    public int getJavaPort() {
        return config.getInt("server.java-port", 25565);
    }

    public int getBedrockPort() {
        return config.getInt("server.bedrock-port", 19132);
    }

    public int getStatsIntervalSeconds() {
        return config.getInt("stats.embed-interval-seconds", 30);
    }

    public String getColorOnline() {
        return config.getString("stats.color-online", "b6cdff");
    }

    public String getColorOffline() {
        return config.getString("stats.color-offline", "ff6b6b");
    }

    public String getGifUrl() {
        return config.getString("stats.gif-url", "");
    }

    public int getPresenceIntervalSeconds() {
        return config.getInt("presence.update-interval-seconds", 20);
    }

    public String getOfflineText() {
        return config.getString("presence.offline-text", "Server Offline 🔴");
    }

    public String getOfflineActivityType() {
        return config.getString("presence.offline-activity-type", "WATCHING");
    }

    public String getPresenceFormat() {
        return config.getString("presence.format", "{emoji} {name} | {online}/{max} Players | {ping}ms");
    }

    public String getWhitelistMode() {
        return config.getString("whitelist.mode", "strict");
    }

    public String getGeyserPrefix() {
        return config.getString("whitelist.geyser-prefix", ".");
    }

    public String getDenyMessage() {
        return config.getString("whitelist.deny-message", "&cYou are not whitelisted.");
    }

    public String getAllowMessage() {
        return config.getString("whitelist.allow-message", "&aWelcome back!");
    }

    public int getUsernameMin() {
        return config.getInt("whitelist.username-min", 3);
    }

    public int getUsernameMax() {
        return config.getInt("whitelist.username-max", 16);
    }

    public String getFormTitle() {
        return config.getString("messages.form-title", "Whitelist Verification");
    }

    public String getPlatformLabel() {
        return config.getString("messages.platform-label", "Platform: Java or Bedrock?");
    }

    public String getPlatformPlaceholder() {
        return config.getString("messages.platform-placeholder", "Java or Bedrock");
    }

    public String getUsernameLabel() {
        return config.getString("messages.username-label", "Minecraft Username");
    }

    public String getUsernamePlaceholder() {
        return config.getString("messages.username-placeholder", "Your exact in-game username");
    }

    public String getEmbedTitle() {
        return config.getString("messages.embed-title", "Server Whitelist Verification");
    }

    public String getEmbedDescription() {
        return config.getString("messages.embed-description", "Click the button below to verify and get whitelisted...");
    }

    public String getEmbedFooter() {
        return config.getString("messages.embed-footer", "DCbridge Whitelist");
    }

    public String getVerifyButtonLabel() {
        return config.getString("messages.verify-button", "Click here to verify and get whitelisted");
    }

    public String getDoneButtonLabel() {
        return config.getString("messages.done-button", "Done");
    }

    public String getCancelButtonLabel() {
        return config.getString("messages.cancel-button", "Cancel");
    }

    public String getApprovedDmTemplate() {
        return config.getString("messages.approved-dm", "Your whitelist request for {username} ({platform}) has been approved!");
    }

    public String getDeniedDmTemplate() {
        return config.getString("messages.denied-dm", "Your whitelist request for {username} ({platform}) was denied.");
    }

    public String getRemoveKeywords() {
        return config.getString("admin.remove-keywords", "remove ts,delete ts");
    }

    public String getUserRemoveKeywords() {
        return config.getString("admin.user-remove-keywords", "user remove ts");
    }

    public int getAutoDeleteSeconds() {
        return config.getInt("admin.auto-delete-seconds", 5);
    }

    public String getSqliteFileName() {
        return config.getString("data.sqlite-file", "whitelist.db");
    }

    public String getStatsFileName() {
        return config.getString("data.stats-file", "stats.json");
    }

    public ConfigurationSection getPresenceSubServers() {
        return config.getConfigurationSection("presence.sub-servers");
    }
}
