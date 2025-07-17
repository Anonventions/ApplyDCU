package io.github.anonventions.capplications.commands;

import io.github.anonventions.capplications.CApplications;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplicationTabCompleter implements TabCompleter {
    private final CApplications plugin;

    public ApplicationTabCompleter(CApplications plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList(
                    "available", "continue", "status", "history", "cancel", "roles", "help"
            );

            if (sender.hasPermission("capplications.manage")) {
                subcommands.addAll(Arrays.asList("accept", "deny", "stats"));
            }

            if (sender.hasPermission("capplications.admin")) {
                subcommands.addAll(Arrays.asList("reload", "purge", "export", "cooldown"));
            }

            // Add available roles
            Set<String> roles = plugin.getCustomConfig().getConfigurationSection("applications").getKeys(false);
            subcommands.addAll(roles);

            completions.addAll(subcommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList()));

        } else if (args.length == 2) {
            String subcommand = args[0].toLowerCase();

            if (subcommand.equals("accept") || subcommand.equals("deny") || subcommand.equals("cooldown")) {
                if (sender.hasPermission("capplications.manage") || sender.hasPermission("capplications.admin")) {
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList()));
                }
            } else if (subcommand.equals("export")) {
                if (sender.hasPermission("capplications.admin")) {
                    Set<String> roles = plugin.getCustomConfig().getConfigurationSection("applications").getKeys(false);
                    completions.addAll(roles.stream()
                            .filter(role -> role.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList()));
                }
            }
        }

        return completions;
    }
}