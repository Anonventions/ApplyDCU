package io.github.anonventions.capplications.utils;

import io.github.anonventions.capplications.CApplications;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ConfigManager {
    private final CApplications plugin;
    private FileConfiguration applicationsConfig;
    private File applicationsConfigFile;

    public ConfigManager(CApplications plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();

        // Load applications config
        loadApplicationsConfig();

        // Ensure default values are set
        setDefaultValues();
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        loadApplicationsConfig();
    }

    private void loadApplicationsConfig() {
        applicationsConfigFile = new File(plugin.getDataFolder(), "applications.yml");

        if (!applicationsConfigFile.exists()) {
            createApplicationsConfig();
        }

        applicationsConfig = YamlConfiguration.loadConfiguration(applicationsConfigFile);
    }

    private void createApplicationsConfig() {
        try {
            InputStream inputStream = plugin.getResource("applications.yml");
            if (inputStream != null) {
                Files.copy(inputStream, applicationsConfigFile.toPath());
            } else {
                applicationsConfigFile.createNewFile();
                // Set default applications config
                setDefaultApplicationsConfig();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create applications.yml: " + e.getMessage());
        }
    }

    private void setDefaultApplicationsConfig() {
        // This would set default application configurations
        // Implementation depends on your specific needs
    }

    private void setDefaultValues() {
        FileConfiguration config = plugin.getConfig();

        // Set default GUI titles if not present
        if (!config.contains("gui.titles.applications")) {
            config.set("gui.titles.applications", "&6&lApplications");
        }
        if (!config.contains("gui.titles.manage")) {
            config.set("gui.titles.manage", "&f♜ Manage Application");
        }
        if (!config.contains("gui.titles.status")) {
            config.set("gui.titles.status", "&f♚ Application Status");
        }
        if (!config.contains("gui.titles.available")) {
            config.set("gui.titles.available", "&f♛ Available Applications");
        }

        // Set default messages if not present
        if (!config.contains("messages.no_permission")) {
            config.set("messages.no_permission", "&cYou don't have permission to use this command.");
        }
        if (!config.contains("messages.application.accepted")) {
            config.set("messages.application.accepted", "&aYour application for &f{0}&a has been accepted by &f{1}&a!");
        }
        if (!config.contains("messages.application.denied")) {
            config.set("messages.application.denied", "&cYour application for &f{0}&c has been denied by &f{1}&c. Reason: &f{2}");
        }

        plugin.saveConfig();
    }

    public FileConfiguration getApplicationsConfig() {
        return applicationsConfig;
    }

    public void saveApplicationsConfig() {
        try {
            applicationsConfig.save(applicationsConfigFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save applications.yml: " + e.getMessage());
        }
    }
}