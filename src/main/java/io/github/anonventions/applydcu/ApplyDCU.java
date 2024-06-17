package io.github.anonventions.applydcu;

import io.github.anonventions.applydcu.commands.ApplyCommand;
import io.github.anonventions.applydcu.commands.ApplyTabCompleter;
import io.github.anonventions.applydcu.events.InventoryClickListener;
import io.github.anonventions.applydcu.events.PlayerChatListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ApplyDCU extends JavaPlugin {
    private FileConfiguration config;
    private Map<UUID, Integer> playerQuestionIndex;
    private Map<UUID, List<String>> playerAnswers;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        createApplicationsFolder();

        // Ensure default titles are set in config
        if (!config.contains("gui.titles.applications")) {
            config.set("gui.titles.applications", "Applications");
        }
        if (!config.contains("gui.titles.manage")) {
            config.set("gui.titles.manage", "Manage Application");
        }
        saveConfig();

        playerQuestionIndex = new HashMap<>();
        playerAnswers = new HashMap<>();

        ApplyCommand applyCommand = new ApplyCommand(this);
        this.getCommand("apply").setExecutor(applyCommand);
        this.getCommand("apply").setTabCompleter(new ApplyTabCompleter(this));

        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerChatListener(this), this);

        new BukkitRunnable() {
            @Override
            public void run() {
                checkExpiredApplications();
            }
        }.runTaskTimer(this, 0, 20 * 60 * 60 * 24); // Run every 24 hours
    }

    @Override
    public void onDisable() {
    }

    private void createApplicationsFolder() {
        File applicationsFolder = new File(getDataFolder(), "applications");
        if (!applicationsFolder.exists()) {
            applicationsFolder.mkdirs();
        }
    }

    public File getApplicationFile(UUID playerId) {
        return new File(getDataFolder() + "/applications", playerId.toString() + ".yml");
    }

    public File getPlayerStatusFile(UUID playerId) {
        return new File(getDataFolder() + "/applications", playerId.toString() + ".json");
    }

    public FileConfiguration loadApplication(UUID playerId) {
        File file = getApplicationFile(playerId);
        if (file.exists()) {
            return YamlConfiguration.loadConfiguration(file);
        } else {
            return null;
        }
    }

    public void saveApplication(UUID playerId, FileConfiguration applicationConfig) {
        try {
            applicationConfig.save(getApplicationFile(playerId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteApplication(UUID playerId) {
        File file = getApplicationFile(playerId);
        if (file.exists()) {
            file.delete();
        }
    }

    public void savePlayerStatus(UUID playerId, String role, String status) {
        File statusFile = getPlayerStatusFile(playerId);
        JSONArray applications = new JSONArray();

        if (statusFile.exists()) {
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(statusFile)) {
                Object obj = parser.parse(reader);
                applications = (JSONArray) obj;
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }

        // Remove any 'In-Progress' status for the same role
        applications.removeIf(app -> {
            JSONObject application = (JSONObject) app;
            return application.get("role").equals(role) && application.get("status").equals("in progress");
        });

        JSONObject applicationDetails = new JSONObject();
        applicationDetails.put("role", role);
        applicationDetails.put("status", status);
        applicationDetails.put("timestamp", System.currentTimeMillis());

        applications.add(applicationDetails);

        try (FileWriter writer = new FileWriter(statusFile)) {
            writer.write(applications.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JSONArray loadPlayerStatus(UUID playerId) {
        File statusFile = getPlayerStatusFile(playerId);
        if (statusFile.exists()) {
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(statusFile)) {
                Object obj = parser.parse(reader);
                return (JSONArray) obj;
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }
        return new JSONArray();
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
        File applicationsFolder = new File(getDataFolder(), "applications");
        File[] applicationFiles = applicationsFolder.listFiles();
        if (applicationFiles != null) {
            for (File file : applicationFiles) {
                FileConfiguration applicationConfig = YamlConfiguration.loadConfiguration(file);
                long submissionTime = applicationConfig.getLong("submissionTime");
                if (submissionTime > 0 && (currentTime - submissionTime) > (14 * 24 * 60 * 60 * 1000)) { // 14 days in milliseconds
                    UUID playerId = UUID.fromString(file.getName().replace(".yml", ""));
                    String role = applicationConfig.getString("role");
                    savePlayerStatus(playerId, role, "denied");
                    deleteApplication(playerId);
                }
            }
        }
    }
}
