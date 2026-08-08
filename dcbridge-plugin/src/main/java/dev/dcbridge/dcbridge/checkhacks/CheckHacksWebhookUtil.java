package dev.dcbridge.dcbridge.checkhacks;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class CheckHacksWebhookUtil {

    public static void send(String webhookUrl, String template, String playerName, String reason, String details) {
        if (webhookUrl == null || webhookUrl.isBlank() || webhookUrl.contains("CHANGE_ME")) {
            return;
        }

        String description = template
                .replace("{player}", playerName)
                .replace("{reason}", reason)
                .replace("{details}", details);

        String json = "{\"embeds\":[{"
                + "\"title\":\"⚠️ DCbridge — CheckHacks Alert\","
                + "\"description\":\"" + escapeJson(description) + "\","
                + "\"color\":16744272,"  // 0xFF6810 — orange-red, less harsh than pure red
                + "\"footer\":{\"text\":\"DCbridge CheckHacks\"},"
                + "\"timestamp\":\"" + Instant.now() + "\""
                + "}]}";

        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) URI.create(webhookUrl).toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("User-Agent", "DCbridge/CheckHacks");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(json.getBytes(StandardCharsets.UTF_8));
                }
                connection.disconnect();
            } catch (Exception ignored) {
                // Ignore webhook failures so the plugin remains stable.
            }
        }, "dcbridge-checkhacks-webhook").start();
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}