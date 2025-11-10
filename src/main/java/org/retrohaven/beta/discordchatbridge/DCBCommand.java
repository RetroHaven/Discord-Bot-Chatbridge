package org.retrohaven.beta.discordchatbridge;

import java.util.ArrayList;
import java.util.Set;

public interface DCBCommand {
    /*
     * Returns all aliases for the given command.
     */
    Set<String> getAliases();

    /*
     * Config setting to enable or disable the command.
     * To not require any config setting, return null.
     */
    String getConfigOption();

    /*
     * Is executed on each command. The resulting message is sent as a response.
     * To not send any message, return null.
     */
    String onCommand(String[] arguments);
}
