package io.github.anonventions.applydcu.events;

import io.github.anonventions.applydcu.ApplyDCU;
import io.github.anonventions.applydcu.gui.PaginatedGUI;
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

    private final ApplyDCU plugin;

    public PlayerChatListener(ApplyDCU plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (plugin.getPlayerQuestionIndex().containsKey(playerId)) {
            event.setCancelled(true); // Prevent message from being broadcasted

            int questionIndex = plugin.getPlayerQuestionIndex().get(playerId);
            List<String> answers = plugin.getPlayerAnswers().get(playerId);
            if (answers == null) {
                answers = new ArrayList<>();
            }
            answers.add(event.getMessage());

            plugin.getPlayerAnswers().put(playerId, answers);
            plugin.getPlayerQuestionIndex().put(playerId, questionIndex + 1);

            FileConfiguration applicationConfig = plugin.loadApplication(playerId);
            applicationConfig.set("answers", answers);
            plugin.saveApplication(playerId, applicationConfig);

            askNextQuestion(player);
        } else if (plugin.getPendingDenials().containsKey(playerId)) {
            event.setCancelled(true); // Prevent message from being broadcasted

            UUID applicationId = plugin.getPendingDenials().remove(playerId);
            FileConfiguration applicationConfig = plugin.loadApplication(applicationId);
            if (applicationConfig != null) {
                String role = applicationConfig.getString("role");
                applicationConfig.set("status", "denied");
                applicationConfig.set("denialReason", event.getMessage());
                applicationConfig.set("deniedBy", player.getName());
                plugin.saveApplication(applicationId, applicationConfig);
                plugin.savePlayerStatus(applicationId, role, "denied", event.getMessage(), player.getName());

                player.sendMessage(ChatColor.RED + "Denied application for player: " + Bukkit.getOfflinePlayer(applicationId).getName() + " for role: " + role + " with reason: " + event.getMessage());

                Player targetPlayer = Bukkit.getPlayer(applicationId);
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    targetPlayer.sendMessage(ChatColor.RED + "Your application for " + role + " has been denied. Reason: " + event.getMessage());
                }
            } else {
                player.sendMessage(ChatColor.RED + "No application found for that player.");
            }
            PaginatedGUI.refreshGUI(player, plugin, ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.titles.applications")));
        }
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
}
