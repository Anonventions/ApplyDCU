package io.github.anonventions.capplications.utils;

import io.github.anonventions.capplications.CApplications;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class MessageUtils {
    private final CApplications plugin;

    public MessageUtils(CApplications plugin) {
        this.plugin = plugin;
    }

    public String formatMessage(String path, Object... replacements) {
        FileConfiguration config = plugin.getConfig();
        String message = config.getString("messages." + path, "Message not found: " + path);

        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(replacements[i]));
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void sendMessage(CommandSender sender, String path, Object... replacements) {
        sender.sendMessage(formatMessage(path, replacements));
    }

    public void sendNoPermissionMessage(CommandSender sender) {
        sendMessage(sender, "no_permission");
    }

    public void sendErrorMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.RED + message);
    }

    public void sendSuccessMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.GREEN + message);
    }

    public void sendInfoMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.YELLOW + message);
    }
}