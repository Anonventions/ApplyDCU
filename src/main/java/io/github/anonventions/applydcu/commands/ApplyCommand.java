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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

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
        if (!plugin.getApplicationsConfig().contains("applications." + playerId)) {
            sender.sendMessage(ChatColor.RED + "No application found for that player.");
            return;
        }

        String role = plugin.getApplicationsConfig().getString("applications." + playerId + ".role");
        plugin.getApplicationsConfig().set("applications." + playerId + ".status", "accepted");
        plugin.saveApplicationsConfig();
        sender.sendMessage(ChatColor.GREEN + "Accepted application for player: " + Bukkit.getOfflinePlayer(UUID.fromString(playerId)).getName() + " for role: " + role);

        Player player = Bukkit.getPlayer(UUID.fromString(playerId));
        if (player != null && player.isOnline()) {
            player.sendMessage(ChatColor.GREEN + "Your application for " + role + " has been accepted.");
        }
    }

    private void denyApplication(CommandSender sender, String playerId) {
        if (!plugin.getApplicationsConfig().contains("applications." + playerId)) {
            sender.sendMessage(ChatColor.RED + "No application found for that player.");
            return;
        }

        String role = plugin.getApplicationsConfig().getString("applications." + playerId + ".role");
        plugin.getApplicationsConfig().set("applications." + playerId + ".status", "denied");
        plugin.saveApplicationsConfig();
        sender.sendMessage(ChatColor.RED + "Denied application for player: " + Bukkit.getOfflinePlayer(UUID.fromString(playerId)).getName() + " for role: " + role);

        Player player = Bukkit.getPlayer(UUID.fromString(playerId));
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
        if (plugin.getApplicationsConfig().contains("applications." + playerId) &&
                plugin.getApplicationsConfig().getString("applications." + playerId + ".status").equals("in progress")) {
            player.sendMessage(ChatColor.RED + "You already have an ongoing application.");
            return;
        }

        List<String> questions = plugin.getCustomConfig().getStringList("applications." + role + ".questions");
        player.sendMessage(ChatColor.GOLD + "Applying for " + role + ". Please answer the following questions:");

        plugin.getPlayerQuestionIndex().put(playerId, 0);
        plugin.getPlayerAnswers().put(playerId, new ArrayList<>());

        plugin.getApplicationsConfig().set("applications." + player.getUniqueId() + ".role", role);
        plugin.getApplicationsConfig().set("applications." + player.getUniqueId() + ".questions", questions);
        plugin.getApplicationsConfig().set("applications." + player.getUniqueId() + ".answers", new ArrayList<>());
        plugin.getApplicationsConfig().set("applications." + player.getUniqueId() + ".status", "in progress");
        plugin.getApplicationsConfig().set("applications." + player.getUniqueId() + ".submissionTime", System.currentTimeMillis());
        plugin.saveApplicationsConfig();

        askNextQuestion(player);
    }

    private void askNextQuestion(Player player) {
        UUID playerId = player.getUniqueId();
        int questionIndex = plugin.getPlayerQuestionIndex().get(playerId);
        List<String> questions = plugin.getApplicationsConfig().getStringList("applications." + playerId + ".questions");

        if (questionIndex < questions.size()) {
            player.sendMessage(ChatColor.YELLOW + questions.get(questionIndex));
        } else {
            completeApplication(player);
        }
    }

    private void completeApplication(Player player) {
        String role = plugin.getApplicationsConfig().getString("applications." + player.getUniqueId() + ".role");
        player.sendMessage(ChatColor.GREEN + "Thank you for applying for " + role + ". Your application has been submitted.");
        plugin.getPlayerQuestionIndex().remove(player.getUniqueId());
        plugin.getPlayerAnswers().remove(player.getUniqueId());
    }

    private void continueApplication(Player player) {
        UUID playerId = player.getUniqueId();

        if (!plugin.getApplicationsConfig().contains("applications." + playerId)) {
            player.sendMessage(ChatColor.RED + "You don't have an ongoing application.");
            return;
        }

        String role = plugin.getApplicationsConfig().getString("applications." + playerId + ".role");
        List<String> questions = plugin.getApplicationsConfig().getStringList("applications." + playerId + ".questions");
        List<String> answers = plugin.getApplicationsConfig().getStringList("applications." + playerId + ".answers");

        plugin.getPlayerQuestionIndex().put(playerId, answers.size());
        plugin.getPlayerAnswers().put(playerId, answers);

        player.sendMessage(ChatColor.GOLD + "Continuing your application for " + role + ".");
        askNextQuestion(player);
    }

    private void showApplicationStatus(Player player) {
        UUID playerId = player.getUniqueId();
        List<ItemStack> items = new ArrayList<>();

        if (plugin.getApplicationsConfig().contains("applications")) {
            Set<String> applicationKeys = plugin.getApplicationsConfig().getConfigurationSection("applications").getKeys(false);

            for (String key : applicationKeys) {
                if (key.equals(playerId.toString())) {
                    String role = plugin.getApplicationsConfig().getString("applications." + key + ".role");
                    String status = plugin.getApplicationsConfig().getString("applications." + key + ".status");
                    if (status == null) {
                        status = "in progress";
                    }
                    ItemStack paper = new ItemStack(Material.PAPER);
                    ItemMeta meta = paper.getItemMeta();
                    meta.setDisplayName(ChatColor.GOLD + "Application for: " + ChatColor.WHITE + role);
                    meta.setLore(Arrays.asList(ChatColor.YELLOW + "Status: " + ChatColor.WHITE + status));
                    paper.setItemMeta(meta);
                    items.add(paper);
                }
            }
        }

        PaginatedGUI.showGUI(player, items, 0);
    }

    private void showAvailableApplications(Player player) {
        List<ItemStack> items = new ArrayList<>();

        if (plugin.getApplicationsConfig().contains("applications")) {
            Set<String> applicationKeys = plugin.getApplicationsConfig().getConfigurationSection("applications").getKeys(false);

            for (String key : applicationKeys) {
                String role = plugin.getApplicationsConfig().getString("applications." + key + ".role");
                List<String> questions = plugin.getApplicationsConfig().getStringList("applications." + key + ".questions");
                List<String> answers = plugin.getApplicationsConfig().getStringList("applications." + key + ".answers");

                ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(key));
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

        PaginatedGUI.showGUI(player, items, 0);
    }
}
