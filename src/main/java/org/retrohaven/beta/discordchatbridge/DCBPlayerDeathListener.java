package org.retrohaven.beta.discordchatbridge;

/*
Most of this file is taken from the plugin BetaDeaths (more specifically
the file src/main/java/me/lukiiy/BetaDeaths/Listener.java), available at
https://github.com/Lukiiy/BetaDeaths and licensed under the MIT License

MIT License

Copyright (c) 2024 Lukiiy

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;

import me.lukiiy.BetaDeaths.BetaDeaths;
import me.lukiiy.BetaDeaths.Utils;

import org.retrohaven.beta.discordchatbridge.DiscordChatBridge;

import java.util.Dictionary;
import java.util.logging.Level;

public class DCBPlayerDeathListener extends EntityListener {
    private DiscordChatBridge plugin;
    private Dictionary LastDamager;

    public DCBPlayerDeathListener(DiscordChatBridge plugin) {
        this.plugin = plugin;
    }

    private boolean isEntityCacheable(Entity entity) {return entity instanceof Player || entity instanceof Tameable;}

    @Override
    public void onEntityDamage(EntityDamageEvent ev) {
        if (!(ev instanceof EntityDamageByEntityEvent) || !(isEntityCacheable(ev.getEntity()))) return;
        EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) ev;

        BetaDeaths.setEntityLastDamager(event.getEntity(), event.getDamager());
    }

    @Override
    public void onEntityDeath(EntityDeathEvent e) {
        Entity entity = e.getEntity();

        if (!isEntityCacheable(entity)) return;
        if (entity instanceof Tameable && !((Tameable) entity).isTamed()) return;

        String entityName = entity instanceof Player ? ((Player) entity).getName() : Utils.getEntityName(entity);

        EntityDamageEvent.DamageCause cause = entity.getLastDamageCause().getCause();

        Entity lastDamager = BetaDeaths.getEntityLastDamager(entity);
        String damagerName = BetaDeaths.getDeathMsg("unknownEntity");


        if (lastDamager != null) {
            if (lastDamager instanceof Projectile) lastDamager = ((Projectile) lastDamager).getShooter();
            if (lastDamager instanceof LivingEntity) {
                damagerName = (lastDamager instanceof Player) ? ((Player) lastDamager).getDisplayName() : Utils.getEntityName(lastDamager);
                if (lastDamager instanceof Creeper && cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) cause = EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
            }
        }

        String reason;
        switch (cause) {
            case CONTACT:
                reason = BetaDeaths.getDeathMsg("contact");
                break;
            case ENTITY_ATTACK:
                reason = BetaDeaths.getDeathMsg("attack");
                break;
            case FALL:
                reason = BetaDeaths.getDeathMsg("fall");
                if (entity.getFallDistance() < 5f) reason = BetaDeaths.getDeathMsg("hard_fall");
                break;
            case FIRE:
            case FIRE_TICK:
                if (entity.getWorld().getBlockAt(entity.getLocation()).getType() == Material.FIRE)
                    reason = BetaDeaths.getDeathMsg("fire");
                else reason = BetaDeaths.getDeathMsg("burn");
                break;
            case LAVA:
                reason = BetaDeaths.getDeathMsg("lava");
                break;
            case VOID:
                reason = BetaDeaths.getDeathMsg("void");
                break;
            case SUICIDE:
                reason = BetaDeaths.getDeathMsg("suicide");
                break;
            case DROWNING:
                reason = BetaDeaths.getDeathMsg("drown");
                break;
            case LIGHTNING:
                reason = BetaDeaths.getDeathMsg("lightning");
                break;
            case PROJECTILE:
                reason = BetaDeaths.getDeathMsg("attack_projectile");
                break;
            case SUFFOCATION:
                reason = BetaDeaths.getDeathMsg("suffocation");
                break;
            case BLOCK_EXPLOSION:
                reason = BetaDeaths.getDeathMsg("explosion_block");
                break;
            case ENTITY_EXPLOSION:
                reason = BetaDeaths.getDeathMsg("explosion");
                break;
            default:
                reason = BetaDeaths.getDeathMsg("default_cause");
                break;
        }

        if (reason.isEmpty()) return;
        String chatMessage = reason
                .replace("(victim)", "**"+entityName+"**")
                .replace('&', '§');
        if (lastDamager != null && Bukkit.getPlayerExact(damagerName) != null) {
            chatMessage = chatMessage.replace("(damager)", "**"+damagerName+"**");
        }
        chatMessage = chatMessage.replace("(damager)", damagerName);
        plugin.getDiscordCore().getDiscordBot().discordSendToChannel(plugin.getConfig().getConfigString("channel-id"), chatMessage);

        // Relay to external bot via relay server if enabled
        if (plugin.isRelayServerEnabled() && plugin.getRelayServer() != null) {
            // Strip Discord formatting from death message
            String plainMessage = chatMessage.replace("**", "");
            String relayMessage = "GAME_DEATH " + plainMessage;
            plugin.getRelayServer().broadcast(relayMessage);
        }
    }
}
