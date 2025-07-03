package org.retrohaven.beta.discordchatbridge;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import org.bukkit.Bukkit;

public class StaffMessagingRelayAPI {
    private static String staffChannelId;
    private static JDA jda;

    public static void init(JDA jdaInstance, String channelId) {
        jda = jdaInstance;
        staffChannelId = channelId;
    }

    public static void sendStaffMessage(String message) {
        if (staffChannelId == null || staffChannelId.isEmpty()) {
            Bukkit.getLogger().warning("[DiscordChatBridge] Staff channel ID not set. Staff message not sent.");
            return;
        }
        if (jda == null) {
            Bukkit.getLogger().warning("[DiscordChatBridge] JDA is not initialized. Staff message not sent.");
            return;
        }

        TextChannel channel = jda.getTextChannelById(staffChannelId);
        if (channel == null) {
            Bukkit.getLogger().warning("[DiscordChatBridge] Staff channel ID invalid: " + staffChannelId);
            return;
        }

        channel.sendMessage(message).queue(
                success -> {},
                error -> Bukkit.getLogger().warning("[DiscordChatBridge] Failed to send staff message: " + error.getMessage())
        );
    }
}
