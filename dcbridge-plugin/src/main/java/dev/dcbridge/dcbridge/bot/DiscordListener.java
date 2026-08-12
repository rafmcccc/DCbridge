package dev.dcbridge.dcbridge.bot;

import dev.dcbridge.dcbridge.admin.SetupListener;
import dev.dcbridge.dcbridge.config.BotConfig;
import dev.dcbridge.dcbridge.whitelist.WhitelistManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.Color;
import java.time.Instant;
import java.util.UUID;

public class DiscordListener extends ListenerAdapter {
    private final JavaPlugin plugin;
    private final BotConfig config;
    private final WhitelistManager whitelistManager;
    private final DiscordManager discordManager;

    public DiscordListener(JavaPlugin plugin, BotConfig config, WhitelistManager whitelistManager, DiscordManager discordManager) {
        this.plugin = plugin;
        this.config = config;
        this.whitelistManager = whitelistManager;
        this.discordManager = discordManager;
    }

    @Override
    public void onReady(ReadyEvent event) {
        registerSlashCommands(event);
    }

    private void registerSlashCommands(ReadyEvent event) {
        // /whitelist setup|remove|settings
        SlashCommandData whitelist = Commands.slash("whitelist", "Manage the whitelist system")
                .addSubcommands(
                        new SubcommandData("setup",
                                "Interactive setup wizard: configure channels, roles, and admin — no manual ID entry"),
                        new SubcommandData("remove",
                                "Remove the whitelist verification embed from the configured whitelist channel"),
                        new SubcommandData("settings",
                                "Configure auto-accept and one-request-per-user behavior")
                );

        // Replace global commands with the current set (this atomically removes any stale ones)
        event.getJDA().updateCommands().addCommands(whitelist).queue(
                cmds -> plugin.getLogger().info("[DCbridge] Slash commands registered: " + cmds.size()),
                err  -> plugin.getLogger().severe("[DCbridge] Failed to register slash commands: " + err.getMessage())
        );
        // Also wipe any guild-scoped commands left over from previous plugin versions
        for (net.dv8tion.jda.api.entities.Guild g : event.getJDA().getGuilds()) {
            g.updateCommands().queue(
                    ignored -> {},
                    err -> plugin.getLogger().warning("[DCbridge] Could not clear guild commands for " + g.getName() + ": " + err.getMessage())
            );
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String cmd = event.getName();
        String sub = event.getSubcommandName();

        if (cmd.equals("whitelist")) {
            if ("setup".equals(sub)) {
                discordManager.getSetupListener().handleSetupCommand(event);
            } else if ("remove".equals(sub)) {
                handleWhitelistRemove(event);
            } else if ("settings".equals(sub)) {
                handleWhitelistSettings(event);
            }
        }
    }

    /** /whitelist settings — dashboard to toggle auto-accept and one-request-per-user. */
    private void handleWhitelistSettings(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.reply("This command must be used in a server.").setEphemeral(true).queue();
            return;
        }
        if (!hasAdminPermission(event.getUser(), event.getMember())) {
            event.reply("You need the whitelist admin role (or Administrator) to change these settings.").setEphemeral(true).queue();
            return;
        }
        event.replyEmbeds(buildSettingsEmbed().build())
                .addComponents(settingsButtons())
                .setEphemeral(true)
                .queue();
    }

    private EmbedBuilder buildSettingsEmbed() {
        boolean autoAccept = config.isAutoAcceptEnabled();
        boolean singleSubmission = config.isSingleSubmissionEnabled();
        return new EmbedBuilder()
                .setTitle("⚙️ Whitelist Settings")
                .setColor(Color.decode("#b6cdff"))
                .addField("Auto-Accept Requests",
                        autoAccept
                                ? "✅ **ON** — new requests are approved immediately and never enter the review queue."
                                : "❌ **OFF** — an admin must approve each request in the queue channel.",
                        false)
                .addField("One Request Per User",
                        singleSubmission
                                ? "✅ **ON** — a user can't submit again while they have a pending or approved request."
                                : "❌ **OFF** — users may submit as many requests as they like.",
                        false)
                .setFooter("Click a button below to toggle a setting");
    }

    private ActionRow settingsButtons() {
        return ActionRow.of(
                Button.primary("settings:toggle:auto-accept", "Toggle Auto-Accept"),
                Button.primary("settings:toggle:single-submission", "Toggle One-Per-User")
        );
    }

    private void handleSettingsToggle(ButtonInteractionEvent event) {
        if (!hasAdminPermission(event)) {
            event.reply("You do not have permission to change these settings.").setEphemeral(true).queue();
            return;
        }
        String key = event.getComponentId().substring("settings:toggle:".length());
        String configKey = switch (key) {
            case "auto-accept" -> "whitelist.auto-accept";
            case "single-submission" -> "whitelist.single-submission";
            default -> null;
        };
        if (configKey == null) {
            event.reply("Unknown setting.").setEphemeral(true).queue();
            return;
        }
        boolean current = plugin.getConfig().getBoolean(configKey, false);
        plugin.getConfig().set(configKey, !current);
        plugin.saveConfig();
        plugin.reloadConfig();
        event.editMessageEmbeds(buildSettingsEmbed().build())
                .setComponents(settingsButtons())
                .queue();
    }

    /** /whitelist remove — clears the stored whitelist channel message ID (embed removal). */
    private void handleWhitelistRemove(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.reply("This command must be used in a server.").setEphemeral(true).queue();
            return;
        }
        // Clear the stored whitelist channel config so no embed is auto-reposted
        plugin.getConfig().set("discord.channels.whitelist", "");
        plugin.saveConfig();
        plugin.reloadConfig();
        event.reply("✅ Whitelist embed configuration cleared. The verification embed will no longer be auto-posted.")
                .setEphemeral(true).queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("verify:open")) {
            openModal(event);
        } else if (event.getComponentId().startsWith("whitelist:review:")) {
            handleReview(event);
        } else if (event.getComponentId().startsWith("settings:toggle:")) {
            handleSettingsToggle(event);
        }
    }

    private void openModal(ButtonInteractionEvent event) {
        TextInput platform = TextInput.create("platform", config.getPlatformLabel(), TextInputStyle.SHORT)
                .setPlaceholder(config.getPlatformPlaceholder())
                .build();
        TextInput username = TextInput.create("username", config.getUsernameLabel(), TextInputStyle.SHORT)
                .setPlaceholder(config.getUsernamePlaceholder())
                .build();
        Modal modal = Modal.create("whitelist:form", config.getFormTitle())
                .addComponents(ActionRow.of(platform), ActionRow.of(username))
                .build();
        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("whitelist:form")) {
            String platform = event.getValue("platform").getAsString();
            String username = event.getValue("username").getAsString();
            handleSubmission(event, platform, username);
        }
    }

    private void handleSubmission(ModalInteractionEvent event, String platform, String username) {
        if (!platform.equalsIgnoreCase("Java") && !platform.equalsIgnoreCase("Bedrock")) {
            event.reply("Platform must be Java or Bedrock.").setEphemeral(true).queue();
            return;
        }
        if (username.length() < config.getUsernameMin() || username.length() > config.getUsernameMax()) {
            event.reply("Username length is invalid.").setEphemeral(true).queue();
            return;
        }
        if (whitelistManager.hasPendingRequest(event.getUser().getId())) {
            event.reply("You already have a request waiting in the review queue. Please wait for it to be approved or denied before submitting again.")
                    .setEphemeral(true).queue();
            return;
        }
        if (config.isSingleSubmissionEnabled() && whitelistManager.hasOpenRequest(event.getUser().getId())) {
            event.reply("You already have a pending or approved whitelist request. Ask an admin to revoke it before submitting again.")
                    .setEphemeral(true).queue();
            return;
        }

        String requestId = whitelistManager.submitRequest(event.getUser().getId(), event.getGuild().getId(), username, platform);
        plugin.getLogger().info("New whitelist submission " + requestId + " for " + username);

        boolean autoAccept = config.isAutoAcceptEnabled();
        String requesterMention = event.getUser().getAsMention();

        if (autoAccept) {
            // Skip the queue entirely — approve through the same path a manual approval uses.
            whitelistManager.handleApproval(event.getUser().getId(), event.getGuild().getId(), username, platform, requestId);
            assignWhitelistRoleToRequester(event.getGuild().getId(), event.getUser().getId());

            EmbedBuilder embed = buildRequestEmbed(requesterMention, username, platform, "Approved", "Auto-Accepted", requestId, Color.GREEN);
            if (!config.getWhitelistLogChannelId().isBlank()) {
                TextChannel logChannel = event.getJDA().getTextChannelById(config.getWhitelistLogChannelId());
                if (logChannel != null) {
                    logChannel.sendMessageEmbeds(embed.build()).queue(message -> whitelistManager.setLogMessageId(requestId, message.getId()));
                }
            }

            sendUserDm(event.getUser(), renderTemplate(config.getApprovedDmTemplate(), username, platform));
            event.reply("Your request was automatically approved. Welcome!").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = buildRequestEmbed(requesterMention, username, platform, "Pending", "Nobody yet", requestId, Color.YELLOW);

        if (!config.getWhitelistLogChannelId().isBlank()) {
            TextChannel logChannel = event.getJDA().getTextChannelById(config.getWhitelistLogChannelId());
            if (logChannel != null) {
                logChannel.sendMessageEmbeds(embed.build()).queue(message -> whitelistManager.setLogMessageId(requestId, message.getId()));
            }
        }

        if (!config.getWhitelistQueueChannelId().isBlank()) {
            TextChannel queueChannel = event.getJDA().getTextChannelById(config.getWhitelistQueueChannelId());
            if (queueChannel != null) {
                String reviewId = "whitelist:review:" + requestId;
                queueChannel.sendMessageEmbeds(embed.build())
                        .addActionRow(
                                Button.success(reviewId + ":approve", config.getDoneButtonLabel()),
                                Button.danger(reviewId + ":deny", config.getCancelButtonLabel())
                        )
                        .queue(message -> {
                            whitelistManager.setQueueMessageId(requestId, message.getId());
                            sendUserDm(event.getUser(), renderTemplate(config.getQueuedDmTemplate(), username, platform));
                        });
            }
        }

        sendUserDm(event.getUser(), renderTemplate(config.getSubmittedDmTemplate(), username, platform));
        event.reply("Your request has been submitted for review.").setEphemeral(true).queue();
    }

    private void handleReview(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        String[] parts = componentId.split(":", 4);
        if (parts.length < 4) {
            event.reply("Invalid review button data. Please try again.").setEphemeral(true).queue();
            return;
        }

        // Guard: admin-only
        if (!hasAdminPermission(event)) {
            event.reply("You do not have permission to review whitelist requests.").setEphemeral(true).queue();
            return;
        }

        String requestId = parts[2];
        String action = parts[3];
        var request = whitelistManager.getRequestById(requestId);
        if (request == null) {
            event.reply("Could not find whitelist request data. Please try again or contact an admin.").setEphemeral(true).queue();
            return;
        }

        String currentStatus = request.status();

        // Guard: revoke is only valid on approved entries
        if (action.equals("revoke") && !currentStatus.equals("approved")) {
            event.reply("You can only revoke an approved whitelist entry.").setEphemeral(true).queue();
            return;
        }

        // Guard: approve/deny only valid on pending
        if ((action.equals("approve") || action.equals("deny")) && !currentStatus.equals("pending")) {
            event.reply("This request has already been handled (status: " + currentStatus + ").").setEphemeral(true).queue();
            return;
        }

        String requesterMention = "<@" + request.userId() + ">";
        String reviewerMention = event.getUser().getAsMention();

        if (action.equals("approve")) {
            whitelistManager.handleApproval(event.getUser().getId(), event.getGuild().getId(), request.username(), request.platform(), requestId);
            assignWhitelistRoleToRequester(request.guildId(), request.userId());
            // After approval: keep only the Revoke button so admins can unwhitelist later
            updateQueueMessageWithRevoke(event, requestId, request.username(), request.platform(), "Approved", reviewerMention, requesterMention);
            updateLogMessage(event, requestId, request.username(), request.platform(), "Approved", reviewerMention, requesterMention);
            sendUserDm(event.getJDA().getUserById(request.userId()), renderTemplate(config.getApprovedDmTemplate(), request.username(), request.platform()));
            event.reply("Request approved.").setEphemeral(true).queue();
        } else if (action.equals("deny")) {
            whitelistManager.handleDenial(requestId, event.getUser().getId());
            // After denial: remove all buttons — no further action possible
            updateQueueMessage(event, requestId, request.username(), request.platform(), "Denied", reviewerMention, requesterMention);
            updateLogMessage(event, requestId, request.username(), request.platform(), "Denied", reviewerMention, requesterMention);
            sendUserDm(event.getJDA().getUserById(request.userId()), renderTemplate(config.getDeniedDmTemplate(), request.username(), request.platform()));
            event.reply("Request denied.").setEphemeral(true).queue();
        } else if (action.equals("revoke")) {
            whitelistManager.revokeWhitelist(requestId, event.getUser().getId());
            removeWhitelistRoleFromRequester(request.guildId(), request.userId());
            // After revoke: remove all buttons — entry is dead
            updateQueueMessage(event, requestId, request.username(), request.platform(), "Revoked", reviewerMention, requesterMention);
            updateLogMessage(event, requestId, request.username(), request.platform(), "Revoked", reviewerMention, requesterMention);
            sendUserDm(event.getJDA().getUserById(request.userId()), renderTemplate(config.getRevokedDmTemplate(), request.username(), request.platform()));
            event.reply("Whitelist access revoked.").setEphemeral(true).queue();
        }
    }

    private EmbedBuilder buildRequestEmbed(String userMention, String username, String platform, String status, String handledBy, String requestId, Color color) {
        String statusEmoji = switch (status) {
            case "Approved" -> "✅ Approved";
            case "Denied"   -> "❌ Denied";
            case "Revoked"  -> "🚫 Revoked";
            default         -> "⏳ Pending";
        };
        return new EmbedBuilder()
                .setTitle("📋 Whitelist Request")
                .setColor(color)
                .addField("👤 Applicant", userMention, true)
                .addField("🎮 Username", "`" + username + "`", true)
                .addField("🖥️ Platform", platform, true)
                .addField("📊 Status", statusEmoji, true)
                .addField("🛡️ Handled By", handledBy, true)
                .addField("\u200B", "\u200B", true) // spacing filler
                .setFooter("Request ID: " + requestId)
                .setTimestamp(Instant.now());
    }

    private boolean hasAdminPermission(ButtonInteractionEvent event) {
        return hasAdminPermission(event.getUser(), event.getMember());
    }

    private boolean hasAdminPermission(net.dv8tion.jda.api.entities.User user, net.dv8tion.jda.api.entities.Member member) {
        if (user.getId().equals(config.getAuthorizedUserId())) {
            return true;
        }
        if (config.getWhitelistAdminRoleId() == null || config.getWhitelistAdminRoleId().isBlank()) {
            return false;
        }
        if (member == null) {
            return false;
        }
        return member.getRoles().stream().anyMatch(role -> role.getId().equals(config.getWhitelistAdminRoleId()));
    }

    private void sendUserDm(net.dv8tion.jda.api.entities.User user, String message) {
        if (user == null || message == null || message.isBlank()) {
            return;
        }
        user.openPrivateChannel().queue(channel -> channel.sendMessage(message).queue(), failure -> plugin.getLogger().warning("Unable to open DM channel: " + failure.getMessage()));
    }

    private String renderTemplate(String template, String username, String platform) {
        if (template == null) {
            return "";
        }
        return template.replace("{username}", username == null ? "" : username)
                .replace("{platform}", platform == null ? "" : platform);
    }

    private void assignWhitelistRoleToRequester(String guildId, String userId) {
        if (config.getWhitelistRoleId().isBlank()) {
            return;
        }
        var jda = DiscordManagerHolder.getJda();
        if (jda == null) {
            return;
        }
        var guild = jda.getGuildById(guildId);
        if (guild == null) {
            return;
        }
        var role = guild.getRoleById(config.getWhitelistRoleId());
        if (role == null) {
            return;
        }
        var selfMember = guild.getSelfMember();
        if (!selfMember.hasPermission(Permission.MANAGE_ROLES)) {
            plugin.getLogger().warning("Cannot assign whitelist role because the bot lacks MANAGE_ROLES permission.");
            return;
        }
        if (!selfMember.canInteract(role)) {
            plugin.getLogger().warning("Cannot assign whitelist role because the bot's highest role is not higher than the whitelist role: " + role.getName() + " (" + role.getId() + ")");
            return;
        }
        guild.retrieveMemberById(userId).queue(member -> {
            if (!selfMember.canInteract(member)) {
                plugin.getLogger().warning("Cannot assign whitelist role because the bot cannot interact with member: " + member.getUser().getAsTag());
                return;
            }
            if (!member.getRoles().contains(role)) {
                guild.addRoleToMember(member, role).queue(
                        ignored -> plugin.getLogger().info("Assigned whitelist role to " + member.getUser().getAsTag()),
                        failure -> plugin.getLogger().warning("Failed to assign whitelist role: " + failure.getMessage())
                );
            }
        }, failure -> plugin.getLogger().warning("Failed to retrieve guild member for role assignment: " + failure.getMessage()));
    }

    private void removeWhitelistRoleFromRequester(String guildId, String userId) {
        if (config.getWhitelistRoleId().isBlank()) {
            return;
        }
        var jda = DiscordManagerHolder.getJda();
        if (jda == null) {
            return;
        }
        var guild = jda.getGuildById(guildId);
        if (guild == null) {
            return;
        }
        var role = guild.getRoleById(config.getWhitelistRoleId());
        if (role == null) {
            return;
        }
        var selfMember = guild.getSelfMember();
        if (!selfMember.hasPermission(Permission.MANAGE_ROLES)) {
            plugin.getLogger().warning("Cannot remove whitelist role because the bot lacks MANAGE_ROLES permission.");
            return;
        }
        if (!selfMember.canInteract(role)) {
            plugin.getLogger().warning("Cannot remove whitelist role because the bot's highest role is not higher than the whitelist role: " + role.getName() + " (" + role.getId() + ")");
            return;
        }
        guild.retrieveMemberById(userId).queue(member -> {
            if (!selfMember.canInteract(member)) {
                plugin.getLogger().warning("Cannot remove whitelist role because the bot cannot interact with member: " + member.getUser().getAsTag());
                return;
            }
            if (member.getRoles().contains(role)) {
                guild.removeRoleFromMember(member, role).queue(
                        ignored -> plugin.getLogger().info("Removed whitelist role from " + member.getUser().getAsTag()),
                        failure -> plugin.getLogger().warning("Failed to remove whitelist role: " + failure.getMessage())
                );
            }
        }, failure -> plugin.getLogger().warning("Failed to retrieve guild member for role removal: " + failure.getMessage()));
    }

    private Color statusColor(String status) {
        return switch (status) {
            case "Approved" -> Color.GREEN;
            case "Denied"   -> Color.RED;
            case "Revoked"  -> Color.decode("#FF8C00"); // dark orange
            default         -> Color.YELLOW;
        };
    }

    private void updateQueueMessage(ButtonInteractionEvent event, String requestId, String username, String platform, String status, String handledBy, String requesterMention) {
        String queueMessageId = whitelistManager.getQueueMessageId(requestId);
        if (queueMessageId == null || queueMessageId.isBlank()) {
            return;
        }
        String queueChannelId = config.getWhitelistQueueChannelId();
        if (queueChannelId.isBlank()) {
            return;
        }
        TextChannel queueChannel = event.getJDA().getTextChannelById(queueChannelId);
        if (queueChannel == null) {
            return;
        }
        queueChannel.retrieveMessageById(queueMessageId).queue(message -> {
            EmbedBuilder updated = buildRequestEmbed(requesterMention, username, platform, status, handledBy, requestId, statusColor(status));
            message.editMessageEmbeds(updated.build()).setComponents().queue();
        }, failure -> plugin.getLogger().warning("Failed to update queue message for request " + requestId + ": " + failure.getMessage()));
    }

    private void updateQueueMessageWithRevoke(ButtonInteractionEvent event, String requestId, String username, String platform, String status, String handledBy, String requesterMention) {
        String queueMessageId = whitelistManager.getQueueMessageId(requestId);
        if (queueMessageId == null || queueMessageId.isBlank()) {
            return;
        }
        String queueChannelId = config.getWhitelistQueueChannelId();
        if (queueChannelId.isBlank()) {
            return;
        }
        TextChannel queueChannel = event.getJDA().getTextChannelById(queueChannelId);
        if (queueChannel == null) {
            return;
        }
        queueChannel.retrieveMessageById(queueMessageId).queue(message -> {
            EmbedBuilder updated = buildRequestEmbed(requesterMention, username, platform, status, handledBy, requestId, statusColor(status));
            String reviewId = "whitelist:review:" + requestId;
            message.editMessageEmbeds(updated.build())
                    .setComponents(ActionRow.of(Button.danger(reviewId + ":revoke", config.getRevokeButtonLabel())))
                    .queue();
        }, failure -> plugin.getLogger().warning("Failed to update queue message for request " + requestId + ": " + failure.getMessage()));
    }

    private void updateLogMessage(ButtonInteractionEvent event, String requestId, String username, String platform, String status, String handledBy, String requesterMention) {
        String logMessageId = whitelistManager.getLogMessageId(requestId);
        if (logMessageId == null || logMessageId.isBlank()) {
            return;
        }
        String logChannelId = config.getWhitelistLogChannelId();
        if (logChannelId.isBlank()) {
            return;
        }
        TextChannel logChannel = event.getJDA().getTextChannelById(logChannelId);
        if (logChannel == null) {
            return;
        }
        logChannel.retrieveMessageById(logMessageId).queue(message -> {
            EmbedBuilder updated = buildRequestEmbed(requesterMention, username, platform, status, handledBy, requestId, statusColor(status));
            message.editMessageEmbeds(updated.build()).queue();
        }, failure -> plugin.getLogger().warning("Failed to update log message for request " + requestId + ": " + failure.getMessage()));
    }

}