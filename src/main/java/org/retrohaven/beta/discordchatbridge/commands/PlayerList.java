package org.retrohaven.beta.discordchatbridge.commands;

import org.retrohaven.beta.discordchatbridge.DCBCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PlayerList implements DCBCommand {
    Set<String> aliases = new HashSet<>(Arrays.asList("list", "playerlist"));

    public Set<String> getAliases() {
        return aliases;
    }

    public String getConfigOption() {
        return null;
    }

    public String onCommand(String[] arguments) {
        return "";
    }
}
