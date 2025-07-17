package io.github.anonventions.applydcu;

import io.github.anonventions.applydcu.api.ApplicationService;
import io.github.anonventions.applydcu.api.ConfigurationService;
import io.github.anonventions.applydcu.commands.ApplyCommand;
import io.github.anonventions.applydcu.commands.ApplyTabCompleter;
import io.github.anonventions.applydcu.config.EnhancedConfigurationService;
import io.github.anonventions.applydcu.core.ServiceRegistry;
import io.github.anonventions.applydcu.events.InventoryClickListener;
import io.github.anonventions.applydcu.events.PlayerChatListener;
import io.github.anonventions.applydcu.services.EnhancedApplicationService;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ApplyDCU v2.0 - Modern service-based architecture with async operations
 */
public class ApplyDCU extends JavaPlugin {
    private static final Logger logger = LoggerFactory.getLogger(ApplyDCU.class);
    private static ApplyDCU instance;
    
    // Legacy fields for backward compatibility
    private FileConfiguration config;
    private Map<UUID, Integer> playerQuestionIndex;
    private Map<UUID, List<String>> playerAnswers;
    private Map<UUID, UUID> pendingDenials;
    
    // Service registry
    private ServiceRegistry serviceRegistry;

    public static ApplyDCU getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Initialize service registry
            serviceRegistry = ServiceRegistry.getInstance();
            
            // Initialize configuration
            saveDefaultConfig();
            config = getConfig();
            
            // Register services
            registerServices();
            
            // Validate configuration
            ConfigurationService configService = serviceRegistry.getService(ConfigurationService.class);
            configService.validateConfiguration().thenAccept(valid -> {
                if (!valid) {
                    logger.warn("Configuration validation failed, some features may not work correctly");
                }
            });
            
            createApplicationsFolder();
            ensureDefaultTitles();

            // Initialize legacy maps for backward compatibility
            playerQuestionIndex = new HashMap<>();
            playerAnswers = new HashMap<>();
            pendingDenials = new HashMap<>();

            // Register commands and events
            ApplyCommand applyCommand = new ApplyCommand(this);
            this.getCommand("apply").setExecutor(applyCommand);
            this.getCommand("apply").setTabCompleter(new ApplyTabCompleter(this));

            Bukkit.getPluginManager().registerEvents(new InventoryClickListener(this), this);
            Bukkit.getPluginManager().registerEvents(new PlayerChatListener(this), this);

            // Start maintenance tasks
            startMaintenanceTasks();
            
            logger.info("ApplyDCU v2.0 enabled successfully with modern architecture");
            
        } catch (Exception e) {
            logger.error("Failed to enable ApplyDCU v2.0", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            // Cleanup services
            if (serviceRegistry != null) {
                serviceRegistry.clear();
            }
            logger.info("ApplyDCU v2.0 disabled successfully");
        } catch (Exception e) {
            logger.error("Error during plugin disable", e);
        }
    }
    
    /**
     * Register all services in the service registry
     */
    private void registerServices() {
        // Configuration service
        EnhancedConfigurationService configService = new EnhancedConfigurationService(getDataFolder());
        serviceRegistry.registerService(ConfigurationService.class, configService);
        
        // Application service
        EnhancedApplicationService applicationService = new EnhancedApplicationService(getDataFolder());
        serviceRegistry.registerService(ApplicationService.class, applicationService);
        
        logger.info("Services registered successfully");
    }
    
    /**
     * Start maintenance tasks
     */
    private void startMaintenanceTasks() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    checkExpiredApplications();
                    checkInactivePlayers();
                } catch (Exception e) {
                    logger.error("Error during maintenance task", e);
                }
            }
        }.runTaskTimer(this, 0, 20 * 60 * 60 * 24); // Run every 24 hours
    }

    public void reloadPlugin(CommandSender sender) {
        try {
            reloadConfig();
            config = getConfig();
            
            // Reload configuration service
            ConfigurationService configService = serviceRegistry.getService(ConfigurationService.class);
            configService.reloadConfiguration().thenRun(() -> {
                sender.sendMessage(ChatColor.GREEN + "ApplyDCU v2.0 reloaded successfully.");
            }).exceptionally(throwable -> {
                sender.sendMessage(ChatColor.RED + "Failed to reload configuration: " + throwable.getMessage());
                logger.error("Configuration reload failed", throwable);
                return null;
            });
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Failed to reload plugin: " + e.getMessage());
            logger.error("Plugin reload failed", e);
        }
    }
    
    /**
     * Get service registry for accessing services
     */
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    private void ensureDefaultTitles() {
        if (!config.contains("gui.titles.applications")) {
            config.set("gui.titles.applications", "Applications");
        }
        if (!config.contains("gui.titles.manage")) {
            config.set("gui.titles.manage", "Manage Application");
        }
        saveConfig();
    }

    private void createApplicationsFolder() {
        File applicationsFolder = new File(getDataFolder(), "applications");
        if (!applicationsFolder.exists()) {
            applicationsFolder.mkdirs();
        }
    }

    // Legacy methods for backward compatibility
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
            logger.error("Failed to save application", e);
        }
    }

    public void deleteApplication(UUID playerId) {
        File file = getApplicationFile(playerId);
        if (file.exists()) {
            file.delete();
        }
    }

    public void savePlayerStatus(UUID playerId, String role, String status) {
        savePlayerStatus(playerId, role, status, "", "");
    }

    public void savePlayerStatus(UUID playerId, String role, String status, String denialReason, String deniedBy) {
        File statusFile = getPlayerStatusFile(playerId);
        JSONArray applications = new JSONArray();

        if (statusFile.exists()) {
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(statusFile)) {
                Object obj = parser.parse(reader);
                applications = (JSONArray) obj;
            } catch (IOException | ParseException e) {
                logger.error("Failed to read player status", e);
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
        if (!denialReason.isEmpty()) {
            applicationDetails.put("denialReason", denialReason);
        }
        if (!deniedBy.isEmpty()) {
            applicationDetails.put("deniedBy", deniedBy);
        }

        applications.add(applicationDetails);

        try (FileWriter writer = new FileWriter(statusFile)) {
            writer.write(applications.toJSONString());
        } catch (IOException e) {
            logger.error("Failed to write player status", e);
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
                logger.error("Failed to load player status", e);
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

    public Map<UUID, UUID> getPendingDenials() {
        return pendingDenials;
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

    private void checkInactivePlayers() {
        long currentTime = System.currentTimeMillis();
        File applicationsFolder = new File(getDataFolder(), "applications");
        File[] applicationFiles = applicationsFolder.listFiles();
        if (applicationFiles != null) {
            for (File file : applicationFiles) {
                UUID playerId = UUID.fromString(file.getName().replace(".yml", ""));
                JSONArray playerStatus = loadPlayerStatus(playerId);
                for (Object obj : playerStatus) {
                    JSONObject application = (JSONObject) obj;
                    String status = (String) application.get("status");
                    long timestamp = (long) application.get("timestamp");

                    if ("accepted".equals(status) && (currentTime - timestamp) > (18 * 24 * 60 * 60 * 1000)) { // 18 days in milliseconds
                        String role = (String) application.get("role");
                        removePlayerPermissions(playerId, role);
                        application.put("status", "inactive");
                    }
                }

                // Save the updated status
                try (FileWriter writer = new FileWriter(getPlayerStatusFile(playerId))) {
                    writer.write(playerStatus.toJSONString());
                } catch (IOException e) {
                    logger.error("Failed to save updated player status", e);
                }
            }
        }
    }

    private void removePlayerPermissions(UUID playerId, String role) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().getUser(playerId);
        if (user != null) {
            String permission = config.getString("permissions." + role);
            if (permission != null) {
                user.data().remove(Node.builder(permission).build());
                luckPerms.getUserManager().saveUser(user);
            }
        }
    }
}
