package org.retrohaven.beta.discordchatbridge;

import org.bukkit.Bukkit;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerListener;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DCBBanListener extends PlayerListener {
    private final DiscordChatBridge plugin;
    private static final Pattern BAN_PATTERN = Pattern.compile("^/?(?:apollo:)?ban\\s+(\\S+)(?:\\s+(.*))?$", Pattern.CASE_INSENSITIVE);
    private final AtomicInteger banCounter = new AtomicInteger(0);

    // Cache channel ID to avoid repeated lookups
    private volatile String channelId;

    public DCBBanListener(DiscordChatBridge plugin) {
        this.plugin = plugin;
        refreshConfig();
    }

    public void refreshConfig() {
        this.channelId = plugin.getConfig().getConfigString("channel-id");
    }

    @Override
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled() || !plugin.getConfig().getConfigBoolean("ban-relay.enabled")) {
            return;
        }

        String command = event.getMessage();

        // Quick check to avoid regex if it's clearly not a ban command
        if (!command.contains("ban")) {
            return;
        }

        Matcher banMatcher = BAN_PATTERN.matcher(command);
        if (banMatcher.matches()) {
            String bannedPlayer = banMatcher.group(1);
            String reason = banMatcher.group(2);

            // Use empty check instead of null + trim for better performance
            if (reason == null || reason.isEmpty()) {
                reason = "No reason specified";
            } else {
                reason = reason.trim();
            }

            sendBanMessage(bannedPlayer, reason);
        }
    }

    // this ban thing here can't get the current ban number from the apollo database might fix later not sure

    private void sendBanMessage(String bannedPlayer, String reason) {
        final int currentBanNumber = banCounter.incrementAndGet();
        final String message = "**" + bannedPlayer + "** has been bonked by the ban hammer! (" + currentBanNumber + ")";

        // Send async to avoid blocking game thread
        Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, () -> {
            plugin.getDiscordCore().getDiscordBot().discordSendToChannel(channelId, message);
        }, 0L);
    }

    // Inner class for console commands
    public static class DCBBanServerListener extends ServerListener {
        private final DiscordChatBridge plugin;
        private final DCBBanListener parentListener;

        public DCBBanServerListener(DiscordChatBridge plugin, DCBBanListener parentListener) {
            this.plugin = plugin;
            this.parentListener = parentListener;
        }

        @Override
        public void onServerCommand(ServerCommandEvent event) {
            if (!plugin.getConfig().getConfigBoolean("ban-relay.enabled")) {
                return;
            }

            String command = event.getCommand();

            // Quick check to avoid regex if it's clearly not a ban command
            if (!command.contains("ban")) {
                return;
            }

            Matcher banMatcher = BAN_PATTERN.matcher(command);
            if (banMatcher.matches()) {
                String bannedPlayer = banMatcher.group(1);
                String reason = banMatcher.group(2);

                if (reason == null || reason.isEmpty()) {
                    reason = "No reason specified";
                } else {
                    reason = reason.trim();
                }

                parentListener.sendBanMessage(bannedPlayer, reason);
            }
        }
    }
}
