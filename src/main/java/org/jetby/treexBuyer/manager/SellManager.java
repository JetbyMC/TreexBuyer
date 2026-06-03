package org.jetby.treexBuyer.manager;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.configurations.Config;
import org.jetby.treexBuyer.configurations.Items;
import org.jetby.treexBuyer.models.Property;
import org.jetby.treexBuyer.models.SellerItem;
import org.jetby.treexBuyer.models.UserData;
import org.jetby.treexBuyer.tools.CustomModelDataUtil;


public class SellManager {

    private static final ItemStack AIR = new ItemStack(Material.AIR);

    public static void sell(Player player, ItemStack item, Inventory inventory) {
        UserData user = UserData.getOrCreate(player.getUniqueId(), BuyerManager.MANAGER.getItems().createScore());
        if (item == null || item.getType().isAir()) return;
        if (!isRegularItem(item)) return;
        SellerItem sellerItem = BuyerManager.MANAGER.getItems().getItemByMaterial(item.getType());
        if (sellerItem == null) return;

        if (sellerItem.model() != null) {
            if (!CustomModelDataUtil.matches(item.getItemMeta(), sellerItem.model())) return;
        }

        double price = countPrice(player, item);
        double score = countScore(item);

        var eq = player.getEquipment();
        if (eq.getItemInOffHand().isSimilar(item)) eq.setItemInOffHand(AIR);
        if (item.isSimilar(eq.getHelmet())) eq.setHelmet(AIR);
        if (item.isSimilar(eq.getChestplate())) eq.setChestplate(AIR);
        if (item.isSimilar(eq.getLeggings())) eq.setLeggings(AIR);
        if (item.isSimilar(eq.getBoots())) eq.setBoots(AIR);

        user.addScore(item.getType(), score);
        inventory.removeItem(item);

        BuyerManager.MANAGER.getEconomy().depositPlayer(player, price);
    }

    public static void sellAll(Player player) {
        for (ItemStack item : player.getInventory()) {
            sell(player, item, player.getInventory());
        }
    }

    public static void sellAll(Player player, Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            sell(player, item, inv);
        }
    }

    public static double countPrice(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return 0.0;
        if (!isRegularItem(item)) return 0.0;
        SellerItem sellerItem = BuyerManager.MANAGER.getItems().getItemByMaterial(item.getType());
        if (sellerItem == null) return 0.0;

        if (sellerItem.model() != null) {
            if (!CustomModelDataUtil.matches(item.getItemMeta(), sellerItem.model())) return 0.0;
        }


        double pricePerItem = BuyerManager.MANAGER.getCoefficientManager().getPriceWithCoefficient(player, item.getType());
        for (Property property : Items.PROPERTIES) {
            if (sellerItem.properties().contains(property)) continue;
            if (property.match(item)) {
                pricePerItem += property.extraPrice();
            }
        }
        for (Property property : sellerItem.properties()) {
            if (property.match(item)) {
                pricePerItem += property.extraPrice();
            }
        }

        return pricePerItem * item.getAmount();
    }

    public static double countScore(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0.0;
        if (!isRegularItem(item)) return 0.0;
        SellerItem sellerItem = BuyerManager.MANAGER.getItems().getItemByMaterial(item.getType());
        if (sellerItem == null) return 0.0;

        if (sellerItem.model() != null) {
            if (!CustomModelDataUtil.matches(item.getItemMeta(), sellerItem.model())) return 0.0;
        }

        double scorePerItem = sellerItem.addScore();
        for (Property property : Items.PROPERTIES) {
            if (sellerItem.properties().contains(property)) continue;
            if (property.match(item)) {
                scorePerItem += property.extraScore();
            }
        }
        for (Property property : sellerItem.properties()) {
            if (property.match(item)) {
                scorePerItem += property.extraScore();
            }
        }

        return scorePerItem * item.getAmount();
    }

    public static double countPrice(Player player) {
        Inventory inv = player.getInventory();
        double total = 0.0;
        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;
            total += SellManager.countPrice(player, item);
        }
        return total;
    }

    public static double countScore(Player player) {
        Inventory inv = player.getInventory();
        double total = 0.0;
        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;
            total += SellManager.countScore(item);
        }
        return total;
    }

    public static boolean isRegularItem(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Config config = BuyerManager.MANAGER.getCfg();
        if (config == null) return true;
        if (config.isDisallowedItemPersistent()) {
            if (!meta.getPersistentDataContainer().isEmpty()) {
                return false;
            }
        }

        if (config.isDisallowedItemCustomName()) {
            if (meta.hasDisplayName()) return false;
        }
        if (config.isDisallowedItemModelData()) {
            if (meta.hasCustomModelData()) return false;
        }
        if (config.isDisallowedItemItemFlags()) {
            if (!meta.getItemFlags().isEmpty()) return false;
        }
        if (config.isDisallowedItemLore()) {
            boolean isPotion = item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION;
            return isPotion || !meta.hasLore();
        }
        return true;

    }
}
