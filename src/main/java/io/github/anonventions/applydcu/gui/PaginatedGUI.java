package io.github.anonventions.applydcu.gui;

import io.github.anonventions.applydcu.ApplyDCU;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PaginatedGUI {

    private static final int PAGE_SIZE = 45;

    public static void showGUI(Player player, List<ItemStack> items, int page) {
        int totalPages = (int) Math.ceil((double) items.size() / PAGE_SIZE);
        Inventory gui = Bukkit.createInventory(null, 54, "Applications - Page " + (page + 1));

        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, items.size());
        for (int i = startIndex; i < endIndex; i++) {
            gui.setItem(i - startIndex, items.get(i));
        }

        if (page > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            prevMeta.setDisplayName(ChatColor.GREEN + "Previous Page");
            prevButton.setItemMeta(prevMeta);
            gui.setItem(45, prevButton);
        }

        if (page < totalPages - 1) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            nextMeta.setDisplayName(ChatColor.GREEN + "Next Page");
            nextButton.setItemMeta(nextMeta);
            gui.setItem(53, nextButton);
        }

        player.openInventory(gui);
    }

    public static void refreshGUI(Player player, ApplyDCU plugin, String playerId) {
        List<ItemStack> items = plugin.getApplicationsConfig().getConfigurationSection("applications").getKeys(false).stream()
                .filter(key -> !key.equals(playerId)) // Exclude the processed application
                .map(key -> {
                    String role = plugin.getApplicationsConfig().getString("applications." + key + ".role");
                    List<String> questions = plugin.getApplicationsConfig().getStringList("applications." + key + ".questions");
                    List<String> answers = plugin.getApplicationsConfig().getStringList("applications." + key + ".answers");

                    ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD, 1);
                    SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(key)));
                    meta.setDisplayName(meta.getOwningPlayer().getName());

                    List<String> lore = questions.stream()
                            .map(question -> ChatColor.YELLOW + question + ": " + ChatColor.WHITE + (answers.size() > questions.indexOf(question) ? answers.get(questions.indexOf(question)) : ""))
                            .collect(Collectors.toList());
                    lore.add(0, ChatColor.GOLD + "Role: " + ChatColor.WHITE + role);

                    meta.setLore(lore);
                    playerHead.setItemMeta(meta);

                    return playerHead;
                })
                .collect(Collectors.toList());

        showGUI(player, items, 0); // Reset to the first page
    }
}
//Need to fucking fix.