package io.github.anonventions.capplications.events;

import io.github.anonventions.capplications.CApplications;
import io.github.anonventions.capplications.gui.PaginatedGUI;
import io.github.anonventions.capplications.utils.MessageUtils;
import io.github.anonventions.capplications.utils.PermissionUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class InventoryClickListener implements Listener {

    private final CApplications plugin;
    private final MessageUtils messageUtils;
    private final PermissionUtils permissionUtils;

    public InventoryClickListener(CApplications plugin) {
        this.plugin = plugin;
        this.messageUtils = new MessageUtils(plugin);
        this.permissionUtils = new PermissionUtils(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        FileConfiguration config = plugin.getConfig();
        String applicationsTitle = ChatColor.translateAlternateColorCodes('&',
                config.getString("gui.titles.applications", "&6&lApplications"));
        String manageTitle = ChatColor.translateAlternateColorCodes('&',
                config.getString("gui.titles.manage", "&f♜ Manage Application"));
        String availableTitle = ChatColor.translateAlternateColorCodes('&',
                config.getString("gui.titles.available", "&f♛ Available Applications"));
        String statusTitle = ChatColor.translateAlternateColorCodes('&',
                config.getString("gui.titles.status", "&f♚ Application Status"));

        String title = event.getView().getTitle();

        if (title.contains(applicationsTitle.replace("&", "")) ||
                title.contains(availableTitle.replace("&", "")) ||
                title.contains(statusTitle.replace("&", ""))) {

            event.setCancelled(true);
            handleApplicationGUIClick(event);

        } else if (title.equals(manageTitle.replace("&", ""))) {
            event.setCancelled(true);
            handleManageGUIClick(event);
        }
    }

    private void handleApplicationGUIClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();

        if (clickedItem == null) return;

        if (clickedItem.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
            OfflinePlayer offlinePlayer = meta.getOwningPlayer();
            if (offlinePlayer != null) {
                openApplicationManagementGUI(player, offlinePlayer.getUniqueId().toString());
            }
        } else if (clickedItem.getType() == Material.ARROW) {
            handlePageNavigation(event);
        }
    }

    private void handleManageGUIClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack centerItem = event.getInventory().getItem(13);

        if (centerItem == null || centerItem.getItemMeta() == null) return;

        String playerId = ChatColor.stripColor(centerItem.getItemMeta().getDisplayName());
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getItemMeta() == null) return;

        String displayName = clickedItem.getItemMeta().getDisplayName();

        if (displayName.contains("Accept")) {
            acceptApplication(player, playerId);
            player.closeInventory();
        } else if (displayName.contains("Deny")) {
            denyApplication(player, playerId);
            player.closeInventory();
        }
    }

    private void handlePageNavigation(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        String displayName = clickedItem.getItemMeta().getDisplayName();

        List<ItemStack> items = Arrays.asList(event.getInventory().getContents());
        items.removeIf(Objects::isNull);

        if (displayName.contains("Next Page")) {
            int currentPage = getPageNumber(event.getView().getTitle());
            PaginatedGUI.showGUI(player, items, currentPage + 1, event.getView().getTitle());
        } else if (displayName.contains("Previous Page")) {
            int currentPage = getPageNumber(event.getView().getTitle());
            PaginatedGUI.showGUI(player, items, currentPage - 1, event.getView().getTitle());
        }
    }

    private void openApplicationManagementGUI(Player player, String playerId) {
        if (!player.hasPermission("capplications.manage")) {
            messageUtils.sendNoPermissionMessage(player);
            return;
        }

        FileConfiguration config = plugin.getConfig();
        String manageTitle = messageUtils.formatMessage("gui.titles.manage");

        Inventory gui = Bukkit.createInventory(null, 27, manageTitle);

        // Accept button
        ItemStack acceptButton = new ItemStack(Material.LIME_DYE, 1);
        ItemMeta acceptMeta = acceptButton.getItemMeta();
        acceptMeta.setDisplayName(ChatColor.GREEN + "Accept Application");
        acceptMeta.setCustomModelData(config.getInt("custom_model_data.accept_button", 0));
        acceptButton.setItemMeta(acceptMeta);

        // Deny button
        ItemStack denyButton = new ItemStack(Material.RED_DYE, 1);
        ItemMeta denyMeta = denyButton.getItemMeta();
        denyMeta.setDisplayName(ChatColor.RED + "Deny Application");
        denyMeta.setCustomModelData(config.getInt("custom_model_data.deny_button", 0));
        denyButton.setItemMeta(denyMeta);

        // Player head
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta headMeta = (SkullMeta) playerHead.getItemMeta();
        headMeta.setDisplayName(ChatColor.GOLD + playerId);
        headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(playerId)));
        playerHead.setItemMeta(headMeta);

        gui.setItem(11, acceptButton);
        gui.setItem(15, denyButton);
        gui.setItem(13, playerHead);

        player.openInventory(gui);
    }

    private void acceptApplication(Player player, String playerId) {
        UUID playerUUID = UUID.fromString(playerId);
        FileConfiguration applicationConfig = plugin.loadApplication(playerUUID);

        if (applicationConfig == null) {
            messageUtils.sendErrorMessage(player, "No application found for that player.");
            return;
        }

        String role = applicationConfig.getString("role");
        applicationConfig.set("status", "accepted");
        applicationConfig.set("acceptedBy", player.getName());

        plugin.saveApplication(playerUUID, applicationConfig);
        plugin.deleteApplication(playerUUID);
        plugin.savePlayerStatus(playerUUID, role, "accepted");
        plugin.logAction(player.getName(), "ACCEPT", playerUUID, role);

        // Grant permissions
        permissionUtils.grantRolePermissions(playerUUID, role);

        messageUtils.sendMessage(player, "admin.accepted_application",
                Bukkit.getOfflinePlayer(playerUUID).getName(), role);

        // Notify target player
        Player targetPlayer = Bukkit.getPlayer(playerUUID);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            messageUtils.sendMessage(targetPlayer, "application.accepted", role, player.getName());
        }

        // Refresh GUI
        String title = messageUtils.formatMessage("gui.titles.applications");
        PaginatedGUI.refreshGUI(player, plugin, title);
    }

    private void denyApplication(Player player, String playerId) {
        UUID playerUUID = UUID.fromString(playerId);
        FileConfiguration applicationConfig = plugin.loadApplication(playerUUID);

        if (applicationConfig == null) {
            messageUtils.sendErrorMessage(player, "No application found for that player.");
            return;
        }

        messageUtils.sendInfoMessage(player, "Please enter the reason for denying the application:");
        plugin.getPendingDenials().put(player.getUniqueId(), playerUUID);
    }

    private int getPageNumber(String title) {
        String[] parts = title.split(" - Page ");
        if (parts.length > 1) {
            try {
                return Integer.parseInt(parts[1]) - 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }
}