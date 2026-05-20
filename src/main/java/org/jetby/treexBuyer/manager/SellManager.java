package org.jetby.treexBuyer.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.models.Property;
import org.jetby.treexBuyer.models.SellerItem;
import org.jetby.treexBuyer.models.UserData;


public class SellManager {

    private static final ItemStack AIR = new ItemStack(Material.AIR);

    public static void sell(Player player, ItemStack item, Inventory inventory) {
        UserData user = UserData.getOrCreate(player.getUniqueId(), BuyerManager.MANAGER.getItems().createScore());
        if (item == null || item.getType().isAir()) return;
        if (!isRegularItem(item)) return;
        SellerItem sellerItem = BuyerManager.MANAGER.getItems().getItemByMaterial(item.getType());
        if (sellerItem == null) return;

        double pricePerItem = BuyerManager.MANAGER.getCoefficientManager().getPriceWithCoefficient(player, item.getType());
        double scorePerItem = sellerItem.addScore();

        for (Property property : sellerItem.properties()) {
            if (property.match(item)) {
                pricePerItem+=property.extraPrice();
                scorePerItem+=property.extraScore();
            }
        }

        double price = pricePerItem * item.getAmount();
        double score = scorePerItem * item.getAmount();

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

        double pricePerItem = BuyerManager.MANAGER.getCoefficientManager().getPriceWithCoefficient(player, item.getType());
        for (Property property : sellerItem.properties()) {
            if (property.match(item)) {
                pricePerItem+=property.extraPrice();
            }
        }

        return pricePerItem * item.getAmount();
    }

    public static double countScore(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0.0;
        if (!isRegularItem(item)) return 0.0;
        SellerItem sellerItem = BuyerManager.MANAGER.getItems().getItemByMaterial(item.getType());
        if (sellerItem == null) return 0.0;
        double scorePerItem = sellerItem.addScore();
        for (Property property : sellerItem.properties()) {
            if (property.match(item)) {
                scorePerItem+=property.extraScore();
            }
        }
        return scorePerItem * item.getAmount();
    }

    public static boolean isRegularItem(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (!meta.getPersistentDataContainer().isEmpty()) return false;
        if (meta.hasDisplayName()) return false;
        if (meta.hasCustomModelData()) return false;
        if (!meta.getItemFlags().isEmpty()) return false;

        boolean isPotion = item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION;
        if (!isPotion && meta.hasLore()) return false;

        return !(meta instanceof LeatherArmorMeta lam)
                || lam.getColor().equals(Bukkit.getItemFactory().getDefaultLeatherColor());
    }
}
