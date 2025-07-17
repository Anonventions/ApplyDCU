package io.github.anonventions.applydcu.services;

import io.github.anonventions.applydcu.api.Application;
import io.github.anonventions.applydcu.api.ApplicationService;
import io.github.anonventions.applydcu.api.ApplicationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced application service with async operations and caching
 */
public class EnhancedApplicationService implements ApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedApplicationService.class);
    
    private final ObjectMapper objectMapper;
    private final File dataFolder;
    private final Map<UUID, Application> applicationCache;
    
    public EnhancedApplicationService(File dataFolder) {
        this.objectMapper = new ObjectMapper();
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
                // Get player name (placeholder for now)
                String playerName = "Player_" + playerId.toString().substring(0, 8);
                
                Application application = new Application(playerId, playerName, role, answers);
                applicationCache.put(playerId, application);
                
                // Save to disk
                saveApplicationToDisk(application);
                
                logger.info("Application submitted for player {} role {}", playerName, role);
                return true;
            } catch (Exception e) {
                logger.error("Failed to submit application for player " + playerId, e);
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
            File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        String fileName = file.getName();
                        UUID playerId = UUID.fromString(fileName.substring(0, fileName.length() - 5));
                        
                        if (!applicationCache.containsKey(playerId)) {
                            Application app = loadApplicationFromDisk(playerId);
                            if (app != null && app.getStatus() == ApplicationStatus.PENDING) {
                                applicationCache.put(playerId, app);
                                pending.add(app);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to load application file: " + file.getName(), e);
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
                
                logger.info("Application accepted for player {} by reviewer {}", 
                    app.getPlayerName(), reviewerId);
                return true;
            } catch (Exception e) {
                logger.error("Failed to accept application for player " + playerId, e);
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
                
                logger.info("Application denied for player {} by reviewer {} reason: {}", 
                    app.getPlayerName(), reviewerId, reason);
                return true;
            } catch (Exception e) {
                logger.error("Failed to deny application for player " + playerId, e);
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
            File file = new File(dataFolder, application.getPlayerId() + ".json");
            objectMapper.writeValue(file, application);
        } catch (IOException e) {
            logger.error("Failed to save application to disk", e);
        }
    }
    
    private Application loadApplicationFromDisk(UUID playerId) {
        try {
            File file = new File(dataFolder, playerId + ".json");
            if (file.exists()) {
                return objectMapper.readValue(file, Application.class);
            }
        } catch (Exception e) {
            logger.error("Failed to load application from disk", e);
        }
        return null;
    }
    
    private void loadApplicationsFromDisk() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try {
                    String fileName = file.getName();
                    UUID playerId = UUID.fromString(fileName.substring(0, fileName.length() - 5));
                    Application app = objectMapper.readValue(file, Application.class);
                    applicationCache.put(playerId, app);
                } catch (Exception e) {
                    logger.warn("Failed to load application: " + file.getName(), e);
                }
            }
        }
        logger.info("Loaded {} applications from disk", applicationCache.size());
    }
}