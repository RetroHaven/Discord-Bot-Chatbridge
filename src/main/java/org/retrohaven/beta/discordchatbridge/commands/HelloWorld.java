package main.java.org.retrohaven.beta.discordchatbridge.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HelloWorld implements org.retrohaven.beta.discordchatbridge.DCBCommand {
    Set<String> aliases = new HashSet<>(Arrays.asList("hello", "helloworld"));

    public Set<String> getAliases() {
        return aliases;
    }

    public String onCommand(ArrayList<String> arguments) {
        return "Hello world!";
    }
}
