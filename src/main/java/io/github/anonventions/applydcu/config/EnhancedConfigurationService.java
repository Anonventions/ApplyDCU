package io.github.anonventions.applydcu.config;

import io.github.anonventions.applydcu.api.ConfigurationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced configuration service with validation and hot-reload capabilities
 */
public class EnhancedConfigurationService implements ConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedConfigurationService.class);
    
    private final ObjectMapper yamlMapper;
    private final File configFile;
    private Map<String, Object> config;
    
    public EnhancedConfigurationService(File dataFolder) {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.configFile = new File(dataFolder, "config.yml");
        loadConfiguration();
    }
    
    @Override
    public CompletableFuture<Void> reloadConfiguration() {
        return CompletableFuture.runAsync(() -> {
            try {
                loadConfiguration();
                logger.info("Configuration reloaded successfully");
            } catch (Exception e) {
                logger.error("Failed to reload configuration", e);
                throw new RuntimeException("Configuration reload failed", e);
            }
        });
    }
    
    @Override
    public String[] getApplicationQuestions(String role) {
        try {
            Map<String, Object> applications = getConfigSection("applications");
            if (applications != null && applications.containsKey(role)) {
                Map<String, Object> roleConfig = (Map<String, Object>) applications.get(role);
                List<String> questions = (List<String>) roleConfig.get("questions");
                return questions != null ? questions.toArray(new String[0]) : new String[0];
            }
        } catch (Exception e) {
            logger.warn("Failed to get questions for role: " + role, e);
        }
        return new String[0];
    }
    
    @Override
    public String getRolePermission(String role) {
        try {
            Map<String, Object> permissions = getConfigSection("permissions");
            if (permissions != null) {
                return (String) permissions.get(role);
            }
        } catch (Exception e) {
            logger.warn("Failed to get permission for role: " + role, e);
        }
        return null;
    }
    
    @Override
    public String[] getAvailableRoles() {
        try {
            Map<String, Object> applications = getConfigSection("applications");
            if (applications != null) {
                return applications.keySet().toArray(new String[0]);
            }
        } catch (Exception e) {
            logger.warn("Failed to get available roles", e);
        }
        return new String[0];
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
            try {
                // Basic validation
                if (config == null) return false;
                
                Map<String, Object> applications = getConfigSection("applications");
                Map<String, Object> permissions = getConfigSection("permissions");
                
                if (applications == null || permissions == null) return false;
                
                // Validate that each role has questions and permissions
                for (String role : applications.keySet()) {
                    if (!permissions.containsKey(role)) {
                        logger.warn("Role '{}' has no permission defined", role);
                        return false;
                    }
                    
                    Map<String, Object> roleConfig = (Map<String, Object>) applications.get(role);
                    if (roleConfig == null || !roleConfig.containsKey("questions")) {
                        logger.warn("Role '{}' has no questions defined", role);
                        return false;
                    }
                }
                
                logger.info("Configuration validation passed");
                return true;
            } catch (Exception e) {
                logger.error("Configuration validation failed", e);
                return false;
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> getConfigSection(String section) {
        if (config != null && config.containsKey(section)) {
            return (Map<String, Object>) config.get(section);
        }
        return null;
    }
    
    private void loadConfiguration() {
        try {
            if (configFile.exists()) {
                config = yamlMapper.readValue(configFile, Map.class);
            } else {
                logger.warn("Configuration file not found: {}", configFile.getPath());
                config = Map.of(); // Empty config
            }
        } catch (Exception e) {
            logger.error("Failed to load configuration", e);
            config = Map.of(); // Fallback to empty config
        }
    }
}