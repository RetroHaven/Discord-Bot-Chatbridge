package org.retrohaven.beta.discordchatbridge;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Staff Messaging Relay API for Discord Chat Bridge
 *
 * This API allows other plugins to:
 * - Send pre-formatted staff messages to Discord
 * - Send pre-formatted staff messages to in-game staff
 * - Listen for staff messages from Discord
 * - Check staff permissions
 *
 * Note: Message formatting is now handled by the calling plugin for better flexibility.
 * The API simply relays the formatted messages to Discord or in-game recipients.
 *
 * Usage example:
 * StaffMessagingRelayAPI.sendMessageToDiscord("**[Staff Chat]** AdminName: Hello staff team!");
 * StaffMessagingRelayAPI.sendMessageToGameStaff(ChatColor.GOLD + "[Staff] " + ChatColor.WHITE + "Message content");
 *
 */
public class StaffMessagingRelayAPI {
    private static String staffChannelId;
    private static JDA jda;
    private static List<StaffMessageListener> listeners = new ArrayList<>();

    public static void init(JDA jdaInstance, String channelId) {
        jda = jdaInstance;
        staffChannelId = channelId;
    }

    /**
     * Send a pre-formatted message to the Discord staff channel
     * @param formattedMessage The already formatted message to send to Discord
     */
    public static void sendMessageToDiscord(String formattedMessage) {
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

        channel.sendMessage(formattedMessage).queue(
                success -> {},
                error -> Bukkit.getLogger().warning("[DiscordChatBridge] Failed to send staff message: " + error.getMessage())
        );
    }

    /**
     * Send a pre-formatted message to all online staff members in-game
     * @param formattedMessage The already formatted message to send to staff
     */
    public static void sendMessageToGameStaff(String formattedMessage) {
        // Send to all online players with staff permission
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasStaffPermission(player)) {
                player.sendMessage(formattedMessage);
            }
        }

        // Also log to console (strip color codes for console)
        String consoleMessage = ChatColor.stripColor(formattedMessage);
        Bukkit.getLogger().info("[Staff Message] " + consoleMessage);
    }

    /**
     * Legacy method for backward compatibility - sends a staff chat message
     * @param playerName The name of the staff member sending the message
     * @param message The message to send to Discord
     * @deprecated Use sendMessageToDiscord with pre-formatted message instead
     */
    @Deprecated
    public static void sendStaffChatToDiscord(String playerName, String message) {
        String formattedMessage = "**[Staff Chat]** " + playerName + ": " + message;
        sendMessageToDiscord(formattedMessage);
    }

    /**
     * Legacy method for backward compatibility - sends a staff notice
     * @param message The notice message to send to Discord
     * @deprecated Use sendMessageToDiscord with pre-formatted message instead
     */
    @Deprecated
    public static void sendStaffNoticeToDiscord(String message) {
        String formattedMessage = "**[Staff Notice]** " + message;
        sendMessageToDiscord(formattedMessage);
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use sendMessageToDiscord with pre-formatted message instead
     */
    @Deprecated
    public static void sendStaffMessageToDiscord(String message) {
        sendStaffNoticeToDiscord(message);
    }

    /**
     * Register a listener to receive staff messages from Discord
     * This allows plugins like RH-Commands to handle Discord staff messages
     * @param listener The listener to register
     */
    public static void registerStaffMessageListener(StaffMessageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            Bukkit.getLogger().info("[DiscordChatBridge] Registered staff message listener: " + listener.getClass().getSimpleName());
        }
    }

    /**
     * Unregister a staff message listener
     * @param listener The listener to unregister
     */
    public static void unregisterStaffMessageListener(StaffMessageListener listener) {
        listeners.remove(listener);
        Bukkit.getLogger().info("[DiscordChatBridge] Unregistered staff message listener: " + listener.getClass().getSimpleName());
    }

    /**
     * Legacy method for backward compatibility - sends Discord message to staff with default formatting
     * @param discordUsername The Discord username of the sender
     * @param message The message content
     * @deprecated Use sendMessageToGameStaff with pre-formatted message instead
     */
    @Deprecated
    public static void sendStaffMessageToGame(String discordUsername, String message) {
        String formattedMessage = ChatColor.GOLD + "[Discord Staff Chat] " + ChatColor.WHITE + discordUsername + ": " + message;
        sendMessageToGameStaff(formattedMessage);
    }

    /**
     * Called internally when a message is received from Discord staff channel
     * This notifies all registered listeners and sends to staff in-game with default formatting
     * @param discordUsername The Discord username of the sender
     * @param message The message content
     */
    public static void notifyStaffMessageFromDiscord(String discordUsername, String message) {
        StaffMessage staffMessage = new StaffMessage(discordUsername, message);

        // Send to staff members in-game with default formatting (for backward compatibility)
        sendStaffMessageToGame(discordUsername, message);

        // Notify registered listeners
        for (StaffMessageListener listener : listeners) {
            try {
                listener.onStaffMessageFromDiscord(staffMessage);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[DiscordChatBridge] Error in staff message listener " +
                    listener.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Get the staff channel ID
     * @return The Discord staff channel ID
     */
    public static String getStaffChannelId() {
        return staffChannelId;
    }

    /**
     * Check if a player has staff permission
     * @param player The player to check
     * @return true if the player has staff permission
     */
    public static boolean hasStaffPermission(Player player) {
        // Check for various staff permissions
        return player.isOp() ||
               player.hasPermission("STAFF_CHAT") ||
                player.hasPermission("STAFF_RECEIVE") ||
               player.hasPermission("STAFF");
    }

    /**
     * Check if the staff messaging system is properly initialized
     * @return true if initialized and ready to use
     */
    public static boolean isInitialized() {
        return jda != null && staffChannelId != null && !staffChannelId.isEmpty();
    }

    /**
     * Get the number of online staff members
     * @return The count of online staff members
     */
    public static int getOnlineStaffCount() {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasStaffPermission(player)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get a list of online staff members
     * @return List of online staff member names
     */
    public static List<String> getOnlineStaffNames() {
        List<String> staffNames = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasStaffPermission(player)) {
                staffNames.add(player.getName());
            }
        }
        return staffNames;
    }

    /**
     * Interface for plugins to implement to receive staff messages from Discord
     */
    public interface StaffMessageListener {
        /**
         * Called when a staff message is received from Discord
         * @param message The staff message containing username and content
         */
        void onStaffMessageFromDiscord(StaffMessage message);
    }

    /**
     * Enum representing different types of staff messages
     */
    public enum StaffMessageType {
        CHAT,    // Regular staff chat message
        NOTICE   // Staff notice/announcement
    }

    /**
     * Represents a staff message from Discord
     */
    public static class StaffMessage {
        private final String discordUsername;
        private final String message;
        private final StaffMessageType type;

        public StaffMessage(String discordUsername, String message) {
            this(discordUsername, message, StaffMessageType.CHAT);
        }

        public StaffMessage(String discordUsername, String message, StaffMessageType type) {
            this.discordUsername = discordUsername;
            this.message = message;
            this.type = type;
        }

        /**
         * Get the Discord username of the sender
         * @return The Discord username
         */
        public String getDiscordUsername() {
            return discordUsername;
        }

        /**
         * Get the message content
         * @return The message content
         */
        public String getMessage() {
            return message;
        }

        /**
         * Get the message type
         * @return The message type
         */
        public StaffMessageType getType() {
            return type;
        }
    }
}
