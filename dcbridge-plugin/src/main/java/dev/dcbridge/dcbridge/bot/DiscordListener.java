package dev.dcbridge.dcbridge.bot;

import dev.dcbridge.dcbridge.config.BotConfig;
import dev.dcbridge.dcbridge.whitelist.WhitelistManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
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

    public DiscordListener(JavaPlugin plugin, BotConfig config, WhitelistManager whitelistManager, DiscordManager discordManager) {
        this.plugin = plugin;
        this.config = config;
        this.whitelistManager = whitelistManager;
    }

    @Override
    public void onReady(ReadyEvent event) {
        registerSlashCommands(event);
    }

    private void registerSlashCommands(ReadyEvent event) {
        SlashCommandData whitelistSetup = Commands.slash("whitelist-setup", "Post the whitelist verification embed");
        SlashCommandData status = Commands.slash("status", "Manage the live status embed")
                .addOption(OptionType.STRING, "action", "setup or remove", true)
                .addOption(OptionType.CHANNEL, "channel", "Channel for the status embed", false);
        event.getJDA().updateCommands().addCommands(whitelistSetup, status).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("whitelist-setup")) {
            handleWhitelistSetup(event);
        } else if (event.getName().equals("status")) {
            handleStatus(event);
        }
    }

    private void handleWhitelistSetup(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.reply("This command must be used in a server.").setEphemeral(true).queue();
            return;
        }
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(config.getEmbedTitle())
                .setDescription(config.getEmbedDescription())
                .setColor(Color.decode("#" + config.getColorOnline()))
                .setFooter(config.getEmbedFooter());
        event.replyEmbeds(builder.build())
                .addActionRow(Button.primary("verify:open", config.getVerifyButtonLabel()))
                .setEphemeral(true)
                .queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("verify:open")) {
            openModal(event);
        } else if (event.getComponentId().startsWith("whitelist:review:")) {
            handleReview(event);
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
        String requestId = whitelistManager.submitRequest(event.getUser().getId(), event.getGuild().getId(), username, platform);
        plugin.getLogger().info("New whitelist submission " + requestId + " for " + username);

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Whitelist Request")
                .setDescription("User " + event.getUser().getAsTag() + " requested whitelist for " + username + " on " + platform)
                .addField("Username", username, true)
                .addField("Platform", platform, true)
                .setColor(Color.GREEN)
                .setTimestamp(Instant.now());

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
                                Button.danger(reviewId + ":cancel", config.getCancelButtonLabel())
                        )
                        .queue(message -> whitelistManager.setQueueMessageId(requestId, message.getId()));
            }
        }

        event.reply("Your request has been submitted for review.").setEphemeral(true).queue();
    }

    private void handleStatus(SlashCommandInteractionEvent event) {
        event.reply("Status embed management is enabled.").setEphemeral(true).queue();
    }

    private void handleReview(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        String requestId = componentId.substring(componentId.indexOf(':', componentId.indexOf(':') + 1) + 1);
        String action = componentId.substring(componentId.lastIndexOf(':') + 1);
        if (action.equals("approve")) {
            whitelistManager.handleApproval(event.getUser().getId(), event.getGuild().getId(), "", "", requestId);
            event.reply("Request approved.").setEphemeral(true).queue();
        } else {
            whitelistManager.handleCancellation(requestId, event.getUser().getId());
            event.reply("Request cancelled.").setEphemeral(true).queue();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.getMessage().getMentions().getUsers().contains(event.getJDA().getSelfUser())) {
            String content = event.getMessage().getContentRaw();
            if (content.contains("remove") || content.contains("delete")) {
                event.getMessage().reply("Admin tool acknowledged. Remove request received.").queue();
            }
        }
    }
}
