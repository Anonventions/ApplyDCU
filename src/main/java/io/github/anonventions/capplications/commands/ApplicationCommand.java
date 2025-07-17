package io.github.anonventions.capplications.commands;

import io.github.anonventions.capplications.CApplications;
import io.github.anonventions.capplications.gui.PaginatedGUI;
import io.github.anonventions.capplications.utils.ApplicationValidator;
import io.github.anonventions.capplications.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ApplicationCommand implements CommandExecutor {

    private final CApplications plugin;
    private final MessageUtils messageUtils;
    private final ApplicationValidator validator;

    public ApplicationCommand(CApplications plugin) {
        this.plugin = plugin;
        this.messageUtils = new MessageUtils(plugin);
        this.validator = new ApplicationValidator(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command must be used by a player.");
                return true;
            }
            showAvailableApplications((Player) sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "help":
                showHelp(sender);
                break;
            case "continue":
                if (!(sender instanceof Player)) {
                    messageUtils.sendMessage(sender, "player_only");
                    return true;
                }
                continueApplication((Player) sender);
                break;
            case "cancel":
                if (!(sender instanceof Player)) {
                    messageUtils.sendMessage(sender, "player_only");
                    return true;
                }
                cancelApplication((Player) sender);
                break;
            case "status":
                if (!(sender instanceof Player)) {
                    messageUtils.sendMessage(sender, "player_only");
                    return true;
                }
                showApplicationStatus((Player) sender);
                break;
            case "history":
                if (args.length > 1 && sender.hasPermission("capplications.admin")) {
                    UUID targetId = parsePlayerUUID(args[1]);
                    if (targetId != null) {
                        showApplicationHistory(sender, targetId);
                    } else {
                        messageUtils.sendErrorMessage(sender, "Invalid player identifier.");
                    }
                } else if (sender instanceof Player) {
                    showApplicationHistory(sender, ((Player) sender).getUniqueId());
                } else {
                    messageUtils.sendMessage(sender, "player_only");
                }
                break;
            case "accept":
                if (!sender.hasPermission("capplications.manage")) {
                    messageUtils.sendNoPermissionMessage(sender);
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /app accept <player>");
                    return true;
                }
                UUID acceptId = parsePlayerUUID(args[1]);
                if (acceptId != null) {
                    processAcceptance(sender, acceptId);
                } else {
                    messageUtils.sendErrorMessage(sender, "Invalid player identifier.");
                }
                break;
            case "deny":
                if (!sender.hasPermission("capplications.manage")) {
                    messageUtils.sendNoPermissionMessage(sender);
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /app deny <player> [reason]");
                    return true;
                }
                UUID denyId = parsePlayerUUID(args[1]);
                if (denyId != null) {
                    String reason = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "Denied by console";
                    processDenial(sender, denyId, reason);
                } else {
                    messageUtils.sendErrorMessage(sender, "Invalid player identifier.");
                }
                break;
            case "stats":
                if (!sender.hasPermission("capplications.admin")) {
                    messageUtils.sendNoPermissionMessage(sender);
                    return true;
                }
                showApplicationStatistics(sender);
                break;
            case "roles":
                showAvailableRoles(sender);
                break;
            case "purge":
                if (!sender.hasPermission("capplications.admin")) {
                    messageUtils.sendNoPermissionMessage(sender);
                    return true;
                }
                int days = args.length > 1 ? Integer.parseInt(args[1]) : 30;
                purgeOldApplications(sender, days);
                break;
            case "reload":
                if (!sender.hasPermission("capplications.admin")) {
                    messageUtils.sendNoPermissionMessage(sender);
                    return true;
                }
                plugin.reloadCustomConfig();
                plugin.reloadConfig();
                messageUtils.sendMessage(sender, "reload_success");
                break;
            case "cooldown":
                if (!sender.hasPermission("capplications.admin")) {
                    messageUtils.sendNoPermissionMessage(sender);
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /app cooldown <player>");
                    return true;
                }
                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer != null) {
                    checkApplicationCooldown(sender, targetPlayer);
                } else {
                    messageUtils.sendErrorMessage(sender, "Player not found.");
                }
                break;
            default:
                // Check if it's a role name
                if (validator.isValidRole(subcommand)) {
                    if (!(sender instanceof Player)) {
                        messageUtils.sendMessage(sender, "player_only");
                        return true;
                    }
                    startApplication((Player) sender, subcommand);
                } else {
                    showHelp(sender);
                }
                break;
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== cApplications Help ===");
        sender.sendMessage(ChatColor.YELLOW + "/app" + ChatColor.WHITE + " - View available applications");
        sender.sendMessage(ChatColor.YELLOW + "/app <role>" + ChatColor.WHITE + " - Start application for role");
        sender.sendMessage(ChatColor.YELLOW + "/app continue" + ChatColor.WHITE + " - Continue pending application");
        sender.sendMessage(ChatColor.YELLOW + "/app cancel" + ChatColor.WHITE + " - Cancel current application");
        sender.sendMessage(ChatColor.YELLOW + "/app status" + ChatColor.WHITE + " - View your application status");
        sender.sendMessage(ChatColor.YELLOW + "/app history" + ChatColor.WHITE + " - View your application history");
        sender.sendMessage(ChatColor.YELLOW + "/app roles" + ChatColor.WHITE + " - List available roles");

        if (sender.hasPermission("capplications.manage")) {
            sender.sendMessage(ChatColor.AQUA + "Admin Commands:");
            sender.sendMessage(ChatColor.YELLOW + "/app accept <player>" + ChatColor.WHITE + " - Accept application");
            sender.sendMessage(ChatColor.YELLOW + "/app deny <player> [reason]" + ChatColor.WHITE + " - Deny application");
            sender.sendMessage(ChatColor.YELLOW + "/app stats" + ChatColor.WHITE + " - View statistics");
        }

        if (sender.hasPermission("capplications.admin")) {
            sender.sendMessage(ChatColor.RED + "Super Admin Commands:");
            sender.sendMessage(ChatColor.YELLOW + "/app reload" + ChatColor.WHITE + " - Reload configuration");
            sender.sendMessage(ChatColor.YELLOW + "/app purge [days]" + ChatColor.WHITE + " - Purge old applications");
            sender.sendMessage(ChatColor.YELLOW + "/app cooldown <player>" + ChatColor.WHITE + " - Check cooldowns");
        }
    }

    private void startApplication(Player player, String role) {
        UUID playerId = player.getUniqueId();

        // Check permissions
        if (!validator.canApplyForRole(player, role)) {
            messageUtils.sendNoPermissionMessage(player);
            return;
        }

        // Check if player has active application
        if (validator.hasActiveApplication(playerId)) {
            messageUtils.sendMessage(player, "application.already_applied");
            return;
        }

        // Check cooldown
        long cooldown = validator.getApplicationCooldown(playerId, role);
        if (cooldown > 0) {
            String timeLeft = formatDuration(cooldown);
            messageUtils.sendMessage(player, "application.cooldown", timeLeft, role);
            return;
        }

        // Check requirements
        if (!validator.meetsRoleRequirements(player, role)) {
            List<String> requirements = plugin.getCustomConfig().getStringList("applications." + role + ".requirements");
            messageUtils.sendMessage(player, "application.requirements_not_met");
            for (String requirement : requirements) {
                player.sendMessage(ChatColor.RED + "- " + requirement);
            }
            return;
        }

        // Start application
        List<String> questions = plugin.getCustomConfig().getStringList("applications." + role + ".questions");
        if (questions.isEmpty()) {
            messageUtils.sendErrorMessage(player, "No questions configured for this role.");
            return;
        }

        FileConfiguration applicationConfig = new org.bukkit.configuration.file.YamlConfiguration();
        applicationConfig.set("playerName", player.getName());
        applicationConfig.set("role", role);
        applicationConfig.set("status", "in progress");
        applicationConfig.set("startTime", System.currentTimeMillis());
        applicationConfig.set("questions", questions);
        applicationConfig.set("answers", new ArrayList<String>());

        plugin.saveApplication(playerId, applicationConfig);
        plugin.getPlayerAnswers().put(playerId, new ArrayList<>());
        plugin.getPlayerQuestionIndex().put(playerId, 0);

        messageUtils.sendMessage(player, "application.started", role);
        askNextQuestion(player);
    }

    private void askNextQuestion(Player player) {
        UUID playerId = player.getUniqueId();
        Integer questionIndex = plugin.getPlayerQuestionIndex().get(playerId);
        if (questionIndex == null) return;

        FileConfiguration applicationConfig = plugin.loadApplication(playerId);
        if (applicationConfig == null) return;

        List<String> questions = applicationConfig.getStringList("questions");

        if (questionIndex < questions.size()) {
            String question = ChatColor.translateAlternateColorCodes('&', questions.get(questionIndex));
            player.sendMessage(ChatColor.YELLOW + "Question " + (questionIndex + 1) + "/" + questions.size() + ": " + question);
            player.sendMessage(ChatColor.GRAY + "Type your answer in chat:");
        } else {
            completeApplication(player);
        }
    }

    private void completeApplication(Player player) {
        UUID playerId = player.getUniqueId();
        FileConfiguration applicationConfig = plugin.loadApplication(playerId);

        if (applicationConfig != null) {
            String role = applicationConfig.getString("role");
            applicationConfig.set("status", "pending");
            applicationConfig.set("submissionTime", System.currentTimeMillis());
            plugin.saveApplication(playerId, applicationConfig);

            plugin.logAction(player.getName(), "SUBMIT", playerId, role);
            messageUtils.sendMessage(player, "application.completed", role);

            // Play sound
            try {
                Sound sound = Sound.valueOf(plugin.getConfig().getString("sounds.application_submit", "ENTITY_EXPERIENCE_ORB_PICKUP"));
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (Exception ignored) {}

            // Notify admins
            notifyAdminsOfNewApplication(player.getName(), role);
        }

        plugin.getPlayerQuestionIndex().remove(playerId);
        plugin.getPlayerAnswers().remove(playerId);
    }

    private void processAcceptance(CommandSender sender, UUID playerId) {
        FileConfiguration applicationConfig = plugin.loadApplication(playerId);
        if (applicationConfig == null) {
            messageUtils.sendMessage(sender, "application.not_found");
            return;
        }

        String role = applicationConfig.getString("role");
        applicationConfig.set("status", "accepted");
        applicationConfig.set("acceptedBy", sender.getName());
        applicationConfig.set("acceptedTime", System.currentTimeMillis());

        plugin.saveApplication(playerId, applicationConfig);
        plugin.deleteApplication(playerId);
        plugin.savePlayerStatus(playerId, role, "accepted", null, sender.getName());

        // Grant permissions
        grantRolePermissions(playerId, role);

        messageUtils.sendMessage(sender, "admin.accepted_application", Bukkit.getOfflinePlayer(playerId).getName(), role);

        // Notify the player if online
        Player targetPlayer = Bukkit.getPlayer(playerId);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            messageUtils.sendMessage(targetPlayer, "application.accepted", role, sender.getName());
            try {
                Sound sound = Sound.valueOf(plugin.getConfig().getString("sounds.application_accept", "ENTITY_PLAYER_LEVELUP"));
                targetPlayer.playSound(targetPlayer.getLocation(), sound, 1.0f, 1.0f);
            } catch (Exception ignored) {}
        }

        plugin.logAction(sender.getName(), "ACCEPT", playerId, role);
    }

    private void processDenial(CommandSender sender, UUID playerId, String reason) {
        FileConfiguration applicationConfig = plugin.loadApplication(playerId);
        if (applicationConfig == null) {
            messageUtils.sendMessage(sender, "application.not_found");
            return;
        }

        String role = applicationConfig.getString("role");
        applicationConfig.set("status", "denied");
        applicationConfig.set("denialReason", reason);
        applicationConfig.set("deniedBy", sender.getName());
        applicationConfig.set("deniedTime", System.currentTimeMillis());

        plugin.saveApplication(playerId, applicationConfig);
        plugin.deleteApplication(playerId);
        plugin.savePlayerStatus(playerId, role, "denied", reason, sender.getName());

        messageUtils.sendMessage(sender, "admin.denied_application", Bukkit.getOfflinePlayer(playerId).getName(), role, reason);

        // Notify the player if online
        Player targetPlayer = Bukkit.getPlayer(playerId);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            messageUtils.sendMessage(targetPlayer, "application.denied", role, sender.getName(), reason);
            try {
                Sound sound = Sound.valueOf(plugin.getConfig().getString("sounds.application_deny", "ENTITY_VILLAGER_NO"));
                targetPlayer.playSound(targetPlayer.getLocation(), sound, 1.0f, 1.0f);
            } catch (Exception ignored) {}
        }

        plugin.logAction(sender.getName(), "DENY", playerId, role);
    }

    private void continueApplication(Player player) {
        UUID playerId = player.getUniqueId();
        FileConfiguration applicationConfig = plugin.loadApplication(playerId);

        if (applicationConfig == null || !"in progress".equals(applicationConfig.getString("status"))) {
            messageUtils.sendMessage(player, "application.not_found");
            return;
        }

        String role = applicationConfig.getString("role");
        List<String> answers = applicationConfig.getStringList("answers");

        plugin.getPlayerAnswers().put(playerId, new ArrayList<>(answers));
        plugin.getPlayerQuestionIndex().put(playerId, answers.size());

        messageUtils.sendMessage(player, "application.continuing", role);
        askNextQuestion(player);
    }

    private void cancelApplication(Player player) {
        UUID playerId = player.getUniqueId();
        FileConfiguration applicationConfig = plugin.loadApplication(playerId);

        if (applicationConfig == null || !"in progress".equals(applicationConfig.getString("status"))) {
            messageUtils.sendMessage(player, "application.not_found");
            return;
        }

        String role = applicationConfig.getString("role");
        plugin.deleteApplication(playerId);
        plugin.getPlayerQuestionIndex().remove(playerId);
        plugin.getPlayerAnswers().remove(playerId);

        messageUtils.sendMessage(player, "application.cancelled", role);
        plugin.logAction(player.getName(), "CANCEL", playerId, role);
    }

    private void showApplicationStatus(Player player) {
        UUID playerId = player.getUniqueId();
        List<ItemStack> items = new ArrayList<>();

        JSONArray applications = plugin.loadPlayerStatus(playerId);
        for (Object obj : applications) {
            JSONObject application = (JSONObject) obj;
            String role = (String) application.get("role");
            String status = (String) application.get("status");
            long timestamp = (long) application.get("timestamp");

            ItemStack item = createStatusItem(role, status, application, timestamp);
            items.add(item);
        }

        if (items.isEmpty()) {
            messageUtils.sendInfoMessage(player, "You have no application history.");
            return;
        }

        String statusTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.titles.status", "Your Applications"));
        PaginatedGUI.showGUI(player, items, 0, statusTitle);
    }

    private void showApplicationHistory(CommandSender sender, UUID targetId) {
        JSONArray applications = plugin.loadPlayerStatus(targetId);
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);

        sender.sendMessage(ChatColor.GOLD + "Application History for: " + ChatColor.WHITE + target.getName());
        sender.sendMessage(ChatColor.GRAY + "----------------------------------------");

        if (applications.isEmpty()) {
            messageUtils.sendInfoMessage(sender, "No application history found.");
            return;
        }

        for (Object obj : applications) {
            JSONObject app = (JSONObject) obj;
            String role = (String) app.get("role");
            String status = (String) app.get("status");
            long timestamp = (long) app.get("timestamp");

            LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
            String formattedDate = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));

            ChatColor statusColor = getStatusColor(status);
            sender.sendMessage(statusColor + "● " + role + " - " + status + ChatColor.GRAY + " (" + formattedDate + ")");
        }
    }

    private void showApplicationStatistics(CommandSender sender) {
        Map<String, Integer> roleStats = new HashMap<>();
        Map<String, Integer> statusStats = new HashMap<>();
        int totalApplications = 0;

        File playerdataFolder = new File(plugin.getDataFolder(), "playerdata");
        File[] files = playerdataFolder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".json")) {
                    JSONArray applications = plugin.loadPlayerStatus(
                            UUID.fromString(file.getName().replace(".json", "")));

                    for (Object obj : applications) {
                        JSONObject app = (JSONObject) obj;
                        String role = (String) app.get("role");
                        String status = (String) app.get("status");

                        roleStats.merge(role, 1, Integer::sum);
                        statusStats.merge(status, 1, Integer::sum);
                        totalApplications++;
                    }
                }
            }
        }

        sender.sendMessage(ChatColor.GOLD + "======= Application Statistics =======");
        sender.sendMessage(ChatColor.YELLOW + "Total Applications: " + ChatColor.WHITE + totalApplications);
        sender.sendMessage("");
        sender.sendMessage(ChatColor.AQUA + "By Status:");
        statusStats.forEach((status, count) ->
                sender.sendMessage(ChatColor.WHITE + "  " + status + ": " + count));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.AQUA + "By Role:");
        roleStats.forEach((role, count) ->
                sender.sendMessage(ChatColor.WHITE + "  " + role + ": " + count));
    }

    private void showAvailableRoles(CommandSender sender) {
        Set<String> roles = plugin.getCustomConfig().getConfigurationSection("applications").getKeys(false);

        sender.sendMessage(ChatColor.GOLD + "Available Roles:");
        for (String role : roles) {
            String displayName = plugin.getCustomConfig().getString("applications." + role + ".display_name", role);
            String description = plugin.getCustomConfig().getString("applications." + role + ".description", "");
            List<String> requirements = plugin.getCustomConfig().getStringList("applications." + role + ".requirements");

            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ChatColor.YELLOW + "● " + displayName));
            if (!description.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "  " + description);
            }
            if (!requirements.isEmpty() && sender.hasPermission("capplications.manage")) {
                sender.sendMessage(ChatColor.GRAY + "  Requirements: " + String.join(", ", requirements));
            }
        }
    }

    private void purgeOldApplications(CommandSender sender, int days) {
        long cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        int purgedCount = 0;

        File playerdataFolder = new File(plugin.getDataFolder(), "playerdata");
        File[] files = playerdataFolder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".json")) {
                    UUID playerId = UUID.fromString(file.getName().replace(".json", ""));
                    JSONArray applications = plugin.loadPlayerStatus(playerId);

                    applications.removeIf(obj -> {
                        JSONObject app = (JSONObject) obj;
                        long timestamp = (long) app.get("timestamp");
                        return timestamp < cutoffTime;
                    });

                    if (applications.isEmpty()) {
                        file.delete();
                        purgedCount++;
                    } else {
                        plugin.savePlayerStatusArray(playerId, applications);
                    }
                }
            }
        }

        messageUtils.sendMessage(sender, "admin.purged_applications", purgedCount);
    }

    private void checkApplicationCooldown(CommandSender sender, Player target) {
        sender.sendMessage(ChatColor.GOLD + "Application Cooldowns for " + target.getName() + ":");

        Set<String> roles = plugin.getCustomConfig().getConfigurationSection("applications").getKeys(false);
        boolean hasCooldowns = false;

        for (String role : roles) {
            long cooldown = validator.getApplicationCooldown(target.getUniqueId(), role);
            if (cooldown > 0) {
                String timeLeft = formatDuration(cooldown);
                sender.sendMessage(ChatColor.YELLOW + "● " + role + ": " + ChatColor.RED + timeLeft);
                hasCooldowns = true;
            } else {
                sender.sendMessage(ChatColor.YELLOW + "● " + role + ": " + ChatColor.GREEN + "Available");
            }
        }

        if (!hasCooldowns) {
            sender.sendMessage(ChatColor.GREEN + "No active cooldowns!");
        }
    }

    // Helper methods
    private UUID parsePlayerUUID(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(input);
            return player.hasPlayedBefore() ? player.getUniqueId() : null;
        }
    }

    private void grantRolePermissions(UUID playerId, String role) {
        try {
            // This would integrate with LuckPerms
            // Implementation depends on specific permission setup
            plugin.getLogger().info("Granted permissions for role '" + role + "' to player: " + playerId);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to grant permissions for role: " + role);
        }
    }

    private void notifyAdminsOfNewApplication(String playerName, String role) {
        String message = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.admin.notification",
                        "&b[cApplications] &f{0}&b submitted an application for &f{1}&b."));
        message = message.replace("{0}", playerName).replace("{1}", role);

        String finalMessage = message;
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("capplications.notify"))
                .forEach(p -> p.sendMessage(finalMessage));
    }

    private ItemStack createStatusItem(String role, String status, JSONObject application, long timestamp) {
        Material material = getStatusMaterial(status);
        ChatColor statusColor = getStatusColor(status);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String displayName = plugin.getCustomConfig().getString("applications." + role + ".display_name", role);
        meta.setDisplayName(ChatColor.GOLD + "Application: " + ChatColor.translateAlternateColorCodes('&', displayName));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Status: " + statusColor + status);

        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        lore.add(ChatColor.GRAY + "Date: " + date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));

        if (application.containsKey("reason")) {
            lore.add(ChatColor.RED + "Reason: " + application.get("reason"));
        }
        if (application.containsKey("handler")) {
            lore.add(ChatColor.AQUA + "Handler: " + application.get("handler"));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Material getStatusMaterial(String status) {
        switch (status.toLowerCase()) {
            case "accepted": return Material.WRITTEN_BOOK;
            case "denied": return Material.BARRIER;
            case "pending": return Material.WRITABLE_BOOK;
            case "in progress": return Material.PAPER;
            case "expired": return Material.GRAY_STAINED_GLASS;
            default: return Material.BOOK;
        }
    }

    private ChatColor getStatusColor(String status) {
        switch (status.toLowerCase()) {
            case "accepted": return ChatColor.GREEN;
            case "denied": return ChatColor.RED;
            case "pending": return ChatColor.YELLOW;
            case "in progress": return ChatColor.AQUA;
            case "expired": return ChatColor.GRAY;
            default: return ChatColor.WHITE;
        }
    }

    private String formatDuration(long milliseconds) {
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }

    private void showAvailableApplications(Player player) {
        List<ItemStack> items = new ArrayList<>();

        File applicationsFolder = new File(plugin.getDataFolder(), "applications");
        File[] applicationFiles = applicationsFolder.listFiles();

        if (applicationFiles != null) {
            for (File file : applicationFiles) {
                if (file.getName().endsWith(".yml")) {
                    FileConfiguration applicationConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
                    String status = applicationConfig.getString("status", "unknown");

                    // Only show pending applications for admins, all roles for regular players
                    if (player.hasPermission("capplications.manage")) {
                        if (!"pending".equals(status)) continue;
                    } else {
                        // Show available roles to apply for
                        continue;
                    }

                    String role = applicationConfig.getString("role");
                    String playerName = applicationConfig.getString("playerName", "Unknown");
                    List<String> questions = applicationConfig.getStringList("questions");
                    List<String> answers = applicationConfig.getStringList("answers");
                    long submissionTime = applicationConfig.getLong("submissionTime");

                    ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD, 1);
                    ItemMeta meta = playerHead.getItemMeta();
                    UUID playerUUID = UUID.fromString(file.getName().replace(".yml", ""));

                    meta.setDisplayName(ChatColor.GOLD + playerName);

                    List<String> lore = new ArrayList<>();
                    String displayName = plugin.getCustomConfig().getString("applications." + role + ".display_name", role);
                    lore.add(ChatColor.YELLOW + "Role: " + ChatColor.translateAlternateColorCodes('&', displayName));
                    lore.add(ChatColor.YELLOW + "Status: " + ChatColor.AQUA + status);

                    LocalDateTime submitDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(submissionTime), ZoneId.systemDefault());
                    lore.add(ChatColor.GRAY + "Submitted: " + submitDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                    lore.add("");

                    // Show first few questions and answers
                    for (int i = 0; i < Math.min(questions.size(), 3); i++) {
                        String question = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', questions.get(i)));
                        lore.add(ChatColor.YELLOW + question);
                        lore.add(ChatColor.WHITE + (i < answers.size() ? answers.get(i) : "No answer"));
                        lore.add("");
                    }

                    if (questions.size() > 3) {
                        lore.add(ChatColor.GRAY + "... and " + (questions.size() - 3) + " more questions");
                    }

                    lore.add(ChatColor.GREEN + "Click to review application");

                    meta.setLore(lore);
                    playerHead.setItemMeta(meta);
                    items.add(playerHead);
                }
            }
        }

        if (items.isEmpty()) {
            if (player.hasPermission("capplications.manage")) {
                messageUtils.sendInfoMessage(player, "No pending applications found.");
            } else {
                // Show available roles to apply for
                showAvailableRoles(player);
            }
            return;
        }

        String availableTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.titles.available", "Available Applications"));
        PaginatedGUI.showGUI(player, items, 0, availableTitle);
    }
}