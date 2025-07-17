package io.github.anonventions.applydcu.api;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Analytics service for tracking application metrics
 */
public interface AnalyticsService {
    
    /**
     * Record an application submission
     */
    void recordApplicationSubmission(String role);
    
    /**
     * Record an application acceptance
     */
    void recordApplicationAcceptance(String role);
    
    /**
     * Record an application denial
     */
    void recordApplicationDenial(String role, String reason);
    
    /**
     * Get application statistics
     */
    CompletableFuture<ApplicationStats> getApplicationStats();
    
    /**
     * Get role-specific statistics
     */
    CompletableFuture<Map<String, RoleStats>> getRoleStats();
    
    /**
     * Get success rate for a specific role
     */
    CompletableFuture<Double> getSuccessRate(String role);
    
    /**
     * Export analytics data
     */
    CompletableFuture<String> exportAnalyticsData();
    
    /**
     * Application statistics data class
     */
    class ApplicationStats {
        private final int totalSubmissions;
        private final int totalAccepted;
        private final int totalDenied;
        private final int totalPending;
        
        public ApplicationStats(int totalSubmissions, int totalAccepted, int totalDenied, int totalPending) {
            this.totalSubmissions = totalSubmissions;
            this.totalAccepted = totalAccepted;
            this.totalDenied = totalDenied;
            this.totalPending = totalPending;
        }
        
        public int getTotalSubmissions() { return totalSubmissions; }
        public int getTotalAccepted() { return totalAccepted; }
        public int getTotalDenied() { return totalDenied; }
        public int getTotalPending() { return totalPending; }
        public double getSuccessRate() { 
            return totalSubmissions > 0 ? (double) totalAccepted / totalSubmissions : 0.0; 
        }
    }
    
    /**
     * Role-specific statistics data class
     */
    class RoleStats {
        private final String role;
        private final int submissions;
        private final int accepted;
        private final int denied;
        private final int pending;
        
        public RoleStats(String role, int submissions, int accepted, int denied, int pending) {
            this.role = role;
            this.submissions = submissions;
            this.accepted = accepted;
            this.denied = denied;
            this.pending = pending;
        }
        
        public String getRole() { return role; }
        public int getSubmissions() { return submissions; }
        public int getAccepted() { return accepted; }
        public int getDenied() { return denied; }
        public int getPending() { return pending; }
        public double getSuccessRate() { 
            return submissions > 0 ? (double) accepted / submissions : 0.0; 
        }
    }
}