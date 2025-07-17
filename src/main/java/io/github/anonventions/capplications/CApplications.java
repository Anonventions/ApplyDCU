package io.github.anonventions.capplications;

import io.github.anonventions.capplications.commands.ApplicationCommand;
import io.github.anonventions.capplications.commands.ApplicationTabCompleter;
import io.github.anonventions.capplications.events.InventoryClickListener;
import io.github.anonventions.capplications.events.PlayerChatListener;
import io.github.anonventions.capplications.utils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CApplications extends JavaPlugin {

    private static CApplications instance;
    private ConfigManager configManager;
    private FileConfiguration customConfig;

    // In-memory storage for active applications
    private final Map<UUID, Integer> playerQuestionIndex = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> playerAnswers = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> pendingDenials = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        // Initialize config manager
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        this.customConfig = configManager.getApplicationsConfig();

        // Create necessary directories
        createDirectories();

        // Register commands
        getCommand("application").setExecutor(new ApplicationCommand(this));
        getCommand("application").setTabCompleter(new ApplicationTabCompleter(this));

        // Register events
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(this), this);

        // Start cleanup task
        startCleanupTask();

        getLogger().info("cApplications has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save any pending data
        getLogger().info("cApplications has been disabled!");
    }

    private void createDirectories() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File applicationsFolder = new File(dataFolder, "applications");
        if (!applicationsFolder.exists()) {
            applicationsFolder.mkdirs();
        }

        File playerdataFolder = new File(dataFolder, "playerdata");
        if (!playerdataFolder.exists()) {
            playerdataFolder.mkdirs();
        }

        File logsFolder = new File(dataFolder, "logs");
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }
    }

    private void startCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            cleanupExpiredApplications();
        }, 20L * 60 * 60, 20L * 60 * 60); // Run every hour
    }

    private void cleanupExpiredApplications() {
        File applicationsFolder = new File(getDataFolder(), "applications");
        File[] files = applicationsFolder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".yml")) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    if ("pending".equals(config.getString("status"))) {
                        long submissionTime = config.getLong("submissionTime", 0);
                        long expiryTime = getConfig().getInt("settings.expiry_days", 14) * 24 * 60 * 60 * 1000L;

                        if (System.currentTimeMillis() - submissionTime > expiryTime) {
                            // Mark as expired
                            config.set("status", "expired");
                            try {
                                config.save(file);
                            } catch (IOException e) {
                                getLogger().warning("Failed to update expired application: " + file.getName());
                            }
                        }
                    }
                }
            }
        }
    }

    // Application management methods
    public FileConfiguration loadApplication(UUID playerId) {
        File file = new File(getDataFolder() + "/applications", playerId.toString() + ".yml");
        if (!file.exists()) {
            return null;
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void saveApplication(UUID playerId, FileConfiguration config) {
        File file = new File(getDataFolder() + "/applications", playerId.toString() + ".yml");
        try {
            config.save(file);
        } catch (IOException e) {
            getLogger().severe("Failed to save application for " + playerId + ": " + e.getMessage());
        }
    }

    public void deleteApplication(UUID playerId) {
        File file = new File(getDataFolder() + "/applications", playerId.toString() + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

    public JSONArray loadPlayerStatus(UUID playerId) {
        File file = new File(getDataFolder() + "/playerdata", playerId.toString() + ".json");
        if (!file.exists()) {
            return new JSONArray();
        }

        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            JSONParser parser = new JSONParser();
            return (JSONArray) parser.parse(content);
        } catch (Exception e) {
            getLogger().warning("Failed to load player status for " + playerId + ": " + e.getMessage());
            return new JSONArray();
        }
    }

    @SuppressWarnings("unchecked")
    public void savePlayerStatus(UUID playerId, String role, String status, String reason, String handler) {
        JSONArray statuses = loadPlayerStatus(playerId);

        JSONObject entry = new JSONObject();
        entry.put("role", role);
        entry.put("status", status);
        entry.put("timestamp", System.currentTimeMillis());
        if (reason != null) {
            entry.put("reason", reason);
        }
        if (handler != null) {
            entry.put("handler", handler);
        }

        statuses.add(entry);
        savePlayerStatusArray(playerId, statuses);
    }

    @SuppressWarnings("unchecked")
    public void savePlayerStatus(UUID playerId, String role, String status) {
        savePlayerStatus(playerId, role, status, null, null);
    }

    public void savePlayerStatusArray(UUID playerId, JSONArray statuses) {
        File file = new File(getDataFolder() + "/playerdata", playerId.toString() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(statuses.toJSONString());
        } catch (IOException e) {
            getLogger().severe("Failed to save player status for " + playerId + ": " + e.getMessage());
        }
    }

    public void logAction(String performer, String action, UUID targetId, String role) {
        File logFile = new File(getDataFolder() + "/logs", "actions.log");
        try (FileWriter writer = new FileWriter(logFile, true)) {
            String logEntry = String.format("[%d] %s performed %s on %s for role %s%n",
                    System.currentTimeMillis(), performer, action, targetId.toString(), role);
            writer.write(logEntry);
        } catch (IOException e) {
            getLogger().warning("Failed to log action: " + e.getMessage());
        }
    }

    // Getters
    public static CApplications getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public FileConfiguration getCustomConfig() {
        return customConfig;
    }

    public Map<UUID, Integer> getPlayerQuestionIndex() {
        return playerQuestionIndex;
    }

    public Map<UUID, List<String>> getPlayerAnswers() {
        return playerAnswers;
    }

    public Map<UUID, UUID> getPendingDenials() {
        return pendingDenials;
    }

    public void reloadCustomConfig() {
        configManager.reloadConfigs();
        this.customConfig = configManager.getApplicationsConfig();
    }
}