package org.retrohaven.beta.discordchatbridge;

import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

import org.retrohaven.beta.discordchatbridge.DiscordChatBridge;

import java.io.IOException;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class DCBGameListener extends PlayerListener {
    private DiscordChatBridge plugin;

    public DCBGameListener(DiscordChatBridge plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent event) {
        String chatMessage = plugin.getConfig().getConfigString("message.join-message");
        chatMessage = chatMessage.replace("%username%", event.getPlayer().getName());
        chatMessage = chatMessage.replace("%onlineCount%", String.valueOf(Bukkit.getServer().getOnlinePlayers().length));
        chatMessage = chatMessage.replace("%maxCount%", String.valueOf(Bukkit.getServer().getMaxPlayers()));
        plugin.getDiscordCore().getDiscordBot().discordSendToChannel(plugin.getConfig().getConfigString("channel-id"), chatMessage);

        // Relay to external bot via relay server if enabled
        if (plugin.isRelayServerEnabled() && plugin.getRelayServer() != null) {
            String relayMessage = "GAME_JOIN " + event.getPlayer().getName();
            plugin.getRelayServer().broadcast(relayMessage);
        }
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        String chatMessage = plugin.getConfig().getConfigString("message.quit-message");
        chatMessage = chatMessage.replace("%username%", event.getPlayer().getName());
        chatMessage = chatMessage.replace("%onlineCount%", String.valueOf(Bukkit.getServer().getOnlinePlayers().length - 1));
        chatMessage = chatMessage.replace("%maxCount%", String.valueOf(Bukkit.getServer().getMaxPlayers()));
        plugin.getDiscordCore().getDiscordBot().discordSendToChannel(plugin.getConfig().getConfigString("channel-id"), chatMessage);

        // Relay to external bot via relay server if enabled
        if (plugin.isRelayServerEnabled() && plugin.getRelayServer() != null) {
            String relayMessage = "GAME_QUIT " + event.getPlayer().getName();
            plugin.getRelayServer().broadcast(relayMessage);
        }
    }

    @Override
    public void onPlayerChat(PlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        String message = event.getMessage();
        boolean suppressRelay = false;

        // Check if message starts with "# " (command that should not be relayed)
        if (message.startsWith("# ")) {
            suppressRelay = true;
            message = message.substring(2); // Remove "# " prefix
        }

        // Check if message is an IRC command (starts with !)
        if (message.startsWith("!")) {
            // Send IRC command to relay server
            if (plugin.isRelayServerEnabled() && plugin.getRelayServer() != null) {
                String relayMessage = "IRC_CMD GAME " + event.getPlayer().getName() + " " + message;
                plugin.getRelayServer().broadcast(relayMessage);

                // If suppressed, don't relay to Discord/game chat
                if (suppressRelay) {
                    event.setCancelled(true);
                }
                return;
            }
        }

        // If message was suppressed (started with "# "), cancel it from being relayed
        if (suppressRelay) {
            event.setCancelled(true);
            return;
        }

        if (plugin.getConfig().getConfigBoolean("webhook.use-webhook")) {
            final DiscordWebhook webhookMessage = new DiscordWebhook(plugin.getConfig().getConfigString("webhook.url"));
            webhookMessage.setUsername(event.getPlayer().getName());
            webhookMessage.setContent(sanitizeMessage(event.getMessage()));
            webhookMessage.setAvatarUrl("http://minotar.net/helm/" + event.getPlayer().getName() + "/100.png");
            webhookMessage.setTts(false);
            Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, () -> {
                try {
                    webhookMessage.execute();
                } catch (IOException exception) {
                    plugin.logger(Level.INFO, "Failed to send message through webhook to Discord chat channel: " + exception + " : " + exception.getMessage());
                }
            }, 0L);

        } else {
            String chatMessage = plugin.getConfig().getConfigString("message.game-chat-message");
            chatMessage = chatMessage.replace("%messageAuthor%", event.getPlayer().getName());
            chatMessage = chatMessage.replace("%message%", event.getMessage());
            chatMessage = sanitizeMessage(chatMessage);
            plugin.getDiscordCore().getDiscordBot().discordSendToChannel(plugin.getConfig().getConfigString("channel-id"), chatMessage);
        }

        // Relay to external bot via relay server if enabled
        if (plugin.isRelayServerEnabled() && plugin.getRelayServer() != null) {
            String relayMessage = "GAME_CHAT " + event.getPlayer().getName() + " " + event.getMessage();
            plugin.getRelayServer().broadcast(relayMessage);
        }
    }

    public String sanitizeMessage(String chatMessage) {
        chatMessage = chatMessage.replaceAll(Pattern.quote("@"), "@ ");
        chatMessage = chatMessage.replaceAll(Pattern.quote("@everyone"), "@ everyone");
        chatMessage = chatMessage.replaceAll(Pattern.quote("@here"), "@ here");
        return chatMessage;
    }

}
