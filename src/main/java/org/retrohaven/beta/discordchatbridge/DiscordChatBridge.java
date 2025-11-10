package org.retrohaven.beta.discordchatbridge;

import com.johnymuffin.discordcore.DiscordCore;
import com.johnymuffin.discordcore.DiscordShutdownEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.retrohaven.beta.discordchatbridge.commands.*;

import org.retrohaven.beta.discordchatbridge.DCBGameListener;
import org.retrohaven.beta.discordchatbridge.DCBPlayerDeathListener;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscordChatBridge extends JavaPlugin {
    private static DiscordChatBridge plugin;
    private Logger log;
    private String pluginName;
    private PluginDescriptionFile pdf;
    private DiscordCore discordCore;
    private DCBConfig dcbConfig;
    private DCBDiscordListener discordListener;
    private DCBCommandHandler commandHandler;
    private RelayServer relayServer;
    private boolean relayServerEnabled = false;
    private boolean enabled = false;
    private Integer taskID = null;
    private boolean shutdown = false;

    @Override
    public void onEnable() {
        plugin = this;
        log = this.getServer().getLogger();
        pdf = this.getDescription();
        pluginName = pdf.getName();
        log.info("[" + pluginName + "] Is Loading, Version: " + pdf.getVersion());

        if (!Bukkit.getServer().getPluginManager().isPluginEnabled("DiscordCore")) {
            log.info("}---------------ERROR---------------{");
            log.info("Discord Chat Bridge Requires Discord Core");
            log.info("Download it at: https://github.com/RhysB/Discord-Bot-Core");
            log.info("}---------------ERROR---------------{");
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        dcbConfig = new DCBConfig(plugin);

        if (dcbConfig.getConfigString("channel-id").isEmpty() || dcbConfig.getConfigString("channel-id").equalsIgnoreCase("id")) {
            log.info("}----------------------------ERROR----------------------------{");
            log.info("Please provide a Servername and Channel for the Link");
            log.info("}----------------------------ERROR----------------------------{");
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        if (dcbConfig.getConfigBoolean("webhook.use-webhook")) {
            if (dcbConfig.getConfigString("webhook.url") == null || dcbConfig.getConfigString("webhook.url").isEmpty() || dcbConfig.getConfigString("webhook.url").equalsIgnoreCase("url")) {
                log.info("}----------------------------ERROR----------------------------{");
                log.info("Please provide a valid Discord Webhook url");
                log.info("}----------------------------ERROR----------------------------{");
                Bukkit.getServer().getPluginManager().disablePlugin(plugin);
                return;
            }
        }

        if (dcbConfig.getConfigBoolean("authentication.enabled") && !Bukkit.getServer().getPluginManager().isPluginEnabled("DiscordAuthentication")) {
            log.info("}---------------ERROR---------------{");
            log.info("Authentication support is enabled, however, the plugin isn't installed.");
            log.info("Download it at: https://github.com/RhysB/Discord-Bot-Core");
            log.info("}---------------ERROR---------------{");
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        // Register commands - we do this early since the command handler is then passed to other stuff
        commandHandler = new DCBCommandHandler(plugin);
        commandHandler.registerCommand(new HelloWorld());
        commandHandler.registerCommand(new PlayerList());

        discordCore = (DiscordCore) Bukkit.getServer().getPluginManager().getPlugin("DiscordCore");
        discordListener = new DCBDiscordListener(plugin, commandHandler);
        discordCore.getDiscordBot().jda.addEventListener(discordListener);

        // STAFF MESSAGING RELAY
        String staffChannelId = dcbConfig.getConfigString("staff-messaging-channel-id");
        StaffMessagingRelayAPI.init(discordCore.getDiscordBot().jda, staffChannelId);
        if (staffChannelId.isEmpty()) {
            log.info("Staff messaging relay is disabled (no staff channel ID set).");
        } else {
            log.info("Staff messaging relay enabled for channel ID: " + staffChannelId);
        }

        // RELAY SERVER
        if (dcbConfig.getConfigBoolean("relay-server.enabled")) {
            try {
                log.info("Relay server is enabled, starting...");
                String bindAddress = dcbConfig.getConfigString("relay-server.bind-address");
                int port = dcbConfig.getConfigInteger("relay-server.port");
                String password = dcbConfig.getConfigString("relay-server.password");

                relayServer = new RelayServer(this, bindAddress, port, password);
                relayServer.start();
                relayServerEnabled = true;
                log.info("Relay server started successfully on " + bindAddress + ":" + port);
            } catch (Exception e) {
                log.severe("Failed to start relay server: " + e.getMessage());
                e.printStackTrace();
                relayServerEnabled = false;
            }
        } else {
            log.info("Relay server is disabled in config");
        }

        // Register game and death listeners
        final DCBGameListener gameListener = new DCBGameListener(plugin);
        final DCBPlayerDeathListener deathDamageListener = new DCBPlayerDeathListener(plugin);
        final DCBBanListener banListener = new DCBBanListener(plugin);
        final DCBBanListener.DCBBanServerListener banServerListener = new DCBBanListener.DCBBanServerListener(plugin, banListener);
        getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, gameListener, Event.Priority.Monitor, this);
        getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, gameListener, Event.Priority.Monitor, this);
        getServer().getPluginManager().registerEvent(Event.Type.PLAYER_CHAT, gameListener, Event.Priority.Highest, this);
        getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DAMAGE, deathDamageListener, Event.Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DEATH, deathDamageListener, Event.Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, banListener, Event.Priority.Monitor, this);
        getServer().getPluginManager().registerEvent(Event.Type.SERVER_COMMAND, banServerListener, Event.Priority.Monitor, this);
        final ShutdownListener shutdownListener = new ShutdownListener();
        getServer().getPluginManager().registerEvent(Event.Type.CUSTOM_EVENT, shutdownListener, Event.Priority.Normal, plugin);

        enabled = true;

        if (dcbConfig.getConfigBoolean("system.starting-message.enable")) {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                String message = dcbConfig.getConfigString("system.starting-message.message");
                message = message.replace("{servername}", dcbConfig.getConfigString("server-name"));
                discordCore.getDiscordBot().discordSendToChannel(dcbConfig.getConfigString("channel-id"), message);
            }, 0L);
        }

        if (dcbConfig.getConfigBoolean("presence-player-count")) {
            taskID = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
                if (discordCore.getDiscordBot().jda.getStatus() == JDA.Status.CONNECTED) {
                    String bktString = dcbConfig.getConfigString("presence-message");
                    bktString = bktString.replace("{servername}", dcbConfig.getConfigString("server-name"));
                    bktString = bktString.replace("{onlineCount}", String.valueOf(Bukkit.getServer().getOnlinePlayers().length));
                    bktString = bktString.replace("{maxOnlineCount}", String.valueOf(Bukkit.getServer().getMaxPlayers()));
                    discordCore.getDiscordBot().jda.getPresence().setActivity(Activity.playing(bktString));
                }
            }, 0L, 20 * 60);
        }
    }

    @Override
    public void onDisable() {
        if (enabled) {
            logger(Level.INFO, "Disabling.");

            // Stop relay server if enabled
            if (relayServerEnabled && relayServer != null) {
                relayServer.stop();
            }

            if (!shutdown) {
                handleDiscordCoreShutdown();
            }
            discordCore.getDiscordBot().jda.removeEventListener(discordListener);
            Bukkit.getServer().getScheduler().cancelTask(taskID);
        }
        logger(Level.INFO, "Has been disabled.");
    }

    public void logger(Level level, String message) {
        log.log(level, "[" + pluginName + "] " + message);
    }

    public DCBConfig getConfig() {
        return dcbConfig;
    }

    public DiscordCore getDiscordCore() {
        return discordCore;
    }

    public RelayServer getRelayServer() {
        return relayServer;
    }

    public boolean isRelayServerEnabled() {
        return relayServerEnabled;
    }

    protected void handleDiscordCoreShutdown() {
        shutdown = true;
        if (getConfig().getConfigBoolean("system.shutdown-message.enable")) {
            String message = getConfig().getConfigString("system.shutdown-message.message");
            message = message.replace("{servername}", getConfig().getConfigString("server-name"));
            TextChannel textChannel = this.discordCore.getDiscordBot().jda.getTextChannelById(dcbConfig.getConfigString("channel-id"));
            textChannel.sendMessage(message).complete();
        }
    }

    private class ShutdownListener extends CustomEventListener {
        @Override
        public void onCustomEvent(Event event) {
            if (!(event instanceof DiscordShutdownEvent)) {
                return;
            }
            if (shutdown) {
                return;
            }
            handleDiscordCoreShutdown();
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
        }
    }
}
