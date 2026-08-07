package dev.dcbridge.dcbridge.bot;

import net.dv8tion.jda.api.JDA;

public class DiscordManagerHolder {
    private static JDA jda;

    public static void setJda(JDA jda) {
        DiscordManagerHolder.jda = jda;
    }

    public static JDA getJda() {
        return jda;
    }
}
