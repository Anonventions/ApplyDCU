package io.github.anonventions.applydcu;

import io.github.anonventions.applydcu.commands.ApplyCommand;
import io.github.anonventions.applydcu.commands.ApplyTabCompleter;
import io.github.anonventions.applydcu.events.InventoryClickListener;
import io.github.anonventions.applydcu.events.PlayerChatListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ApplyDCU extends JavaPlugin {
    private FileConfiguration config;
    private FileConfiguration applicationsConfig;
    private Map<UUID, Integer> playerQuestionIndex;
    private Map<UUID, List<String>> playerAnswers;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadApplicationsConfig();

        playerQuestionIndex = new HashMap<>();
        playerAnswers = new HashMap<>();

        // Register commands
        ApplyCommand applyCommand = new ApplyCommand(this);
        this.getCommand("apply").setExecutor(applyCommand);
        this.getCommand("apply").setTabCompleter(new ApplyTabCompleter(this));

        // Register events
        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerChatListener(this), this);

        // Schedule application check task
        new BukkitRunnable() {
            @Override
            public void run() {
                checkExpiredApplications();
            }
        }.runTaskTimer(this, 0, 20 * 60 * 60 * 24); // Run every 24 hours
    }

    @Override
    public void onDisable() {
        saveApplicationsConfig();
    }

    public FileConfiguration getApplicationsConfig() {
        return applicationsConfig;
    }

    public void loadApplicationsConfig() {
        File applicationsFile = new File(getDataFolder(), "applications.yml");
        if (!applicationsFile.exists()) {
            try {
                applicationsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        applicationsConfig = YamlConfiguration.loadConfiguration(applicationsFile);
    }

    public void saveApplicationsConfig() {
        try {
            applicationsConfig.save(new File(getDataFolder(), "applications.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getCustomConfig() {
        return config;
    }

    public Map<UUID, Integer> getPlayerQuestionIndex() {
        return playerQuestionIndex;
    }

    public Map<UUID, List<String>> getPlayerAnswers() {
        return playerAnswers;
    }

    private void checkExpiredApplications() {
        long currentTime = System.currentTimeMillis();
        if (applicationsConfig.contains("applications")) {
            for (String key : applicationsConfig.getConfigurationSection("applications").getKeys(false)) {
                long submissionTime = applicationsConfig.getLong("applications." + key + ".submissionTime");
                if (submissionTime > 0 && (currentTime - submissionTime) > (14 * 24 * 60 * 60 * 1000)) { // 14 days in milliseconds
                    applicationsConfig.set("applications." + key + ".status", "denied");
                    saveApplicationsConfig();

                    UUID playerId = UUID.fromString(key);
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        player.sendMessage(ChatColor.RED + "Your application has been denied due to inactivity.");
                    }
                }
            }
        }
    }
}
