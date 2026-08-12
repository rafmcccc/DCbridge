package dev.dcbridge.dcbridge.admin;

import dev.dcbridge.dcbridge.config.BotConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the /whitelist-setup slash command as a fully interactive Discord-side wizard.
 * Admins never touch a config file for channel/role/guild IDs.
 *
 * Flow:
 *   Step 1 → pick whitelist channel
 *   Step 2 → pick log channel
 *   Step 3 → pick queue channel
 *   Step 4 → pick whitelist role
 *   Step 5 → pick admin role
 *   Step 6 → confirm and save
 */
public class SetupListener extends ListenerAdapter {

    // One session per Discord user running the wizard
    private final Map<String, SetupSession> sessions = new ConcurrentHashMap<>();

    private final JavaPlugin plugin;
    private final BotConfig config;

    public SetupListener(JavaPlugin plugin, BotConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public void handleSetupCommand(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.reply("Run this command inside your Discord server.").setEphemeral(true).queue();
            return;
        }

        // Only authorized user or admin role may run setup
        boolean authorized = event.getUser().getId().equals(config.getAuthorizedUserId());
        if (!authorized && event.getMember() != null) {
            String adminRole = config.getWhitelistAdminRoleId();
            if (adminRole != null && !adminRole.isBlank()) {
                authorized = event.getMember().getRoles().stream()
                        .anyMatch(r -> r.getId().equals(adminRole));
            }
        }
        // First-run: no authorized user set yet → allow anyone with ADMINISTRATOR permission
        if (!authorized && event.getMember() != null) {
            authorized = event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR);
        }
        if (!authorized) {
            event.reply("You need Administrator permission or the configured admin role to run setup.").setEphemeral(true).queue();
            return;
        }

        SetupSession session = new SetupSession();
        session.guildId = event.getGuild().getId();
        sessions.put(event.getUser().getId(), session);

        event.replyEmbeds(stepEmbed("Step 1 / 5 — Whitelist Channel",
                "Select the channel where the verification embed will be posted.\n" +
                "Users click a button in this channel to request whitelisting.",
                1).build())
                .addComponents(ActionRow.of(
                        EntitySelectMenu.create("setup:channel:whitelist", EntitySelectMenu.SelectTarget.CHANNEL)
                                .setChannelTypes(ChannelType.TEXT)
                                .setPlaceholder("Pick the whitelist channel")
                                .build()
                ))
                .setEphemeral(true).queue();
    }

    // ── Entity select (channels & roles) ─────────────────────────────────────

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        String id = event.getComponentId();
        String userId = event.getUser().getId();
        SetupSession session = sessions.get(userId);
        if (session == null) return;

        switch (id) {
            case "setup:channel:whitelist" -> {
                session.whitelistChannelId = event.getMentions().getChannels().get(0).getId();
                event.editMessageEmbeds(stepEmbed("Step 2 / 5 — Log Channel",
                        "Select the channel where every whitelist action (submitted, approved, denied, revoked) is logged.",
                        2).build())
                        .setComponents(ActionRow.of(
                                EntitySelectMenu.create("setup:channel:log", EntitySelectMenu.SelectTarget.CHANNEL)
                                        .setChannelTypes(ChannelType.TEXT)
                                        .setPlaceholder("Pick the log channel")
                                        .build()
                        )).queue();
            }
            case "setup:channel:log" -> {
                session.logChannelId = event.getMentions().getChannels().get(0).getId();
                event.editMessageEmbeds(stepEmbed("Step 3 / 5 — Queue Channel",
                        "Select the channel where pending whitelist requests are posted for admins to approve or deny.",
                        3).build())
                        .setComponents(ActionRow.of(
                                EntitySelectMenu.create("setup:channel:queue", EntitySelectMenu.SelectTarget.CHANNEL)
                                        .setChannelTypes(ChannelType.TEXT)
                                        .setPlaceholder("Pick the queue channel")
                                        .build()
                        )).queue();
            }
            case "setup:channel:queue" -> {
                session.queueChannelId = event.getMentions().getChannels().get(0).getId();
                event.editMessageEmbeds(stepEmbed("Step 4 / 5 — Whitelist Role",
                        "Select the role that is assigned to users when they are approved.\n" +
                        "The bot must have a role **higher** than this role in the server hierarchy.",
                        4).build())
                        .setComponents(ActionRow.of(
                                EntitySelectMenu.create("setup:role:whitelist", EntitySelectMenu.SelectTarget.ROLE)
                                        .setPlaceholder("Pick the whitelist role")
                                        .build()
                        )).queue();
            }
            case "setup:role:whitelist" -> {
                session.whitelistRoleId = event.getMentions().getRoles().get(0).getId();
                event.editMessageEmbeds(stepEmbed("Step 5 / 5 — Admin Role",
                        "Select the role whose members can approve, deny, and revoke whitelist requests.",
                        5).build())
                        .setComponents(ActionRow.of(
                                EntitySelectMenu.create("setup:role:admin", EntitySelectMenu.SelectTarget.ROLE)
                                        .setPlaceholder("Pick the admin role")
                                        .build()
                        )).queue();
            }
            case "setup:role:admin" -> {
                session.adminRoleId = event.getMentions().getRoles().get(0).getId();
                // Optional: pick an authorized super-admin user
                event.editMessageEmbeds(stepEmbed("Optional — Authorized Super-Admin User",
                        "Select a Discord user who can always run admin commands regardless of role.\n" +
                        "Skip this step by selecting yourself or any placeholder; you can change it later in config.yml.\n\n" +
                        "**Or** click **Save Config** below to finish now.",
                        0).build())
                        .setComponents(
                                ActionRow.of(
                                        EntitySelectMenu.create("setup:user:admin", EntitySelectMenu.SelectTarget.USER)
                                                .setPlaceholder("Pick an authorized user (optional)")
                                                .build()
                                ),
                                ActionRow.of(
                                        net.dv8tion.jda.api.interactions.components.buttons.Button.success("setup:save", "💾 Save Config")
                                )
                        ).queue();
            }
            case "setup:user:admin" -> {
                session.authorizedUserId = event.getMentions().getUsers().get(0).getId();
                saveAndConfirm(event, session, userId);
            }
        }
    }

    // ── Button: Save ──────────────────────────────────────────────────────────

    @Override
    public void onButtonInteraction(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event) {
        if (!event.getComponentId().equals("setup:save")) return;
        String userId = event.getUser().getId();
        SetupSession session = sessions.get(userId);
        if (session == null) {
            event.reply("No active setup session. Run /whitelist-setup again.").setEphemeral(true).queue();
            return;
        }
        saveAndConfirm(event, session, userId);
    }

    // ── Save to config.yml ────────────────────────────────────────────────────

    private void saveAndConfirm(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event,
                                SetupSession session, String userId) {
        sessions.remove(userId);

        FileConfiguration cfg = plugin.getConfig();
        cfg.set("discord.guild-id", session.guildId);
        cfg.set("discord.channels.whitelist", session.whitelistChannelId);
        cfg.set("discord.channels.whitelist-log", session.logChannelId);
        cfg.set("discord.channels.whitelist-queue", session.queueChannelId);
        cfg.set("discord.roles.whitelist", session.whitelistRoleId);
        cfg.set("discord.roles.whitelist-admin", session.adminRoleId);
        if (session.authorizedUserId != null && !session.authorizedUserId.isBlank()) {
            cfg.set("discord.authorized-user-id", session.authorizedUserId);
        }
        plugin.saveConfig();
        plugin.reloadConfig();

        EmbedBuilder done = new EmbedBuilder()
                .setTitle("✅ DCbridge Setup Complete")
                .setColor(Color.GREEN)
                .setDescription("All Discord IDs have been saved to `config.yml`. The bot is now fully configured.")
                .addField("Guild ID", "`" + session.guildId + "`", false)
                .addField("Whitelist Channel", "<#" + session.whitelistChannelId + ">", true)
                .addField("Log Channel", "<#" + session.logChannelId + ">", true)
                .addField("Queue Channel", "<#" + session.queueChannelId + ">", true)
                .addField("Whitelist Role", "<@&" + session.whitelistRoleId + ">", true)
                .addField("Admin Role", "<@&" + session.adminRoleId + ">", true)
                .addField("Authorized User", session.authorizedUserId != null && !session.authorizedUserId.isBlank()
                        ? "<@" + session.authorizedUserId + ">" : "_not set_", true)
                .addField("Next step", "Run `/whitelist settings` to configure auto-accept and one-request-per-user. The whitelist verification embed will be posted automatically in the configured whitelist channel.", false)
                .setFooter("DCbridge — config saved");

        if (event instanceof net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent btn) {
            btn.editMessageEmbeds(done.build()).setComponents().queue();
        } else if (event instanceof EntitySelectInteractionEvent sel) {
            sel.editMessageEmbeds(done.build()).setComponents().queue();
        }

        plugin.getLogger().info("[DCbridge] Setup complete. Config saved by Discord user " + userId);

        // Auto-post the verification embed in the whitelist channel
        postVerificationEmbed(event, session.whitelistChannelId);
    }

    // ── Auto-post verification embed ─────────────────────────────────────────

    private void postVerificationEmbed(IReplyCallback event, String whitelistChannelId) {
        if (whitelistChannelId == null || whitelistChannelId.isBlank()) return;
        var jda = dev.dcbridge.dcbridge.bot.DiscordManagerHolder.getJda();
        if (jda == null) return;
        TextChannel channel = jda.getTextChannelById(whitelistChannelId);
        if (channel == null) {
            plugin.getLogger().warning("[DCbridge] Could not find whitelist channel " + whitelistChannelId + " to post verification embed.");
            return;
        }
        java.awt.Color embedColor;
        try {
            embedColor = java.awt.Color.decode("#b6cdff");
        } catch (Exception e) {
            embedColor = java.awt.Color.GREEN;
        }
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(config.getEmbedTitle())
                .setDescription(config.getEmbedDescription())
                .setColor(embedColor)
                .setFooter(config.getEmbedFooter());
        channel.sendMessageEmbeds(embed.build())
                .addActionRow(Button.primary("verify:open", config.getVerifyButtonLabel()))
                .queue(
                        msg -> plugin.getLogger().info("[DCbridge] Verification embed posted in channel " + whitelistChannelId),
                        err -> plugin.getLogger().warning("[DCbridge] Failed to post verification embed: " + err.getMessage())
                );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EmbedBuilder stepEmbed(String title, String description, int step) {
        EmbedBuilder b = new EmbedBuilder()
                .setTitle("⚙️ " + title)
                .setDescription(description)
                .setColor(Color.decode("#b6cdff"))
                .setFooter("DCbridge Setup Wizard" + (step > 0 ? " • Step " + step + " of 5" : ""));
        return b;
    }
}
