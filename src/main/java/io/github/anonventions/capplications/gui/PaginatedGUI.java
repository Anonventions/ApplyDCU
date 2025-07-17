package io.github.anonventions.capplications.gui;

import io.github.anonventions.capplications.CApplications;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PaginatedGUI {

    private static final int PAGE_SIZE = 45;

    public static void showGUI(Player player, List<ItemStack> items, int page, String title) {
        FileConfiguration config = CApplications.getInstance().getConfig();

        String guiTitle = ChatColor.translateAlternateColorCodes('&', title + " - Page " + (page + 1));
        int totalPages = (int) Math.ceil((double) items.size() / PAGE_SIZE);

        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;

        Inventory gui = Bukkit.createInventory(null, 54, guiTitle);

        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            gui.setItem(i - startIndex, items.get(i));
        }

        // Navigation buttons
        if (page > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            prevMeta.setDisplayName(ChatColor.GREEN + "Previous Page");
            prevMeta.setCustomModelData(config.getInt("custom_model_data.previous_page", 0));
            prevButton.setItemMeta(prevMeta);
            gui.setItem(45, prevButton);
        }

        if (page < totalPages - 1) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            nextMeta.setDisplayName(ChatColor.GREEN + "Next Page");
            nextMeta.setCustomModelData(config.getInt("custom_model_data.next_page", 0));
            nextButton.setItemMeta(nextMeta);
            gui.setItem(53, nextButton);
        }

        player.openInventory(gui);
    }

    public static void refreshGUI(Player player, CApplications plugin, String title) {
        File applicationsFolder = new File(plugin.getDataFolder(), "applications");
        if (!applicationsFolder.exists()) {
            player.sendMessage(ChatColor.YELLOW + "No applications found.");
            return;
        }

        File[] applicationFiles = applicationsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (applicationFiles == null || applicationFiles.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "No applications found.");
            return;
        }

        List<ItemStack> items = Arrays.stream(applicationFiles)
                .map(file -> createApplicationItem(file, plugin))
                .filter(item -> item != null)
                .collect(Collectors.toList());

        showGUI(player, items, 0, title);
    }

    private static ItemStack createApplicationItem(File file, CApplications plugin) {
        try {
            FileConfiguration applicationConfig = YamlConfiguration.loadConfiguration(file);
            String role = applicationConfig.getString("role", "Unknown");
            String status = applicationConfig.getString("status", "pending");
            List<String> questions = applicationConfig.getStringList("questions");
            List<String> answers = applicationConfig.getStringList("answers");

            UUID playerId = UUID.fromString(file.getName().replace(".yml", ""));

            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerId));

            String playerName = Bukkit.getOfflinePlayer(playerId).getName();
            if (playerName == null) playerName = "Unknown Player";

            meta.setDisplayName(ChatColor.GOLD + playerName);

            List<String> lore = questions.stream()
                    .map(question -> {
                        int index = questions.indexOf(question);
                        String answer = (answers.size() > index) ? answers.get(index) : "No answer";
                        return ChatColor.YELLOW + question + "\n" + ChatColor.WHITE + "Answer: " + answer;
                    })
                    .collect(Collectors.toList());

            lore.add(0, ChatColor.BLUE + "Role: " + ChatColor.WHITE + role);
            lore.add(ChatColor.RED + "Status: " + ChatColor.WHITE + status);

            if ("denied".equalsIgnoreCase(status)) {
                String deniedBy = applicationConfig.getString("deniedBy", "Unknown");
                String reason = applicationConfig.getString("denialReason", "No reason provided");
                lore.add(ChatColor.RED + "Denied By: " + ChatColor.WHITE + deniedBy);
                lore.add(ChatColor.RED + "Reason: " + ChatColor.WHITE + reason);
            } else if ("accepted".equalsIgnoreCase(status)) {
                String acceptedBy = applicationConfig.getString("acceptedBy", "Unknown");
                lore.add(ChatColor.GREEN + "Accepted By: " + ChatColor.WHITE + acceptedBy);
            }

            meta.setLore(lore);
            playerHead.setItemMeta(meta);

            return playerHead;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create item for application file: " + file.getName());
            return null;
        }
    }
}