package io.github.anonventions.applydcu.api;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Core service interface for application management
 */
public interface ApplicationService {
    
    /**
     * Submit a new application
     */
    CompletableFuture<Boolean> submitApplication(UUID playerId, String role, List<String> answers);
    
    /**
     * Get application by player ID
     */
    CompletableFuture<Application> getApplication(UUID playerId);
    
    /**
     * Get all pending applications
     */
    CompletableFuture<List<Application>> getPendingApplications();
    
    /**
     * Accept an application
     */
    CompletableFuture<Boolean> acceptApplication(UUID playerId, UUID reviewerId);
    
    /**
     * Deny an application
     */
    CompletableFuture<Boolean> denyApplication(UUID playerId, UUID reviewerId, String reason);
    
    /**
     * Get application status for a player
     */
    CompletableFuture<ApplicationStatus> getApplicationStatus(UUID playerId, String role);
}