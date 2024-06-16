package io.github.anonventions.applydcu.events;

import io.github.anonventions.applydcu.ApplyDCU;
import io.github.anonventions.applydcu.gui.PaginatedGUI;
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class InventoryClickListener implements Listener {

    private final ApplyDCU plugin;

    public InventoryClickListener(ApplyDCU plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("Applications - Page ")) {
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
                String title = event.getView().getTitle();
                int currentPage = Integer.parseInt(title.split(" ")[3]) - 1;
                List<ItemStack> items = Arrays.asList(event.getInventory().getContents());
                items.removeIf(Objects::isNull);

                if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Next Page")) {
                    PaginatedGUI.showGUI(player, items, currentPage + 1);
                } else if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Previous Page")) {
                    PaginatedGUI.showGUI(player, items, currentPage - 1);
                }
            }
        } else if (event.getView().getTitle().equals("Manage Application")) {
            event.setCancelled(true);

            Player player = (Player) event.getWhoClicked();
            String playerId = ChatColor.stripColor(event.getInventory().getItem(13).getItemMeta().getDisplayName());

            if (event.getCurrentItem().getType() == Material.GREEN_WOOL) {
                acceptApplication(player, playerId);
                player.closeInventory();
            } else if (event.getCurrentItem().getType() == Material.RED_WOOL) {
                denyApplication(player, playerId);
                player.closeInventory();
            }
        }
    }

    private void openApplicationManagementGUI(Player player, String playerId) {
        Inventory gui = Bukkit.createInventory(null, 27, "Manage Application");

        ItemStack acceptButton = new ItemStack(Material.GREEN_WOOL, 1);
        ItemMeta acceptMeta = acceptButton.getItemMeta();
        acceptMeta.setDisplayName(ChatColor.GREEN + "Accept Application");
        acceptButton.setItemMeta(acceptMeta);

        ItemStack denyButton = new ItemStack(Material.RED_WOOL, 1);
        ItemMeta denyMeta = denyButton.getItemMeta();
        denyMeta.setDisplayName(ChatColor.RED + "Deny Application");
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
        if (!plugin.getApplicationsConfig().contains("applications." + playerId)) {
            player.sendMessage(ChatColor.RED + "No application found for that player.");
            return;
        }

        String role = plugin.getApplicationsConfig().getString("applications." + playerId + ".role");
        plugin.getApplicationsConfig().set("applications." + playerId + ".status", "accepted");
        plugin.saveApplicationsConfig();
        player.sendMessage(ChatColor.GREEN + "Accepted application for player: " + Bukkit.getOfflinePlayer(UUID.fromString(playerId)).getName() + " for role: " + role);

        Player targetPlayer = Bukkit.getPlayer(UUID.fromString(playerId));
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(ChatColor.GREEN + "Your application for " + role + " has been accepted.");
        }

        // Refresh the GUI
        PaginatedGUI.refreshGUI(player, plugin, playerId);
    }

    private void denyApplication(Player player, String playerId) {
        if (!plugin.getApplicationsConfig().contains("applications." + playerId)) {
            player.sendMessage(ChatColor.RED + "No application found for that player.");
            return;
        }

        String role = plugin.getApplicationsConfig().getString("applications." + playerId + ".role");
        plugin.getApplicationsConfig().set("applications." + playerId + ".status", "denied");
        plugin.saveApplicationsConfig();
        player.sendMessage(ChatColor.RED + "Denied application for player: " + Bukkit.getOfflinePlayer(UUID.fromString(playerId)).getName() + " for role: " + role);

        Player targetPlayer = Bukkit.getPlayer(UUID.fromString(playerId));
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(ChatColor.RED + "Your application for " + role + " has been denied.");
        }

        // Refresh the GUI
        PaginatedGUI.refreshGUI(player, plugin, playerId);
    }
}