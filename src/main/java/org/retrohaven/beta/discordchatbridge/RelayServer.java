package org.retrohaven.beta.discordchatbridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * IRC bot relay
 * Protocol:
 * - AUTH <password>          - Authenticate
 * - GAME_CHAT <player> <msg> - Player chat from game
 * - GAME_JOIN <player>       - Player joined
 * - GAME_QUIT <player>       - Player quit
 * - GAME_DEATH <message>     - Death message
 * - IRC_MSG <user> <msg>     - IRC user message (from external bot)
 * - IRC_JOIN <user> <channel> - IRC user joined channel (from external bot)
 * - IRC_PART <user> <channel> - IRC user left channel (from external bot)
 * - DISCORD_MSG <user>|<msg> - Discord user message (uses | delimiter to handle spaces in usernames)
 * - IRC_CMD <source> <requester> <command> - IRC command from Discord/Game (source: DISCORD or GAME)
 * - IRC_CMD_RESPONSE <source> <requester> <response> - IRC command response to send back
 * - PING                     - Keep-alive ping
 * - PONG                     - Keep-alive response
 */
public class RelayServer {
    private final DiscordChatBridge plugin;
    private final int port;
    private final String bindAddress;
    private final String password;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private final Set<RelayClient> clients = new HashSet<>();
    private boolean running = false;

    public RelayServer(DiscordChatBridge plugin, String bindAddress, int port, String password) {
        this.plugin = plugin;
        this.bindAddress = bindAddress;
        this.port = port;
        this.password = password;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port, 50, java.net.InetAddress.getByName(bindAddress));
            running = true;

            plugin.logger(Level.INFO, "Relay server started on " + bindAddress + ":" + port);

            // Accept connections in a separate thread
            acceptThread = new Thread(() -> {
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        RelayClient client = new RelayClient(clientSocket);
                        synchronized (clients) {
                            clients.add(client);
                        }
                        client.start();
                    } catch (IOException e) {
                        if (running) {
                            plugin.logger(Level.WARNING, "Error accepting relay connection: " + e.getMessage());
                        }
                    }
                }
            }, "RelayServer-Accept");
            acceptThread.start();

        } catch (IOException e) {
            plugin.logger(Level.SEVERE, "Failed to start relay server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;

        // Close all clients
        synchronized (clients) {
            for (RelayClient client : clients) {
                client.disconnect();
            }
            clients.clear();
        }

        // Close server socket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                plugin.logger(Level.WARNING, "Error closing relay server: " + e.getMessage());
            }
        }

        plugin.logger(Level.INFO, "Relay server stopped");
    }

    public void broadcast(String message) {
        synchronized (clients) {
            for (RelayClient client : clients) {
                if (client.isAuthenticated()) {
                    client.send(message);
                }
            }
        }
    }

    /**
     * Sanitize Discord mentions to prevent abuse from IRC
     * Prevents @everyone, @here, and user/role mentions from working
     */
    private static String sanitizeDiscordMentions(String message) {
        if (message == null) return "";

        // Replace @everyone with @ everyone (adds space to break the mention)
        message = message.replace("@everyone", "@ everyone");

        // Replace @here with @ here (adds space to break the mention)
        message = message.replace("@here", "@ here");

        // Replace all other @ mentions with @ <text> to prevent user/role pings
        // This regex finds @word and replaces with @ word
        message = message.replaceAll("@(\\w+)", "@ $1");

        return message;
    }

    /**
     * Individual client connection handler
     */
    private class RelayClient {
        private final Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private boolean authenticated = false;
        private Thread readThread;

        public RelayClient(Socket socket) {
            this.socket = socket;
        }

        public void start() {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);

                // Send welcome message
                send("HELLO RelayServer v1.0");
                send("AUTH_REQUIRED Please authenticate with: AUTH <password>");

                // Read messages in a separate thread
                readThread = new Thread(() -> {
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            handleMessage(line.trim());
                        }
                    } catch (IOException e) {
                        // Client disconnected
                    } finally {
                        disconnect();
                    }
                }, "RelayClient-Read");
                readThread.start();

            } catch (IOException e) {
                plugin.logger(Level.WARNING, "Error starting relay client: " + e.getMessage());
                disconnect();
            }
        }

        private void handleMessage(String message) {
            if (message.isEmpty()) return;

            String[] parts = message.split(" ", 2);
            String command = parts[0];

            // AUTH command doesn't require authentication
            if (command.equals("AUTH")) {
                if (parts.length < 2) {
                    send("ERROR Missing password");
                    return;
                }

                if (parts[1].equals(password)) {
                    authenticated = true;
                    send("AUTH_OK Successfully authenticated");
                    plugin.logger(Level.INFO, "Relay client authenticated from " + socket.getRemoteSocketAddress());
                } else {
                    send("AUTH_FAIL Invalid password");
                    plugin.logger(Level.WARNING, "Failed auth attempt from " + socket.getRemoteSocketAddress());
                }
                return;
            }

            // All other commands require authentication
            if (!authenticated) {
                send("ERROR Not authenticated");
                return;
            }

            // Handle authenticated commands
            if (command.equals("PING")) {
                send("PONG");
            } else if (command.equals("IRC_CMD_RESPONSE")) {
                if (parts.length < 2) return;

                // Parse: IRC_CMD_RESPONSE <source> <requester> <response>
                String[] responseParts = parts[1].split(" ", 3);
                if (responseParts.length < 3) return;

                final String source = responseParts[0];
                final String requester = responseParts[1];
                final String response = responseParts[2];

                if (source.equalsIgnoreCase("GAME")) {
                    // Send response to game player
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                        public void run() {
                            Player player = Bukkit.getServer().getPlayer(requester);
                            if (player != null && player.isOnline()) {
                                player.sendMessage("§f[§6IRC§f]§7 " + response);
                            }
                        }
                    });
                } else if (source.equalsIgnoreCase("DISCORD")) {
                    // Send response to Discord channel
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                        public void run() {
                            String discordMessage = "**[IRC]** " + response;
                            plugin.getDiscordCore().getDiscordBot().discordSendToChannel(
                                    plugin.getConfig().getConfigString("channel-id"),
                                    discordMessage
                            );
                        }
                    });
                }
            } else if (command.equals("IRC_MSG")) {
                if (parts.length < 2) return;

                // Parse: IRC_MSG <user> <message>
                String[] msgParts = parts[1].split(" ", 2);
                if (msgParts.length < 2) return;

                final String user = msgParts[0];
                final String msg = msgParts[1];

                // Sanitize Discord mentions to prevent abuse from IRC
                final String sanitizedMsg = sanitizeDiscordMentions(msg);

                // Relay to Minecraft
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        String chatMessage = "§f[§6IRC§f]§7 " + user + ": " + sanitizedMsg;
                        Bukkit.getServer().broadcastMessage(chatMessage);
                    }
                });

                // Relay to Discord
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        String discordMessage = "**[IRC]** " + user + ": " + sanitizedMsg;
                        plugin.getDiscordCore().getDiscordBot().discordSendToChannel(
                                plugin.getConfig().getConfigString("channel-id"),
                                discordMessage
                        );
                    }
                });
            } else if (command.equals("IRC_JOIN")) {
                if (parts.length < 2) return;

                // Parse: IRC_JOIN <user> <channel>
                String[] joinParts = parts[1].split(" ", 2);
                if (joinParts.length < 2) return;

                final String user = joinParts[0];
                final String channel = joinParts[1];

                // Relay to Minecraft
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        String chatMessage = "§f[§6IRC§f]§7 " + user + " has joined " + channel;
                        Bukkit.getServer().broadcastMessage(chatMessage);
                    }
                });

                // Relay to Discord
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        String discordMessage = "**[IRC]** " + user + " has joined " + channel;
                        plugin.getDiscordCore().getDiscordBot().discordSendToChannel(
                                plugin.getConfig().getConfigString("channel-id"),
                                discordMessage
                        );
                    }
                });
            } else if (command.equals("IRC_PART")) {
                if (parts.length < 2) return;

                // Parse: IRC_PART <user> <channel>
                String[] partParts = parts[1].split(" ", 2);
                if (partParts.length < 2) return;

                final String user = partParts[0];
                final String channel = partParts[1];

                // Relay to Minecraft
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        String chatMessage = "§f[§6IRC§f]§7 " + user + " has disconnected from " + channel;
                        Bukkit.getServer().broadcastMessage(chatMessage);
                    }
                });

                // Relay to Discord
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        String discordMessage = "**[IRC]** " + user + " has disconnected from " + channel;
                        plugin.getDiscordCore().getDiscordBot().discordSendToChannel(
                                plugin.getConfig().getConfigString("channel-id"),
                                discordMessage
                        );
                    }
                });
            }
        }

        public void send(String message) {
            if (writer != null && !socket.isClosed()) {
                writer.println(message);
            }
        }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public void disconnect() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                // Ignore
            }

            synchronized (clients) {
                clients.remove(this);
            }
        }
    }
}
