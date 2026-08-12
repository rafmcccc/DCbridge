package dev.dcbridge.dcbridge.admin;

/**
 * Holds in-progress /whitelist-setup data collected across multiple Discord interactions.
 * Keyed by the Discord user ID of the admin who ran the command.
 */
public class SetupSession {
    public String guildId;
    public String whitelistChannelId;
    public String logChannelId;
    public String queueChannelId;
    public String whitelistRoleId;
    public String adminRoleId;
    public String authorizedUserId;

    /** True when all required fields have been filled. */
    public boolean isComplete() {
        return notBlank(guildId)
                && notBlank(whitelistChannelId)
                && notBlank(logChannelId)
                && notBlank(queueChannelId)
                && notBlank(whitelistRoleId)
                && notBlank(adminRoleId);
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
