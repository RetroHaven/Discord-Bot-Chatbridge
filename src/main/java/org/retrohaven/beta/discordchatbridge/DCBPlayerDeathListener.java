package org.retrohaven.beta.discordchatbridge;

import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.legacyminecraft.poseidon.event.PlayerDeathEvent;
import com.legacyminecraft.poseidon.event.PoseidonCustomListener;

import org.retrohaven.beta.discordchatbridge.DiscordChatBridge;

public class DCBPlayerDeathListener implements Listener {
    private DiscordChatBridge plugin;

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        String chatMessage = event.getDeathMessage();
        String[] chatMessageArray = chatMessage.split(" ");
        String playername = chatMessageArray[0];
        String[] deathMessageArray = new String[0];
        System.arraycopy(chatMessageArray, 1, deathMessageArray, 0, chatMessageArray.length-1);
        chatMessage = "**" + chatMessageArray[0] + "** " + String.join(" ", deathMessageArray);
        plugin.getDiscordCore().getDiscordBot().discordSendToChannel(plugin.getConfig().getConfigString("channel-id"), chatMessage);
    }
}
