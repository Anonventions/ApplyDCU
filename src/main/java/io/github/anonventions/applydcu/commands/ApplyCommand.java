package io.github.anonventions.applydcu.commands;

import io.github.anonventions.applydcu.ApplyDCU;
import io.github.anonventions.applydcu.gui.PaginatedGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ApplyCommand implements CommandExecutor {

    private final ApplyDCU plugin;

    public ApplyCommand(ApplyDCU plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("apply")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Please specify a role to apply for.");
                return false;
            }

            if (args[0].equalsIgnoreCase("available") && sender.hasPermission("apply.view")) {
                if (sender instanceof Player) {
                    showAvailableApplications((Player) sender);
                } else {
                    sender.sendMessage(ChatColor.RED + "Only players can view applications.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("accept") && sender.hasPermission("apply.manage")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Please specify the application to accept.");
                    return false;
                }
                acceptApplication(sender, args[1]);
                return true;
            }

            if (args[0].equalsIgnoreCase("deny") && sender.hasPermission("apply.manage")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Please specify the application to deny.");
                    return false;
                }
                denyApplication(sender, args[1]);
                return true;
            }

            if (args[0].equalsIgnoreCase("continue")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can continue applications.");
                    return true;
                }
                Player player = (Player) sender;
                continueApplication(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("status")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can check their application status.");
                    return true;
                }
                Player player = (Player) sender;
                showApplicationStatus(player);
                return true;
            }

            if (args.length == 1) {
                handleApplicationStart(sender, args[0]);
                return true;
            }
        }
        return false;
    }

    private void acceptApplication(CommandSender sender, String playerId) {
        UUID playerUUID;
        try {
            playerUUID = UUID.fromString(playerId);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid UUID format.");
            return;
        }
        FileConfiguration applicationConfig = plugin.loadApplication(playerUUID);
        if (applicationConfig == null) {
            sender.sendMessage(ChatColor.RED + "No application found for that player.");
            return;
        }

        String role = applicationConfig.getString("role");
        applicationConfig.set("status", "accepted");
        plugin.saveApplication(playerUUID, applicationConfig);
        plugin.deleteApplication(playerUUID);
        plugin.savePlayerStatus(playerUUID, role, "accepted");
        sender.sendMessage(ChatColor.GREEN + "Accepted application for player: " + Bukkit.getOfflinePlayer(playerUUID).getName() + " for role: " + role);

        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            player.sendMessage(ChatColor.GREEN + "Your application for " + role + " has been accepted.");
        }
    }

    private void denyApplication(CommandSender sender, String playerId) {
        UUID playerUUID;
        try {
            playerUUID = UUID.fromString(playerId);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid UUID format.");
            return;
        }
        FileConfiguration applicationConfig = plugin.loadApplication(playerUUID);
        if (applicationConfig == null) {
            sender.sendMessage(ChatColor.RED + "No application found for that player.");
            return;
        }

        String role = applicationConfig.getString("role");
        applicationConfig.set("status", "denied");
        plugin.saveApplication(playerUUID, applicationConfig);
        plugin.deleteApplication(playerUUID);
        plugin.savePlayerStatus(playerUUID, role, "denied");
        sender.sendMessage(ChatColor.RED + "Denied application for player: " + Bukkit.getOfflinePlayer(playerUUID).getName() + " for role: " + role);

        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            player.sendMessage(ChatColor.RED + "Your application for " + role + " has been denied.");
        }
    }

    private void handleApplicationStart(CommandSender sender, String role) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can apply for roles.");
            return;
        }

        Player player = (Player) sender;
        if (!plugin.getCustomConfig().contains("applications." + role)) {
            player.sendMessage(ChatColor.RED + "That role does not exist.");
            return;
        }

        UUID playerId = player.getUniqueId();
        FileConfiguration existingApplication = plugin.loadApplication(playerId);
        if (existingApplication != null && "in progress".equals(existingApplication.getString("status"))) {
            player.sendMessage(ChatColor.RED + "You already have an ongoing application.");
            return;
        }

        List<String> questions = plugin.getCustomConfig().getStringList("applications." + role + ".questions");
        player.sendMessage(ChatColor.GOLD + "Applying for " + role + ". Please answer the following questions:");

        plugin.getPlayerQuestionIndex().put(playerId, 0);
        plugin.getPlayerAnswers().put(playerId, new ArrayList<>());

        FileConfiguration applicationConfig = YamlConfiguration.loadConfiguration(plugin.getApplicationFile(playerId));
        applicationConfig.set("role", role);
        applicationConfig.set("questions", questions);
        applicationConfig.set("answers", new ArrayList<>());
        applicationConfig.set("status", "in progress");
        applicationConfig.set("submissionTime", System.currentTimeMillis());
        plugin.saveApplication(playerId, applicationConfig);

        plugin.savePlayerStatus(playerId, role, "in progress");

        askNextQuestion(player);
    }

    private void askNextQuestion(Player player) {
        UUID playerId = player.getUniqueId();
        int questionIndex = plugin.getPlayerQuestionIndex().get(playerId);
        FileConfiguration applicationConfig = plugin.loadApplication(playerId);
        List<String> questions = applicationConfig.getStringList("questions");

        if (questionIndex < questions.size()) {
            player.sendMessage(ChatColor.YELLOW + questions.get(questionIndex));
        } else {
            completeApplication(player);
        }
    }

    private void completeApplication(Player player) {
        String role = plugin.loadApplication(player.getUniqueId()).getString("role");
        player.sendMessage(ChatColor.GREEN + "Thank you for applying for " + role + ". Your application has been submitted.");
        plugin.getPlayerQuestionIndex().remove(player.getUniqueId());
        plugin.getPlayerAnswers().remove(player.getUniqueId());
    }

    private void continueApplication(Player player) {
        UUID playerId = player.getUniqueId();

        FileConfiguration applicationConfig = plugin.loadApplication(playerId);
        if (applicationConfig == null) {
            player.sendMessage(ChatColor.RED + "You don't have an ongoing application.");
            return;
        }

        String role = applicationConfig.getString("role");
        List<String> questions = applicationConfig.getStringList("questions");
        List<String> answers = applicationConfig.getStringList("answers");

        plugin.getPlayerQuestionIndex().put(playerId, answers.size());
        plugin.getPlayerAnswers().put(playerId, answers);

        player.sendMessage(ChatColor.GOLD + "Continuing your application for " + role + ".");
        askNextQuestion(player);
    }

    private void showApplicationStatus(Player player) {
        UUID playerId = player.getUniqueId();
        List<ItemStack> items = new ArrayList<>();

        JSONArray applications = plugin.loadPlayerStatus(playerId);
        for (Object obj : applications) {
            JSONObject application = (JSONObject) obj;
            String role = (String) application.get("role");
            String status = (String) application.get("status");

            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Application for: " + ChatColor.WHITE + role);
            meta.setLore(Arrays.asList(ChatColor.YELLOW + "Status: " + ChatColor.WHITE + status));
            paper.setItemMeta(meta);
            items.add(paper);
        }

        PaginatedGUI.showGUI(player, items, 0);
    }

    private void showAvailableApplications(Player player) {
        List<ItemStack> items = new ArrayList<>();

        File applicationsFolder = new File(plugin.getDataFolder(), "applications");
        File[] applicationFiles = applicationsFolder.listFiles();
        if (applicationFiles != null) {
            for (File file : applicationFiles) {
                if (file.getName().endsWith(".yml")) { // Only process YAML files
                    FileConfiguration applicationConfig = YamlConfiguration.loadConfiguration(file);
                    String role = applicationConfig.getString("role");
                    List<String> questions = applicationConfig.getStringList("questions");
                    List<String> answers = applicationConfig.getStringList("answers");

                    ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD, 1);
                    SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
                    UUID playerUUID = UUID.fromString(file.getName().replace(".yml", ""));
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
                    meta.setOwningPlayer(offlinePlayer);
                    meta.setDisplayName(offlinePlayer.getName());

                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GOLD + "Role: " + ChatColor.WHITE + role);
                    for (int i = 0; i < questions.size(); i++) {
                        lore.add(ChatColor.YELLOW + questions.get(i) + ": " + ChatColor.WHITE + (i < answers.size() ? answers.get(i) : ""));
                    }
                    meta.setLore(lore);

                    playerHead.setItemMeta(meta);
                    items.add(playerHead);
                }
            }
        }

        PaginatedGUI.showGUI(player, items, 0);
    }
}
