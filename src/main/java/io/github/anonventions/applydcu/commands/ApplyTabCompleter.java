package io.github.anonventions.applydcu.commands;

import io.github.anonventions.applydcu.ApplyDCU;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class ApplyTabCompleter implements TabCompleter {

    private final ApplyDCU plugin;

    public ApplyTabCompleter(ApplyDCU plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("apply")) {
            if (args.length == 1) {
                List<String> roles = new ArrayList<>(plugin.getCustomConfig().getConfigurationSection("applications").getKeys(false));
                roles.add("available");
                roles.add("continue");
                roles.add("status");
                return roles;
            }
        }
        return null;
    }
}
