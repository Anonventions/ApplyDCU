package io.github.anonventions.applydcu.events;

import io.github.anonventions.applydcu.ApplyDCU;
import io.github.anonventions.applydcu.gui.PaginatedGUI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class InventoryClickListener implements Listener {

    private final ApplyDCU plugin;

    public InventoryClickListener(ApplyDCU plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        FileConfiguration config = plugin.getConfig();
        @SuppressWarnings("deprecation") String applicationsTitle = ChatColor.translateAlternateColorCodes('&', config.getString("gui.titles.applications"));
        String manageTitle = ChatColor.translateAlternateColorCodes('&', config.getString("gui.titles.manage"));
        String availableTitle = ChatColor.translateAlternateColorCodes('&', config.getString("gui.titles.available"));
        String statusTitle = ChatColor.translateAlternateColorCodes('&', config.getString("gui.titles.status"));

        if (event.getView().getTitle().equals(applicationsTitle) ||
                event.getView().getTitle().equals(availableTitle) ||
                event.getView().getTitle().equals(statusTitle)) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() == Material.PLAYER_HEAD) {
                Player player = (Player) event.getWhoClicked();
                SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
                OfflinePlayer offlinePlayer = meta.getOwningPlayer();
                if (offlinePlayer != null) {
                    openApplicationManagementGUI(player, offlinePlayer.getUniqueId().toString());
                }
            } else if (clickedItem != null && clickedItem.getType() == Material.ARROW) {
                Player player = (Player) event.getWhoClicked();
                List<ItemStack> items = Arrays.asList(event.getInventory().getContents());
                items.removeIf(Objects::isNull);

                if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Next Page")) {
                    PaginatedGUI.showGUI(player, items, getPageNumber(event.getView().getTitle()) + 1, event.getView().getTitle());
                } else if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Previous Page")) {
                    PaginatedGUI.showGUI(player, items, getPageNumber(event.getView().getTitle()) - 1, event.getView().getTitle());
                }
            }
        } else if (event.getView().getTitle().equals(manageTitle)) {
            event.setCancelled(true);

            Player player = (Player) event.getWhoClicked();
            String playerId = ChatColor.stripColor(event.getInventory().getItem(13).getItemMeta().getDisplayName());

            if (event.getCurrentItem().getType() == Material.PAPER) {
                ItemMeta meta = event.getCurrentItem().getItemMeta();
                if (meta != null && meta.getDisplayName().contains("Accept")) {
                    acceptApplication(player, playerId);
                    player.closeInventory();
                } else if (meta != null && meta.getDisplayName().contains("Deny")) {
                    denyApplication(player, playerId);
                    player.closeInventory();
                }
            }
        }
    }

    private void openApplicationManagementGUI(Player player, String playerId) {
        FileConfiguration config = plugin.getConfig();
        String manageTitle = ChatColor.translateAlternateColorCodes('&', config.getString("gui.titles.manage"));

        Inventory gui = Bukkit.createInventory(null, 27, manageTitle);

        ItemStack acceptButton = new ItemStack(Material.PAPER, 1);
        ItemMeta acceptMeta = acceptButton.getItemMeta();
        acceptMeta.setDisplayName(ChatColor.GREEN + "Accept Application");
        acceptMeta.setCustomModelData(config.getInt("custommodeldata.accept"));
        acceptButton.setItemMeta(acceptMeta);

        ItemStack denyButton = new ItemStack(Material.PAPER, 1);
        ItemMeta denyMeta = denyButton.getItemMeta();
        denyMeta.setDisplayName(ChatColor.RED + "Deny Application");
        denyMeta.setCustomModelData(config.getInt("custommodeldata.deny"));
        denyButton.setItemMeta(denyMeta);

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
            player.sendMessage(ChatColor.RED + "No application found for that player.");
            return;
        }

        String role = applicationConfig.getString("role");
        applicationConfig.set("status", "accepted");
        plugin.saveApplication(playerUUID, applicationConfig);
        plugin.deleteApplication(playerUUID);
        plugin.savePlayerStatus(playerUUID, role, "accepted");
        player.sendMessage(ChatColor.GREEN + "Accepted application for player: " + Bukkit.getOfflinePlayer(playerUUID).getName() + " for role: " + role);

        // Grant permissions via LuckPerms
        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().getUser(playerUUID);
        if (user != null) {
            String permission = plugin.getConfig().getString("permissions." + role);
            if (permission != null) {
                user.data().add(Node.builder(permission).build());
                luckPerms.getUserManager().saveUser(user);
            }
        }

        Player targetPlayer = Bukkit.getPlayer(playerUUID);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(ChatColor.GREEN + "Your application for " + role + " has been accepted.");
        }

        // Refresh the GUI
        PaginatedGUI.refreshGUI(player, plugin, playerId, ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.titles.applications")));
    }

    private void denyApplication(Player player, String playerId) {
        UUID playerUUID = UUID.fromString(playerId);
        FileConfiguration applicationConfig = plugin.loadApplication(playerUUID);
        if (applicationConfig == null) {
            player.sendMessage(ChatColor.RED + "No application found for that player.");
            return;
        }

        String role = applicationConfig.getString("role");
        applicationConfig.set("status", "denied");
        plugin.saveApplication(playerUUID, applicationConfig);
        plugin.deleteApplication(playerUUID);
        plugin.savePlayerStatus(playerUUID, role, "denied");
        player.sendMessage(ChatColor.RED + "Denied application for player: " + Bukkit.getOfflinePlayer(playerUUID).getName() + " for role: " + role);

        Player targetPlayer = Bukkit.getPlayer(playerUUID);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(ChatColor.RED + "Your application for " + role + " has been denied.");
        }

        // Refresh the GUI
        PaginatedGUI.refreshGUI(player, plugin, playerId, ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.titles.applications")));
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
