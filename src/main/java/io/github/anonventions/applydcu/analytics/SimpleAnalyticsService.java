package io.github.anonventions.applydcu.analytics;

import io.github.anonventions.applydcu.api.AnalyticsService;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple analytics service implementation
 */
public class SimpleAnalyticsService implements AnalyticsService {
    
    private final File dataFolder;
    private final Map<String, AtomicInteger> submissionCounts;
    private final Map<String, AtomicInteger> acceptanceCounts;
    private final Map<String, AtomicInteger> denialCounts;
    private final Map<String, List<String>> denialReasons;
    
    public SimpleAnalyticsService(File dataFolder) {
        this.dataFolder = new File(dataFolder, "analytics");
        this.submissionCounts = new ConcurrentHashMap<>();
        this.acceptanceCounts = new ConcurrentHashMap<>();
        this.denialCounts = new ConcurrentHashMap<>();
        this.denialReasons = new ConcurrentHashMap<>();
        
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }
        
        loadAnalyticsData();
    }
    
    @Override
    public void recordApplicationSubmission(String role) {
        submissionCounts.computeIfAbsent(role, k -> new AtomicInteger(0)).incrementAndGet();
        saveAnalyticsData();
        logEvent("APPLICATION_SUBMITTED", role, null);
    }
    
    @Override
    public void recordApplicationAcceptance(String role) {
        acceptanceCounts.computeIfAbsent(role, k -> new AtomicInteger(0)).incrementAndGet();
        saveAnalyticsData();
        logEvent("APPLICATION_ACCEPTED", role, null);
    }
    
    @Override
    public void recordApplicationDenial(String role, String reason) {
        denialCounts.computeIfAbsent(role, k -> new AtomicInteger(0)).incrementAndGet();
        denialReasons.computeIfAbsent(role, k -> new ArrayList<>()).add(reason);
        saveAnalyticsData();
        logEvent("APPLICATION_DENIED", role, reason);
    }
    
    @Override
    public CompletableFuture<ApplicationStats> getApplicationStats() {
        return CompletableFuture.supplyAsync(() -> {
            int totalSubmissions = submissionCounts.values().stream()
                    .mapToInt(AtomicInteger::get).sum();
            int totalAccepted = acceptanceCounts.values().stream()
                    .mapToInt(AtomicInteger::get).sum();
            int totalDenied = denialCounts.values().stream()
                    .mapToInt(AtomicInteger::get).sum();
            int totalPending = totalSubmissions - totalAccepted - totalDenied;
            
            return new ApplicationStats(totalSubmissions, totalAccepted, totalDenied, totalPending);
        });
    }
    
    @Override
    public CompletableFuture<Map<String, RoleStats>> getRoleStats() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, RoleStats> roleStats = new HashMap<>();
            
            Set<String> allRoles = new HashSet<>();
            allRoles.addAll(submissionCounts.keySet());
            allRoles.addAll(acceptanceCounts.keySet());
            allRoles.addAll(denialCounts.keySet());
            
            for (String role : allRoles) {
                int submissions = submissionCounts.getOrDefault(role, new AtomicInteger(0)).get();
                int accepted = acceptanceCounts.getOrDefault(role, new AtomicInteger(0)).get();
                int denied = denialCounts.getOrDefault(role, new AtomicInteger(0)).get();
                int pending = submissions - accepted - denied;
                
                roleStats.put(role, new RoleStats(role, submissions, accepted, denied, pending));
            }
            
            return roleStats;
        });
    }
    
    @Override
    public CompletableFuture<Double> getSuccessRate(String role) {
        return CompletableFuture.supplyAsync(() -> {
            int submissions = submissionCounts.getOrDefault(role, new AtomicInteger(0)).get();
            int accepted = acceptanceCounts.getOrDefault(role, new AtomicInteger(0)).get();
            
            return submissions > 0 ? (double) accepted / submissions : 0.0;
        });
    }
    
    @Override
    public CompletableFuture<String> exportAnalyticsData() {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder export = new StringBuilder();
            export.append("ApplyDCU Analytics Export\n");
            export.append("Generated: ").append(LocalDateTime.now()).append("\n\n");
            
            // Overall stats
            ApplicationStats stats = getApplicationStats().join();
            export.append("Overall Statistics:\n");
            export.append("Total Submissions: ").append(stats.getTotalSubmissions()).append("\n");
            export.append("Total Accepted: ").append(stats.getTotalAccepted()).append("\n");
            export.append("Total Denied: ").append(stats.getTotalDenied()).append("\n");
            export.append("Total Pending: ").append(stats.getTotalPending()).append("\n");
            export.append("Success Rate: ").append(String.format("%.2f%%", stats.getSuccessRate() * 100)).append("\n\n");
            
            // Role-specific stats
            Map<String, RoleStats> roleStats = getRoleStats().join();
            export.append("Role-Specific Statistics:\n");
            for (RoleStats roleStat : roleStats.values()) {
                export.append("Role: ").append(roleStat.getRole()).append("\n");
                export.append("  Submissions: ").append(roleStat.getSubmissions()).append("\n");
                export.append("  Accepted: ").append(roleStat.getAccepted()).append("\n");
                export.append("  Denied: ").append(roleStat.getDenied()).append("\n");
                export.append("  Pending: ").append(roleStat.getPending()).append("\n");
                export.append("  Success Rate: ").append(String.format("%.2f%%", roleStat.getSuccessRate() * 100)).append("\n");
                
                // Denial reasons
                List<String> reasons = denialReasons.get(roleStat.getRole());
                if (reasons != null && !reasons.isEmpty()) {
                    export.append("  Common Denial Reasons:\n");
                    Map<String, Integer> reasonCounts = new HashMap<>();
                    for (String reason : reasons) {
                        reasonCounts.put(reason, reasonCounts.getOrDefault(reason, 0) + 1);
                    }
                    reasonCounts.entrySet().stream()
                            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                            .limit(5)
                            .forEach(entry -> export.append("    ").append(entry.getKey())
                                    .append(": ").append(entry.getValue()).append("\n"));
                }
                export.append("\n");
            }
            
            return export.toString();
        });
    }
    
    private void saveAnalyticsData() {
        try {
            Properties props = new Properties();
            
            // Save submission counts
            for (Map.Entry<String, AtomicInteger> entry : submissionCounts.entrySet()) {
                props.setProperty("submissions." + entry.getKey(), String.valueOf(entry.getValue().get()));
            }
            
            // Save acceptance counts
            for (Map.Entry<String, AtomicInteger> entry : acceptanceCounts.entrySet()) {
                props.setProperty("acceptances." + entry.getKey(), String.valueOf(entry.getValue().get()));
            }
            
            // Save denial counts
            for (Map.Entry<String, AtomicInteger> entry : denialCounts.entrySet()) {
                props.setProperty("denials." + entry.getKey(), String.valueOf(entry.getValue().get()));
            }
            
            File analyticsFile = new File(dataFolder, "analytics.properties");
            try (FileOutputStream fos = new FileOutputStream(analyticsFile)) {
                props.store(fos, "ApplyDCU Analytics Data");
            }
        } catch (IOException e) {
            // Handle error silently
        }
    }
    
    private void loadAnalyticsData() {
        try {
            File analyticsFile = new File(dataFolder, "analytics.properties");
            if (!analyticsFile.exists()) return;
            
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(analyticsFile)) {
                props.load(fis);
            }
            
            // Load submission counts
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith("submissions.")) {
                    String role = key.substring("submissions.".length());
                    int count = Integer.parseInt(props.getProperty(key));
                    submissionCounts.put(role, new AtomicInteger(count));
                } else if (key.startsWith("acceptances.")) {
                    String role = key.substring("acceptances.".length());
                    int count = Integer.parseInt(props.getProperty(key));
                    acceptanceCounts.put(role, new AtomicInteger(count));
                } else if (key.startsWith("denials.")) {
                    String role = key.substring("denials.".length());
                    int count = Integer.parseInt(props.getProperty(key));
                    denialCounts.put(role, new AtomicInteger(count));
                }
            }
        } catch (Exception e) {
            // Handle error silently
        }
    }
    
    private void logEvent(String eventType, String role, String details) {
        try {
            File logFile = new File(dataFolder, "events.log");
            try (FileWriter writer = new FileWriter(logFile, true)) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                writer.write(String.format("%s [%s] Role: %s", timestamp, eventType, role));
                if (details != null) {
                    writer.write(String.format(" Details: %s", details));
                }
                writer.write("\n");
            }
        } catch (IOException e) {
            // Handle error silently
        }
    }
}