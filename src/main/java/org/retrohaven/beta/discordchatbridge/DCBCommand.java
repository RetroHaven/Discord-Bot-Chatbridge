package org.retrohaven.beta.discordchatbridge;

import java.util.ArrayList;
import java.util.Set;

public interface DCBCommand {
    /*
     * Returns all aliases for the given command.
     */
    Set<String> getAliases();

    /*
     * Is executed on each command. The resulting message is sent as a response.
     * To not send any message, return null.
     */
    String onCommand(ArrayList<String> arguments);
}
