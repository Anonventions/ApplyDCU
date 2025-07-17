package io.github.anonventions.capplications.utils;

import io.github.anonventions.capplications.CApplications;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PermissionUtils {
    private final CApplications plugin;

    public PermissionUtils(CApplications plugin) {
        this.plugin = plugin;
    }

    public boolean grantRolePermissions(UUID playerId, String role) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(playerId);

            if (user == null) {
                plugin.getLogger().warning("Could not find LuckPerms user for UUID: " + playerId);
                return false;
            }

            String permission = plugin.getConfig().getString("permissions." + role);
            if (permission == null) {
                plugin.getLogger().warning("No permission configured for role: " + role);
                return false;
            }

            // Check if it's a group permission
            if (permission.startsWith("group.")) {
                String groupName = permission.substring(6);
                user.data().add(Node.builder("group." + groupName).build());
            } else {
                // Regular permission
                user.data().add(Node.builder(permission).build());
            }

            luckPerms.getUserManager().saveUser(user);
            plugin.getLogger().info("Granted permissions for role '" + role + "' to player: " + playerId);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to grant permissions for role '" + role + "' to player " + playerId + ": " + e.getMessage());
            return false;
        }
    }

    public boolean revokeRolePermissions(UUID playerId, String role) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(playerId);

            if (user == null) {
                plugin.getLogger().warning("Could not find LuckPerms user for UUID: " + playerId);
                return false;
            }

            String permission = plugin.getConfig().getString("permissions." + role);
            if (permission == null) {
                plugin.getLogger().warning("No permission configured for role: " + role);
                return false;
            }

            // Check if it's a group permission
            if (permission.startsWith("group.")) {
                String groupName = permission.substring(6);
                user.data().remove(Node.builder("group." + groupName).build());
            } else {
                // Regular permission
                user.data().remove(Node.builder(permission).build());
            }

            luckPerms.getUserManager().saveUser(user);
            plugin.getLogger().info("Revoked permissions for role '" + role + "' from player: " + playerId);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to revoke permissions for role '" + role + "' from player " + playerId + ": " + e.getMessage());
            return false;
        }
    }

    public boolean hasRolePermission(Player player, String role) {
        String permission = plugin.getConfig().getString("permissions." + role);
        if (permission == null) {
            return false;
        }

        return player.hasPermission(permission);
    }

    public boolean canApplyForRole(Player player, String role) {
        // Check specific permission for the role
        if (player.hasPermission("capplications.apply." + role)) {
            return true;
        }

        // Check wildcard permission
        if (player.hasPermission("capplications.apply.*")) {
            return true;
        }

        // Check if they have admin permissions
        if (player.hasPermission("capplications.admin")) {
            return true;
        }

        return false;
    }

    public boolean isLuckPermsAvailable() {
        try {
            LuckPermsProvider.get();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public void addApplicationPermissions(UUID playerId, String role) {
        if (!isLuckPermsAvailable()) {
            plugin.getLogger().warning("LuckPerms not available - cannot add permissions");
            return;
        }

        grantRolePermissions(playerId, role);
    }

    public void removeApplicationPermissions(UUID playerId, String role) {
        if (!isLuckPermsAvailable()) {
            plugin.getLogger().warning("LuckPerms not available - cannot remove permissions");
            return;
        }

        revokeRolePermissions(playerId, role);
    }
}