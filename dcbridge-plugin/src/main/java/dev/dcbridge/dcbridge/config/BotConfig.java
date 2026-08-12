package dev.dcbridge.dcbridge.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class BotConfig {
    private final JavaPlugin plugin;

    public BotConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Always fetch the live config from the plugin — reloadConfig() swaps in a new FileConfiguration instance,
     *  so caching a reference here would silently go stale after any write (e.g. /whitelist settings toggles). */
    private FileConfiguration config() {
        return plugin.getConfig();
    }

    public String getDiscordToken() {
        return config().getString("discord.token", "");
    }

    public String getClientId() {
        return config().getString("discord.client-id", "");
    }

    public String getGuildId() {
        return config().getString("discord.guild-id", "");
    }

    public String getWhitelistChannelId() {
        return config().getString("discord.channels.whitelist", "");
    }

    public String getWhitelistLogChannelId() {
        return config().getString("discord.channels.whitelist-log", "");
    }

    public String getWhitelistQueueChannelId() {
        return config().getString("discord.channels.whitelist-queue", "");
    }

    public String getWhitelistRoleId() {
        return config().getString("discord.roles.whitelist", "");
    }

    public String getWhitelistAdminRoleId() {
        return config().getString("discord.roles.whitelist-admin", "");
    }

    public String getAuthorizedUserId() {
        return config().getString("discord.authorized-user-id", "");
    }

    public String getWhitelistMode() {
        return config().getString("whitelist.mode", "strict");
    }

    /** If true, whitelist requests are approved immediately on submission instead of waiting in the queue. */
    public boolean isAutoAcceptEnabled() {
        return config().getBoolean("whitelist.auto-accept", false);
    }

    /** If true, a user cannot submit a new request while they already have one pending or approved. */
    public boolean isSingleSubmissionEnabled() {
        return config().getBoolean("whitelist.single-submission", false);
    }

    public String getGeyserPrefix() {
        return config().getString("whitelist.geyser-prefix", ".");
    }

    public String getDenyMessage() {
        return config().getString("whitelist.deny-message", "&cYou are not whitelisted.");
    }

    public String getAllowMessage() {
        return config().getString("whitelist.allow-message", "&aWelcome back!");
    }

    public int getUsernameMin() {
        return config().getInt("whitelist.username-min", 3);
    }

    public int getUsernameMax() {
        return config().getInt("whitelist.username-max", 16);
    }

    public String getFormTitle() {
        return config().getString("messages.form-title", "Whitelist Verification");
    }

    public String getPlatformLabel() {
        return config().getString("messages.platform-label", "Platform: Java or Bedrock?");
    }

    public String getPlatformPlaceholder() {
        return config().getString("messages.platform-placeholder", "Java or Bedrock");
    }

    public String getUsernameLabel() {
        return config().getString("messages.username-label", "Minecraft Username");
    }

    public String getUsernamePlaceholder() {
        return config().getString("messages.username-placeholder", "Your exact in-game username");
    }

    public String getEmbedTitle() {
        return config().getString("messages.embed-title", "Server Whitelist Verification");
    }

    public String getEmbedDescription() {
        return config().getString("messages.embed-description", "Click the button below to verify and get whitelisted...");
    }

    public String getEmbedFooter() {
        return config().getString("messages.embed-footer", "DCbridge Whitelist");
    }

    public String getVerifyButtonLabel() {
        return config().getString("messages.verify-button", "Click here to verify and get whitelisted");
    }

    public String getDoneButtonLabel() {
        return config().getString("messages.done-button", "Done");
    }

    public String getCancelButtonLabel() {
        return config().getString("messages.cancel-button", "Cancel");
    }

    public String getRevokeButtonLabel() {
        return config().getString("messages.revoke-button", "Revoke");
    }

    public String getSubmittedDmTemplate() {
        return config().getString("messages.submitted-dm", "Your whitelist request for {username} ({platform}) has been submitted and is pending review.");
    }

    public String getQueuedDmTemplate() {
        return config().getString("messages.queue-dm", "Your whitelist request for {username} ({platform}) is now in the review queue.");
    }

    public String getApprovedDmTemplate() {
        return config().getString("messages.approved-dm", "Your whitelist request for {username} ({platform}) has been approved!");
    }

    public String getDeniedDmTemplate() {
        return config().getString("messages.denied-dm", "Your whitelist request for {username} ({platform}) was denied.");
    }

    public String getRevokedDmTemplate() {
        return config().getString("messages.revoked-dm", "Your whitelist request for {username} ({platform}) has been revoked.");
    }

    public String getRemoveKeywords() {
        return config().getString("admin.remove-keywords", "remove ts,delete ts");
    }

    public String getUserRemoveKeywords() {
        return config().getString("admin.user-remove-keywords", "user remove ts");
    }

    public int getAutoDeleteSeconds() {
        return config().getInt("admin.auto-delete-seconds", 5);
    }

    public String getSqliteFileName() {
        return config().getString("data.sqlite-file", "whitelist.db");
    }
}
