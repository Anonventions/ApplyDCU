package io.github.anonventions.applydcu.config;

import io.github.anonventions.applydcu.api.ConfigurationService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Simple configuration service that works without external dependencies
 * This provides basic functionality while we resolve dependency issues
 */
public class SimpleConfigurationService implements ConfigurationService {
    
    private final File configFile;
    private Properties config;
    
    public SimpleConfigurationService(File dataFolder) {
        this.configFile = new File(dataFolder, "config.properties");
        loadConfiguration();
    }
    
    @Override
    public CompletableFuture<Void> reloadConfiguration() {
        return CompletableFuture.runAsync(() -> {
            loadConfiguration();
        });
    }
    
    @Override
    public String[] getApplicationQuestions(String role) {
        String questionsKey = "applications." + role + ".questions";
        String questionsStr = config.getProperty(questionsKey, "");
        if (questionsStr.isEmpty()) {
            return new String[0];
        }
        return questionsStr.split(",");
    }
    
    @Override
    public String getRolePermission(String role) {
        return config.getProperty("permissions." + role);
    }
    
    @Override
    public String[] getAvailableRoles() {
        List<String> roles = new ArrayList<>();
        for (String key : config.stringPropertyNames()) {
            if (key.startsWith("applications.") && key.endsWith(".questions")) {
                String role = key.substring("applications.".length(), key.length() - ".questions".length());
                roles.add(role);
            }
        }
        return roles.toArray(new String[0]);
    }
    
    @Override
    public boolean isValidRole(String role) {
        String[] availableRoles = getAvailableRoles();
        for (String availableRole : availableRoles) {
            if (availableRole.equals(role)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public CompletableFuture<Boolean> validateConfiguration() {
        return CompletableFuture.supplyAsync(() -> {
            if (config == null) return false;
            
            String[] roles = getAvailableRoles();
            for (String role : roles) {
                if (getRolePermission(role) == null) {
                    return false;
                }
            }
            return true;
        });
    }
    
    private void loadConfiguration() {
        config = new Properties();
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                config.load(fis);
            } catch (IOException e) {
                // Use default empty configuration
            }
        }
        
        // Set default values if not present
        setDefaultIfMissing("applications.mod.questions", "What is your experience?,Why do you want to be a mod?,How many hours can you commit?");
        setDefaultIfMissing("applications.builder.questions", "What is your building experience?,Can you use WorldEdit?,Show us your builds");
        setDefaultIfMissing("applications.lore.questions", "Why are you interested in lore?,What is your favorite DC character?,Rate your DC knowledge");
        setDefaultIfMissing("permissions.mod", "group.mod");
        setDefaultIfMissing("permissions.builder", "group.builder");
        setDefaultIfMissing("permissions.lore", "group.lore");
    }
    
    private void setDefaultIfMissing(String key, String defaultValue) {
        if (!config.containsKey(key)) {
            config.setProperty(key, defaultValue);
        }
    }
}