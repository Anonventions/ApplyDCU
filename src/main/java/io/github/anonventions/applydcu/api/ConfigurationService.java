package io.github.anonventions.applydcu.api;

import java.util.concurrent.CompletableFuture;

/**
 * Configuration service interface
 */
public interface ConfigurationService {
    
    /**
     * Reload configuration
     */
    CompletableFuture<Void> reloadConfiguration();
    
    /**
     * Get application questions for a role
     */
    String[] getApplicationQuestions(String role);
    
    /**
     * Get permission for a role
     */
    String getRolePermission(String role);
    
    /**
     * Get available roles
     */
    String[] getAvailableRoles();
    
    /**
     * Check if a role is valid
     */
    boolean isValidRole(String role);
    
    /**
     * Validate configuration
     */
    CompletableFuture<Boolean> validateConfiguration();
}