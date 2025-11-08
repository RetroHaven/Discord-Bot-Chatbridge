package org.retrohaven.beta.discordchatbridge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DCBCommandHandler {
    // very basic for now. can be expanded later -eleanor
    Map<String, DCBCommand> aliases;

    public void registerCommand(DCBCommand command) {
        Set<String> commandAliases = command.getAliases();
        for (String commandAlias : commandAliases) {
            aliases.putIfAbsent(commandAlias, command);
        }
    }

    public String onCommand(String commandName, ArrayList<String> arguments) {
        if (aliases.containsKey(commandName)) {
            return aliases.get(commandName).onCommand(arguments);
        }
        return "Command not found.";
    }
}
