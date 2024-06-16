package io.github.anonventions.applydcu.events;

import io.github.anonventions.applydcu.ApplyDCU;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

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

            plugin.getApplicationsConfig().set("applications." + playerId + ".answers", answers);
            plugin.saveApplicationsConfig();

            askNextQuestion(player);
        }
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
}
//Need to fucking fix.