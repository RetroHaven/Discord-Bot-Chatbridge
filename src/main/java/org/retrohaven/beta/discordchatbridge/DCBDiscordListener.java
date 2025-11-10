package org.retrohaven.beta.discordchatbridge;

import com.johnymuffin.beta.discordauth.DiscordAuthentication;
import com.johnymuffin.jperms.beta.JohnyPerms;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

public class DCBDiscordListener extends ListenerAdapter {
    private DiscordChatBridge plugin;
    private DCBCommandHandler commandHandler;

    public DCBDiscordListener(DiscordChatBridge plugin, DCBCommandHandler commandHandler) {
        this.plugin = plugin;
        this.commandHandler = commandHandler;
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        //Don't respond to bots
        if (event.getAuthor().isBot() || event.isWebhookMessage()) {
            return;
        }
        //Don't respond to funky messages
        if (event.getMessage().getContentRaw().isEmpty()) {
            return;
        }
        //Ignore messages starting with #, but not the ones using markdown titles
        if (event.getMessage().getContentRaw().startsWith("#") && !event.getMessage().getContentRaw().substring(1).startsWith(" ")) {
            return;
        }

        String gameBridgeChannelID = plugin.getConfig().getConfigString("channel-id");
        String[] messageCMD = event.getMessage().getContentRaw().split(" ");
        String response;

        // Command handling
        if (messageCMD[0].startsWith("!") ) {
            if (plugin.getConfig().getConfigBoolean("bot-command-channel-enabled")) {
                //Does it match?
                if (Objects.equals(plugin.getConfig().getConfigString("bot-command-channel-id"), event.getChannel().getId())) {
                    response = commandHandler.onCommand(messageCMD[0].substring(1), messageCMD);
                }

            } else {

            }
            if (messageCMD[0].equalsIgnoreCase("!online") && plugin.getConfig().getConfigBoolean("online-command-enabled")) {

        }

            //Check for if its enabled.
            if (plugin.getConfig().getConfigBoolean("bot-command-channel-enabled")) {
                //Does it match?
                if (Objects.equals(plugin.getConfig().getConfigString("bot-command-channel-id"), event.getChannel().getId())) {
                    //begin Online Command Response
                    String onlineMessage = "**The online players are:** ";
                    for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                        onlineMessage += p.getName() + ", ";
                    }
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle(plugin.getConfig().getConfigString("server-name") + " Online Players", null);
                    if (Bukkit.getServer().getOnlinePlayers().length > 0) {
                        int rnd = new Random().nextInt(Bukkit.getServer().getOnlinePlayers().length);
                        Player player = Bukkit.getServer().getOnlinePlayers()[rnd];
                        eb.setThumbnail("http://minotar.net/helm/" + player.getName() + "/100.png");
                    }
                    eb.setColor(Color.red);
                    eb.setDescription("There are currently **" + Bukkit.getServer().getOnlinePlayers().length
                            + "** players online\n" + onlineMessage);
                    eb.setFooter("https://github.com/RhysB/Discord-Bot-Chatbridge",
                            "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png");

                    event.getChannel().sendMessage(eb.build()).queue();
                    return;
                }
                if (plugin.getConfig().getConfigString("bot-command-channel-id").isEmpty() || Objects.equals(plugin.getConfig().getConfigString("bot-command-channel-id"), "id")) {
                    Bukkit.getLogger().warning("You appear to have forgotten to add a channel ID. go to the config and add an ID or disable the bot command channel limiter");
                    Bukkit.getLogger().info("Will proceed like the feature is disabled.");
                    //begin Online Command Response
                    String onlineMessage = "**The online players are:** ";
                    for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                        onlineMessage += p.getName() + ", ";
                    }
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle(plugin.getConfig().getConfigString("server-name") + " Online Players", null);
                    if (Bukkit.getServer().getOnlinePlayers().length > 0) {
                        int rnd = new Random().nextInt(Bukkit.getServer().getOnlinePlayers().length);
                        Player player = Bukkit.getServer().getOnlinePlayers()[rnd];
                        eb.setThumbnail("http://minotar.net/helm/" + player.getName() + "/100.png");
                    }
                    eb.setColor(Color.red);
                    eb.setDescription("There are currently **" + Bukkit.getServer().getOnlinePlayers().length
                            + "** players online\n" + onlineMessage);
                    eb.setFooter("https://github.com/RhysB/Discord-Bot-Chatbridge",
                            "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png");

                    event.getChannel().sendMessage(eb.build()).queue();
                    return;
                }

            }
            //Check for if it's not enabled
            if (!plugin.getConfig().getConfigBoolean("bot-command-channel-enabled")) {
                //begin Online Command Response
                String onlineMessage = "**The online players are:** ";
                for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                    onlineMessage += p.getName() + ", ";
                }
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle(plugin.getConfig().getConfigString("server-name") + " Online Players", null);
                if (Bukkit.getServer().getOnlinePlayers().length > 0) {
                    int rnd = new Random().nextInt(Bukkit.getServer().getOnlinePlayers().length);
                    Player player = Bukkit.getServer().getOnlinePlayers()[rnd];
                    eb.setThumbnail("http://minotar.net/helm/" + player.getName() + "/100.png");
                }
                eb.setColor(Color.red);
                eb.setDescription("There are currently **" + Bukkit.getServer().getOnlinePlayers().length
                        + "** players online\n" + onlineMessage);
                eb.setFooter("https://github.com/RhysB/Discord-Bot-Chatbridge",
                        "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png");

                event.getChannel().sendMessage(eb.build()).queue();
                return;
            }
        }

        //Check if message is in staff messaging channel
        String staffChannelId = StaffMessagingRelayAPI.getStaffChannelId();
        if (staffChannelId != null && !staffChannelId.isEmpty() &&
            event.getChannel().getId().equalsIgnoreCase(staffChannelId)) {

            String discordUsername;
            if (event.getMember().getNickname() != null) {
                discordUsername = event.getMember().getNickname();
            } else {
                discordUsername = event.getAuthor().getName();
            }

            String message = event.getMessage().getContentDisplay();
            message = ChatColor.stripColor(message); // Remove any color codes for staff messages

            // Notify all registered listeners (like RH-Commands) about the staff message
            StaffMessagingRelayAPI.notifyStaffMessageFromDiscord(discordUsername, message);
            return;
        }

        //Is the message in the game bridge channel
        if (event.getChannel().getId().equalsIgnoreCase(gameBridgeChannelID)) {
            String rawMessage = event.getMessage().getContentRaw();
            boolean suppressRelay = false;

            // Check if message starts with "# " (command that should not be relayed)
            if (rawMessage.startsWith("# ")) {
                suppressRelay = true;
                rawMessage = rawMessage.substring(2); // Remove "# " prefix
            }

            // Check if message is an IRC command (starts with !)
            if (rawMessage.startsWith("!")) {
                // Send IRC command to relay server
                if (plugin.isRelayServerEnabled() && plugin.getRelayServer() != null) {
                    String displayName;
                    if (event.getMember().getNickname() != null) {
                        displayName = event.getMember().getNickname();
                    } else {
                        displayName = event.getAuthor().getName();
                    }

                    String relayMessage = "IRC_CMD DISCORD " + displayName + " " + rawMessage;
                    plugin.getRelayServer().broadcast(relayMessage);

                    // If not suppressed, still broadcast to game
                    if (!suppressRelay) {
                        final String finalDisplayName = displayName;
                        final String finalRawMessage = rawMessage;
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                            public void run() {
                                String chatMessage = "§f[§9Discord§f]§7 " + finalDisplayName + ": " + finalRawMessage;
                                Bukkit.getServer().broadcastMessage(chatMessage);
                            }
                        });
                    }
                }
                return;
            }

            // If message was suppressed (started with "# "), don't relay to game
            if (suppressRelay) {
                return;
            }

            String displayName = null;
            String prefix = null;
            UUID playerUUID = null;

            if (plugin.getConfig().getConfigBoolean("authentication.enabled")) {
                DiscordAuthentication authPlugin = (DiscordAuthentication) Bukkit.getServer().getPluginManager().getPlugin("DiscordAuthentication");

                //Get playerUUID from DiscordID if possible
                if(authPlugin.getData().isDiscordIDAlreadyLinked(event.getAuthor().getId())) {
                    playerUUID = UUID.fromString(authPlugin.getData().getUUIDFromDiscordID(event.getAuthor().getId()));
                }

                if (plugin.getConfig().getConfigBoolean("authentication.discord.only-allow-linked-users")) {
                    if (!authPlugin.getData().isDiscordIDAlreadyLinked(event.getAuthor().getId())) {
                        event.getChannel().sendMessage(plugin.getConfig().getString("message.require-link")).queue();
                        return;
                    }
                }
                if (plugin.getConfig().getConfigBoolean("authentication.discord.use-in-game-names-if-available")) {
                    displayName = authPlugin.getData().getLastUsernameFromDiscordID(event.getAuthor().getId());
                }
            }

            //Check for prefix
            if (this.plugin.getConfig().getConfigBoolean("johnyperms-prefix-support.enabled")) {
                if (playerUUID != null) {
                    if(Bukkit.getPluginManager().isPluginEnabled("JPerms")) {
                        JohnyPerms jperms = (JohnyPerms) Bukkit.getServer().getPluginManager().getPlugin("JPerms");
                        //Attempt to get prefix from JohnyPerms for user then group
                        prefix = jperms.getUser(playerUUID).getPrefix();
                        if(prefix == null) {
                            prefix = jperms.getUser(playerUUID).getGroup().getPrefix();
                        }
                    } else {
                        this.plugin.logger(Level.WARNING, "JohnyPerms prefix support is enabled but the plugin is not installed or enabled.");
                    }
                } else {
                    this.plugin.logger(Level.WARNING, "JohnyPerms prefix support is enabled but the player UUID is null. This is likely due to the DiscordAuthentication plugin not being installed or enabled.");
                }
            }

            //Reimplemented from f0f832
            String dmsg = event.getMessage().getContentDisplay();
            dmsg = dmsg.replaceAll("(&([a-f0-9]))", "\u00A7$2");
            if (!plugin.getConfig().getConfigBoolean("message.allow-chat-colors")) {
                dmsg = ChatColor.stripColor(dmsg);
            }

            if (displayName == null) {
                if (event.getMember().getNickname() != null) {
                    displayName = event.getMember().getNickname();
                } else {
                    displayName = event.getAuthor().getName();
                }
            }

            //Final prefix check
            if (prefix == null) {
                prefix = "";
            }
            prefix = prefix.replaceAll("(&([a-f0-9]))", "\u00A7$2");

            String chatMessage = plugin.getConfig().getConfigString("message.discord-chat-message");
            chatMessage = chatMessage.replace("%messageAuthor%", displayName);
            chatMessage = chatMessage.replace("%message%", dmsg);
            chatMessage = chatMessage.replaceAll("(&([a-f0-9]))", "\u00A7$2");
            chatMessage = chatMessage.replace("%prefix%", prefix);
            Bukkit.getServer().broadcastMessage(chatMessage);

            // Relay to external bot via relay server if enabled
            if (plugin.isRelayServerEnabled() && plugin.getRelayServer() != null) {
                // Use | as delimiter to properly handle usernames with spaces
                String relayMessage = "DISCORD_MSG " + displayName + "|" + dmsg;
                plugin.getRelayServer().broadcast(relayMessage);
            }

            return;
        }


    }


}
