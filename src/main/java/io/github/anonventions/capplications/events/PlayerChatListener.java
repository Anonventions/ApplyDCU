package io.github.anonventions.capplications.events;

import io.github.anonventions.capplications.CApplications;
import io.github.anonventions.capplications.gui.PaginatedGUI;
import io.github.anonventions.capplications.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerChatListener implements Listener {

    private final CApplications plugin;
    private final MessageUtils messageUtils;

    public PlayerChatListener(CApplications plugin) {
        this.plugin = plugin;
        this.messageUtils = new MessageUtils(plugin);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (plugin.getPlayerQuestionIndex().containsKey(playerId)) {
            event.setCancelled(true);
            handleApplicationAnswer(player, event.getMessage());
        } else if (plugin.getPendingDenials().containsKey(playerId)) {
            event.setCancelled(true);
            handleDenialReason(player, event.getMessage());
        }
    }

    private void handleApplicationAnswer(Player player, String answer) {
        UUID playerId = player.getUniqueId();
        int questionIndex = plugin.getPlayerQuestionIndex().get(playerId);

        List<String> answers = plugin.getPlayerAnswers().computeIfAbsent(playerId, k -> new ArrayList<>());
        answers.add(answer);

        plugin.getPlayerAnswers().put(playerId, answers);
        plugin.getPlayerQuestionIndex().put(playerId, questionIndex + 1);

        FileConfiguration applicationConfig = plugin.loadApplication(playerId);
        if (applicationConfig != null) {
            applicationConfig.set("answers", answers);
            plugin.saveApplication(playerId, applicationConfig);
        }

        askNextQuestion(player);
    }

    private void handleDenialReason(Player player, String reason) {
        UUID playerId = player.getUniqueId();
        UUID applicationId = plugin.getPendingDenials().remove(playerId);

        FileConfiguration applicationConfig = plugin.loadApplication(applicationId);
        if (applicationConfig != null) {
            String role = applicationConfig.getString("role");
            applicationConfig.set("status", "denied");
            applicationConfig.set("denialReason", reason);
            applicationConfig.set("deniedBy", player.getName());

            plugin.saveApplication(applicationId, applicationConfig);
            plugin.savePlayerStatus(applicationId, role, "denied", reason, player.getName());
            plugin.logAction(player.getName(), "DENY", applicationId, role);

            messageUtils.sendMessage(player, "admin.denied_application",
                    Bukkit.getOfflinePlayer(applicationId).getName(), role, reason);

            // Notify target player
            Player targetPlayer = Bukkit.getPlayer(applicationId);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                messageUtils.sendMessage(targetPlayer, "application.denied", role, player.getName(), reason);
            }
        } else {
            messageUtils.sendErrorMessage(player, "No application found for that player.");
        }

        String title = messageUtils.formatMessage("gui.titles.applications");
        PaginatedGUI.refreshGUI(player, plugin, title);
    }

    private void askNextQuestion(Player player) {
        UUID playerId = player.getUniqueId();
        int questionIndex = plugin.getPlayerQuestionIndex().get(playerId);
        FileConfiguration applicationConfig = plugin.loadApplication(playerId);

        if (applicationConfig == null) return;

        List<String> questions = applicationConfig.getStringList("questions");

        if (questionIndex < questions.size()) {
            String question = ChatColor.translateAlternateColorCodes('&', questions.get(questionIndex));
            player.sendMessage(ChatColor.YELLOW + question);
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

            // Notify admins
            notifyAdminsOfNewApplication(player.getName(), role);
        }

        plugin.getPlayerQuestionIndex().remove(playerId);
        plugin.getPlayerAnswers().remove(playerId);
    }

    private void notifyAdminsOfNewApplication(String playerName, String role) {
        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("capplications.manage")) {
                messageUtils.sendMessage(admin, "admin.notification", playerName, role);
            }
        }
    }
}