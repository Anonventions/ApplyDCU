package io.github.anonventions.applydcu.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Configuration schema validator for ApplyDCU v2.0
 */
public class ConfigurationValidator {
    
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        
        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
    }
    
    /**
     * Validates a configuration map against the expected schema
     */
    public static ValidationResult validateConfiguration(Map<String, Object> config) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Check required top-level sections
        if (!config.containsKey("applications")) {
            errors.add("Missing required section: applications");
        }
        if (!config.containsKey("permissions")) {
            errors.add("Missing required section: permissions");
        }
        
        // Validate applications section
        if (config.containsKey("applications")) {
            validateApplicationsSection(config.get("applications"), errors, warnings);
        }
        
        // Validate permissions section
        if (config.containsKey("permissions")) {
            validatePermissionsSection(config.get("permissions"), errors, warnings);
        }
        
        // Validate GUI section (optional)
        if (config.containsKey("gui")) {
            validateGuiSection(config.get("gui"), errors, warnings);
        }
        
        // Cross-validate applications and permissions
        crossValidateApplicationsAndPermissions(config, errors, warnings);
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    @SuppressWarnings("unchecked")
    private static void validateApplicationsSection(Object applications, List<String> errors, List<String> warnings) {
        if (!(applications instanceof Map)) {
            errors.add("applications section must be a map");
            return;
        }
        
        Map<String, Object> appsMap = (Map<String, Object>) applications;
        
        if (appsMap.isEmpty()) {
            warnings.add("No application types defined");
            return;
        }
        
        for (Map.Entry<String, Object> entry : appsMap.entrySet()) {
            String role = entry.getKey();
            Object roleConfig = entry.getValue();
            
            if (!(roleConfig instanceof Map)) {
                errors.add("Application role '" + role + "' must be a map");
                continue;
            }
            
            Map<String, Object> roleMap = (Map<String, Object>) roleConfig;
            
            // Check for required questions field
            if (!roleMap.containsKey("questions")) {
                errors.add("Application role '" + role + "' missing required 'questions' field");
            } else {
                Object questions = roleMap.get("questions");
                if (!(questions instanceof List)) {
                    errors.add("Application role '" + role + "' questions must be a list");
                } else {
                    List<?> questionsList = (List<?>) questions;
                    if (questionsList.isEmpty()) {
                        warnings.add("Application role '" + role + "' has no questions defined");
                    }
                    
                    // Validate each question is a string
                    for (int i = 0; i < questionsList.size(); i++) {
                        if (!(questionsList.get(i) instanceof String)) {
                            errors.add("Application role '" + role + "' question " + i + " must be a string");
                        }
                    }
                }
            }
            
            // Validate optional fields
            if (roleMap.containsKey("enabled") && !(roleMap.get("enabled") instanceof Boolean)) {
                errors.add("Application role '" + role + "' enabled field must be boolean");
            }
            
            if (roleMap.containsKey("description") && !(roleMap.get("description") instanceof String)) {
                errors.add("Application role '" + role + "' description field must be string");
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void validatePermissionsSection(Object permissions, List<String> errors, List<String> warnings) {
        if (!(permissions instanceof Map)) {
            errors.add("permissions section must be a map");
            return;
        }
        
        Map<String, Object> permsMap = (Map<String, Object>) permissions;
        
        if (permsMap.isEmpty()) {
            warnings.add("No permissions defined");
            return;
        }
        
        for (Map.Entry<String, Object> entry : permsMap.entrySet()) {
            String role = entry.getKey();
            Object permission = entry.getValue();
            
            if (!(permission instanceof String)) {
                errors.add("Permission for role '" + role + "' must be a string");
            } else {
                String permStr = (String) permission;
                if (permStr.trim().isEmpty()) {
                    errors.add("Permission for role '" + role + "' cannot be empty");
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void validateGuiSection(Object gui, List<String> errors, List<String> warnings) {
        if (!(gui instanceof Map)) {
            errors.add("gui section must be a map");
            return;
        }
        
        Map<String, Object> guiMap = (Map<String, Object>) gui;
        
        // Validate titles subsection
        if (guiMap.containsKey("titles")) {
            Object titles = guiMap.get("titles");
            if (!(titles instanceof Map)) {
                errors.add("gui.titles must be a map");
            } else {
                Map<String, Object> titlesMap = (Map<String, Object>) titles;
                for (Map.Entry<String, Object> entry : titlesMap.entrySet()) {
                    if (!(entry.getValue() instanceof String)) {
                        errors.add("gui.titles." + entry.getKey() + " must be a string");
                    }
                }
            }
        }
        
        // Validate materials subsection
        if (guiMap.containsKey("materials")) {
            Object materials = guiMap.get("materials");
            if (!(materials instanceof Map)) {
                errors.add("gui.materials must be a map");
            } else {
                Map<String, Object> materialsMap = (Map<String, Object>) materials;
                for (Map.Entry<String, Object> entry : materialsMap.entrySet()) {
                    if (!(entry.getValue() instanceof String)) {
                        errors.add("gui.materials." + entry.getKey() + " must be a string");
                    }
                }
            }
        }
        
        // Validate custommodeldata subsection
        if (guiMap.containsKey("custommodeldata")) {
            Object customModelData = guiMap.get("custommodeldata");
            if (!(customModelData instanceof Map)) {
                errors.add("gui.custommodeldata must be a map");
            } else {
                Map<String, Object> cmdMap = (Map<String, Object>) customModelData;
                for (Map.Entry<String, Object> entry : cmdMap.entrySet()) {
                    if (!(entry.getValue() instanceof Integer)) {
                        errors.add("gui.custommodeldata." + entry.getKey() + " must be an integer");
                    }
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void crossValidateApplicationsAndPermissions(Map<String, Object> config, 
                                                               List<String> errors, List<String> warnings) {
        Object applications = config.get("applications");
        Object permissions = config.get("permissions");
        
        if (!(applications instanceof Map) || !(permissions instanceof Map)) {
            return; // Already validated in individual sections
        }
        
        Map<String, Object> appsMap = (Map<String, Object>) applications;
        Map<String, Object> permsMap = (Map<String, Object>) permissions;
        
        // Check that every application role has a corresponding permission
        for (String role : appsMap.keySet()) {
            if (!permsMap.containsKey(role)) {
                errors.add("Application role '" + role + "' has no corresponding permission defined");
            }
        }
        
        // Warn about permissions without corresponding applications
        for (String role : permsMap.keySet()) {
            if (!appsMap.containsKey(role)) {
                warnings.add("Permission defined for role '" + role + "' but no application template exists");
            }
        }
    }
}