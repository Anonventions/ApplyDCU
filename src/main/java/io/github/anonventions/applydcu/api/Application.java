package io.github.anonventions.applydcu.api;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents an application in the system
 */
public class Application {
    private final UUID playerId;
    private final String playerName;
    private final String role;
    private final List<String> answers;
    private final LocalDateTime submittedAt;
    private ApplicationStatus status;
    private UUID reviewerId;
    private String denialReason;
    private LocalDateTime reviewedAt;
    
    public Application(UUID playerId, String playerName, String role, List<String> answers) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.role = role;
        this.answers = answers;
        this.submittedAt = LocalDateTime.now();
        this.status = ApplicationStatus.PENDING;
    }
    
    // Getters
    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public String getRole() { return role; }
    public List<String> getAnswers() { return answers; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public ApplicationStatus getStatus() { return status; }
    public UUID getReviewerId() { return reviewerId; }
    public String getDenialReason() { return denialReason; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    
    // Setters for status updates
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public void setReviewerId(UUID reviewerId) { this.reviewerId = reviewerId; }
    public void setDenialReason(String denialReason) { this.denialReason = denialReason; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}