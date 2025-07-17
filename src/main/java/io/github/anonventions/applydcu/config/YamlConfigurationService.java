package io.github.anonventions.applydcu.config;

import io.github.anonventions.applydcu.api.ConfigurationService;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced YAML configuration service using our simple parser
 */
public class YamlConfigurationService implements ConfigurationService {
    
    private final File configFile;
    private Map<String, Object> config;
    
    public YamlConfigurationService(File dataFolder) {
        this.configFile = new File(dataFolder, "config.yml");
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
        try {
            Map<String, Object> applications = getConfigSection("applications");
            if (applications != null && applications.containsKey(role)) {
                Map<String, Object> roleConfig = (Map<String, Object>) applications.get(role);
                List<String> questions = (List<String>) roleConfig.get("questions");
                return questions != null ? questions.toArray(new String[0]) : new String[0];
            }
        } catch (Exception e) {
            // Return empty array on error
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
            // Return null on error
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
            // Return empty array on error
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
                        return false;
                    }
                    
                    Map<String, Object> roleConfig = (Map<String, Object>) applications.get(role);
                    if (roleConfig == null || !roleConfig.containsKey("questions")) {
                        return false;
                    }
                }
                
                return true;
            } catch (Exception e) {
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
                config = SimpleYamlParser.parseYaml(configFile);
            } else {
                // Create default configuration
                config = createDefaultConfiguration();
            }
        } catch (Exception e) {
            // Fallback to default configuration
            config = createDefaultConfiguration();
        }
    }
    
    private Map<String, Object> createDefaultConfiguration() {
        Map<String, Object> defaultConfig = Map.of(
            "applications", Map.of(
                "mod", Map.of("questions", List.of(
                    "&eIn-game nickname? &7[Bot-Proof]",
                    "&eHow old are you?",
                    "&eWhat is your timezone?",
                    "&eWhat would you rate your plugin knowledge as?",
                    "&eWhy do you want to be a mod?",
                    "&eHow many hours can you commit weekly?"
                )),
                "builder", Map.of("questions", List.of(
                    "&eIn-game nickname? &7[Bot-Proof]",
                    "&eWhat is your building experience?",
                    "&eAre you familiar with WorldEdit and VoxelSniper?",
                    "&eHow many hours can you commit weekly?",
                    "&eHave you been a builder previously in any other servers?",
                    "&eCan you provide screenshots of your builds? &7[Send IMGUR links]"
                )),
                "lore", Map.of("questions", List.of(
                    "&eIn-game nickname? &7[Bot-Proof]",
                    "&eWhy are you interested in lore?",
                    "&eWhat is your favorite comic-book in DC Comics?",
                    "&eWhat would you rate your knowledge on DC Comics Characters as?",
                    "&eAny specific Characters you're very familiar with?",
                    "&eHow well can you write for events?",
                    "&eHow active will you be?",
                    "&eWhat is your timezone?"
                ))
            ),
            "permissions", Map.of(
                "mod", "group.mod",
                "builder", "group.builder",
                "lore", "group.lore"
            ),
            "gui", Map.of(
                "titles", Map.of(
                    "applications", "&6&lApplications",
                    "manage", "&fƪƪƪƪƪƪƪƪ♜",
                    "status", "&fƪƪƪƪƪƪƪƪ♚",
                    "available", "&fƪƪƪƪƪƪƪƪ"
                )
            )
        );
        return defaultConfig;
    }
}