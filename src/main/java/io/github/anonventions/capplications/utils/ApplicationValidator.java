package io.github.anonventions.capplications.utils;

import io.github.anonventions.capplications.CApplications;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ApplicationValidator {
    private final CApplications plugin;

    public ApplicationValidator(CApplications plugin) {
        this.plugin = plugin;
    }

    public boolean isValidRole(String role) {
        Set<String> roles = plugin.getCustomConfig().getConfigurationSection("applications").getKeys(false);
        return roles.contains(role);
    }

    public boolean canApplyForRole(Player player, String role) {
        return player.hasPermission("capplications.apply." + role) ||
                player.hasPermission("capplications.apply.*");
    }

    public boolean hasActiveApplication(UUID playerId) {
        FileConfiguration config = plugin.loadApplication(playerId);
        return config != null && "in progress".equals(config.getString("status"));
    }

    public long getApplicationCooldown(UUID playerId, String role) {
        JSONArray statuses = plugin.loadPlayerStatus(playerId);
        long cooldownDuration = TimeUnit.HOURS.toMillis(
                plugin.getConfig().getInt("settings.cooldown_hours", 24)
        );

        for (Object obj : statuses) {
            JSONObject status = (JSONObject) obj;
            if (role.equals(status.get("role")) &&
                    ("denied".equals(status.get("status")) || "expired".equals(status.get("status")))) {
                long lastAttempt = (long) status.get("timestamp");
                long timePassed = System.currentTimeMillis() - lastAttempt;
                return Math.max(0, cooldownDuration - timePassed);
            }
        }
        return 0;
    }

    public boolean meetsRoleRequirements(Player player, String role) {
        List<String> requirements = plugin.getCustomConfig().getStringList("applications." + role + ".requirements");

        for (String requirement : requirements) {
            if (requirement.startsWith("permission:")) {
                String permission = requirement.substring(11);
                if (!player.hasPermission(permission)) {
                    return false;
                }
            } else if (requirement.startsWith("playtime:")) {
                // Implementation for playtime requirements
                // This would require a playtime tracking plugin or stat
                continue;
            }
        }
        return true;
    }

    public boolean hasRecentApplication(UUID playerId, String role, int hours) {
        JSONArray statuses = plugin.loadPlayerStatus(playerId);
        long timeThreshold = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);

        for (Object obj : statuses) {
            JSONObject status = (JSONObject) obj;
            if (role.equals(status.get("role"))) {
                long timestamp = (long) status.get("timestamp");
                if (timestamp > timeThreshold) {
                    return true;
                }
            }
        }
        return false;
    }
}