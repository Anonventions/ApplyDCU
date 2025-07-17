package io.github.anonventions.applydcu.services;

import io.github.anonventions.applydcu.api.Application;
import io.github.anonventions.applydcu.api.ApplicationService;
import io.github.anonventions.applydcu.api.ApplicationStatus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple application service that works without external dependencies
 */
public class SimpleApplicationService implements ApplicationService {
    
    private final File dataFolder;
    private final Map<UUID, Application> applicationCache;
    
    public SimpleApplicationService(File dataFolder) {
        this.dataFolder = new File(dataFolder, "applications");
        this.applicationCache = new ConcurrentHashMap<>();
        
        // Ensure applications directory exists
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }
        
        // Load existing applications into cache
        loadApplicationsFromDisk();
    }
    
    @Override
    public CompletableFuture<Boolean> submitApplication(UUID playerId, String role, List<String> answers) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String playerName = "Player_" + playerId.toString().substring(0, 8);
                Application application = new Application(playerId, playerName, role, answers);
                applicationCache.put(playerId, application);
                
                // Save to disk
                saveApplicationToDisk(application);
                
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Application> getApplication(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Application app = applicationCache.get(playerId);
            if (app == null) {
                app = loadApplicationFromDisk(playerId);
                if (app != null) {
                    applicationCache.put(playerId, app);
                }
            }
            return app;
        });
    }
    
    @Override
    public CompletableFuture<List<Application>> getPendingApplications() {
        return CompletableFuture.supplyAsync(() -> {
            List<Application> pending = new ArrayList<>();
            for (Application app : applicationCache.values()) {
                if (app.getStatus() == ApplicationStatus.PENDING) {
                    pending.add(app);
                }
            }
            
            // Also load any applications not in cache
            File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".properties"));
            if (files != null) {
                for (File file : files) {
                    try {
                        String fileName = file.getName();
                        UUID playerId = UUID.fromString(fileName.substring(0, fileName.length() - 11)); // Remove .properties
                        
                        if (!applicationCache.containsKey(playerId)) {
                            Application app = loadApplicationFromDisk(playerId);
                            if (app != null && app.getStatus() == ApplicationStatus.PENDING) {
                                applicationCache.put(playerId, app);
                                pending.add(app);
                            }
                        }
                    } catch (Exception e) {
                        // Skip invalid files
                    }
                }
            }
            
            return pending;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> acceptApplication(UUID playerId, UUID reviewerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Application app = applicationCache.get(playerId);
                if (app == null) {
                    app = loadApplicationFromDisk(playerId);
                    if (app == null) return false;
                }
                
                app.setStatus(ApplicationStatus.ACCEPTED);
                app.setReviewerId(reviewerId);
                app.setReviewedAt(LocalDateTime.now());
                
                applicationCache.put(playerId, app);
                saveApplicationToDisk(app);
                
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> denyApplication(UUID playerId, UUID reviewerId, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Application app = applicationCache.get(playerId);
                if (app == null) {
                    app = loadApplicationFromDisk(playerId);
                    if (app == null) return false;
                }
                
                app.setStatus(ApplicationStatus.DENIED);
                app.setReviewerId(reviewerId);
                app.setDenialReason(reason);
                app.setReviewedAt(LocalDateTime.now());
                
                applicationCache.put(playerId, app);
                saveApplicationToDisk(app);
                
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<ApplicationStatus> getApplicationStatus(UUID playerId, String role) {
        return CompletableFuture.supplyAsync(() -> {
            Application app = applicationCache.get(playerId);
            if (app == null) {
                app = loadApplicationFromDisk(playerId);
            }
            
            if (app != null && app.getRole().equals(role)) {
                return app.getStatus();
            }
            
            return null;
        });
    }
    
    private void saveApplicationToDisk(Application application) {
        try {
            Properties props = new Properties();
            props.setProperty("playerId", application.getPlayerId().toString());
            props.setProperty("playerName", application.getPlayerName());
            props.setProperty("role", application.getRole());
            props.setProperty("status", application.getStatus().name());
            props.setProperty("submittedAt", application.getSubmittedAt().toString());
            
            // Save answers
            for (int i = 0; i < application.getAnswers().size(); i++) {
                props.setProperty("answer." + i, application.getAnswers().get(i));
            }
            
            if (application.getReviewerId() != null) {
                props.setProperty("reviewerId", application.getReviewerId().toString());
            }
            if (application.getDenialReason() != null) {
                props.setProperty("denialReason", application.getDenialReason());
            }
            if (application.getReviewedAt() != null) {
                props.setProperty("reviewedAt", application.getReviewedAt().toString());
            }
            
            File file = new File(dataFolder, application.getPlayerId() + ".properties");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                props.store(fos, "Application for " + application.getPlayerName());
            }
        } catch (IOException e) {
            // Handle error
        }
    }
    
    private Application loadApplicationFromDisk(UUID playerId) {
        try {
            File file = new File(dataFolder, playerId + ".properties");
            if (!file.exists()) return null;
            
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            }
            
            String playerName = props.getProperty("playerName");
            String role = props.getProperty("role");
            
            // Load answers
            List<String> answers = new ArrayList<>();
            int i = 0;
            while (props.containsKey("answer." + i)) {
                answers.add(props.getProperty("answer." + i));
                i++;
            }
            
            Application app = new Application(playerId, playerName, role, answers);
            
            // Set status and other fields
            String statusStr = props.getProperty("status", "PENDING");
            app.setStatus(ApplicationStatus.valueOf(statusStr));
            
            String reviewerIdStr = props.getProperty("reviewerId");
            if (reviewerIdStr != null) {
                app.setReviewerId(UUID.fromString(reviewerIdStr));
            }
            
            String denialReason = props.getProperty("denialReason");
            if (denialReason != null) {
                app.setDenialReason(denialReason);
            }
            
            String reviewedAtStr = props.getProperty("reviewedAt");
            if (reviewedAtStr != null) {
                app.setReviewedAt(LocalDateTime.parse(reviewedAtStr));
            }
            
            return app;
        } catch (Exception e) {
            return null;
        }
    }
    
    private void loadApplicationsFromDisk() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".properties"));
        if (files != null) {
            for (File file : files) {
                try {
                    String fileName = file.getName();
                    UUID playerId = UUID.fromString(fileName.substring(0, fileName.length() - 11));
                    Application app = loadApplicationFromDisk(playerId);
                    if (app != null) {
                        applicationCache.put(playerId, app);
                    }
                } catch (Exception e) {
                    // Skip invalid files
                }
            }
        }
    }
}