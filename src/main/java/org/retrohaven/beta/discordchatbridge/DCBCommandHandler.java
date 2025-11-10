package org.retrohaven.beta.discordchatbridge;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class DCBCommandHandler {
    // very basic for now. can be expanded later -eleanor
    Map<String, DCBCommand> aliases;
    DiscordChatBridge plugin;

    public DCBCommandHandler(DiscordChatBridge plugin) {
        this.plugin = plugin;
    }

    public void registerCommand(DCBCommand command) {
        Set<String> commandAliases = command.getAliases();
        for (String commandAlias : commandAliases) {
            aliases.putIfAbsent(commandAlias, command);
        }
    }

    public String onCommand(String commandName, String[] arguments) {
        if (aliases.containsKey(commandName) && (aliases.get(commandName).getConfigOption() == null || plugin.getConfig().getConfigBoolean(aliases.get(commandName).getConfigOption()))) {
            return aliases.get(commandName).onCommand(arguments);
        }
        return "Command not found.";
    }
}
