package org.jetby.treexBuyer.menus;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetby.libb.InstanceFactory;
import org.jetby.libb.gui.parser.Item;
import org.jetby.libb.gui.parser.ParseUtil;
import org.jetby.libb.gui.parser.ParsedGui;
import org.jetby.libb.gui.parser.ParserContext;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.configurations.GeneralConfiguration;
import org.jetby.treexBuyer.configurations.ItemsConfiguration;
import org.jetby.treexBuyer.manager.SellManager;
import org.jetby.treexBuyer.models.Property;
import org.jetby.treexBuyer.models.SellerItem;
import org.jetby.treexBuyer.models.UserData;
import org.jetby.treexBuyer.models.properties.EnchantmentProperty;
import org.jetby.treexBuyer.tools.NumberUtils;

import java.util.*;
import java.util.function.Consumer;

public class BuyerGui extends ParsedGui {
    @Getter
    private final List<Integer> sellSlots;
    private final BuyerManager manager;
    private final UserData user;

    public BuyerGui(@NotNull Player viewer, UserData user, @NotNull FileConfiguration config, BuyerManager manager) {
        super(viewer, config, manager.getPlugin(), ParserContext.of(GeneralConfiguration.SERIALIZER));
        this.user = user;
        this.manager = manager;
        this.sellSlots = ParseUtil.parseSlots(config.getStringList("sell-slots"));

        lockEmptySlots(false);

        onClick(event -> {

            int slot = event.getRawSlot();
            int invSize = getInventory().getSize();

            boolean isGuiSlot = sellSlots.contains(slot);
            boolean isShiftFromPlayer = event.isShiftClick() && slot >= invSize;

            if (!isGuiSlot && !isShiftFromPlayer) return;

            event.setCancelled(false);
            manager.getPlugin().getServer().getScheduler().runTask(manager.getPlugin(), this::refresh);
        });

        onDrag(event -> {
            Set<Integer> slots = event.getRawSlots();
            boolean affectsSellSlots = slots.stream().anyMatch(sellSlots::contains);
            if (!affectsSellSlots) return;

            event.setCancelled(false);
            manager.getPlugin().getServer().getScheduler().runTask(manager.getPlugin(), this::refresh);
        });


        Consumer<InventoryCloseEvent> onClose = onClose();
        onClose(event -> {
            if (onClose != null)
                onClose.accept(event);

            sellSlots.forEach(slot -> {
                ItemStack item = event.getInventory().getItem(slot);
                if (item == null) return;
                if (item.getItemMeta().getPersistentDataContainer().has(InstanceFactory.GUI_ITEM)) return;
                player.getInventory().addItem(item);
            });
        });
    }

    private void recalcSellPay() {
        Inventory inv = getInventory();
        double total = 0.0;
        for (int slot : sellSlots) {
            ItemStack item = inv.getItem(slot);
            if (item == null) continue;

            total += SellManager.countPrice(player, item);
        }
        setReplace("%sell_pay%", NumberUtils.format(total));
        setReplace("%sell_pay_commas%", NumberUtils.formatWithCommas(total));
    }

    private void recalcSellScore() {
        Inventory inv = getInventory();
        double total = 0.0;
        for (int slot : sellSlots) {
            ItemStack item = inv.getItem(slot);
            if (item == null) continue;

            total += SellManager.countScore(item);
        }
        setReplace("%sell_score%", NumberUtils.format(total));
        setReplace("%sell_score_commas%", NumberUtils.formatWithCommas(total));
    }

    @Override
    public void refresh() {
        recalcSellPay();
        recalcSellScore();
        setReplace("%score%", NumberUtils.format(user.getScore().getTotal()));
        setReplace("{score}", NumberUtils.format(user.getScore().getTotal()));

        setReplace("%score_commas%", NumberUtils.formatWithCommas(user.getScore().getTotal()));
        setReplace("{score_commas}", NumberUtils.formatWithCommas(user.getScore().getTotal()));

        setReplace("%coefficient%", NumberUtils.format(manager.getCoefficientManager().getTotalCoefficient(player, user.getScore())));
        setReplace("{coefficient}", NumberUtils.format(manager.getCoefficientManager().getTotalCoefficient(player, user.getScore())));
        super.refresh();
    }

    @Override
    public void buildItems(List<Item> items) {
        if (manager == null) {
            super.buildItems(items);
            return;
        }

        if (items == null) return;

        List<Item> expanded = new ArrayList<>();
        for (Item item : items) {
            String category = item.section() != null
                    ? item.section().getString("category")
                    : null;

            boolean isForSale = "for_sale".equalsIgnoreCase(item.type());
            String forSaleMaterial = isForSale && item.section() != null
                    ? item.section().getString("material")
                    : null;

            if (category == null && !isForSale) {
                expanded.add(item);
                continue;
            }

            if (category != null) {
                expanded.addAll(expandCategoryItem(item, category));
                continue;
            }

            expanded.addAll(expandForSaleItem(item, forSaleMaterial));
        }

        super.buildItems(expanded);
    }

    @Override
    public void clearInventory() {
        getWrappers().clear();
        for (int i = 0; i < getInventory().getSize(); i++) {
            if (!sellSlots.contains(i)) {
                getInventory().setItem(i, null);
            }
        }
    }


    private List<Item> expandCategoryItem(Item template, String category) {
        List<SellerItem> items = ItemsConfiguration.SELLER_ITEMS.values().stream()
                .filter(item -> item.category().equalsIgnoreCase(category))
                .toList();

        List<Integer> slots = template.slots();
        List<Item> result = new ArrayList<>();

        int slotIndex = 0;
        for (SellerItem sellerItem : items) {
            if (slotIndex >= slots.size()) break;

            if (sellerItem.properties().isEmpty()) {
                Item copy = cloneWithMaterial(template, sellerItem.price(), null, sellerItem);
                copy.slots(List.of(slots.get(slotIndex++)));
                result.add(copy);
                continue;
            }

            for (Property property : sellerItem.properties()) {
                if (slotIndex >= slots.size()) break;
                Item copy = cloneWithMaterial(template, sellerItem.price() + property.extraPrice(), property, sellerItem);
                copy.slots(List.of(slots.get(slotIndex++)));
                result.add(copy);
                if (!manager.getCfg().isGuiDuplicateByRule()) break;
            }
        }

        return result;
    }

    private List<Item> expandForSaleItem(Item template, String materialName) {
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return List.of(template);
        }

        SellerItem sellerItem = ItemsConfiguration.SELLER_ITEMS.values().stream()
                .filter(si -> si.material() == material)
                .findFirst()
                .orElse(null);

        if (sellerItem == null) return List.of(template);

        List<Item> result = new ArrayList<>();

        if (sellerItem.properties().isEmpty()) {
            Item item = cloneWithMaterial(template, sellerItem.price(), null, sellerItem);
            item.slots(template.slots());
            result.add(item);
            return result;
        }

        for (Property property : sellerItem.properties()) {
            Item item = cloneWithMaterial(template, sellerItem.price() + property.extraPrice(), property, sellerItem);
            item.slots(template.slots());
            result.add(item);
            if (!manager.getCfg().isGuiDuplicateByRule()) break;
        }

        return result;
    }


    @Override
    public void refreshHelpfulPlaceholders() {}

    private Item cloneWithMaterial(Item template, double price, Property property, SellerItem sellerItem) {
        Item copy = new Item(new ItemStack(sellerItem.material()));
        copy.section(template.section());
        copy.onClick(template.onClick());
        copy.priority(template.priority());
        copy.viewRequirements(template.viewRequirements());
        copy.flags(template.flags());

        copy.displayName(template.displayName().isEmpty() ? null : template.displayName());
        copy.lore(template.lore());

        copy.customModelData(sellerItem.model());

        if (property != null) {
            if (property instanceof EnchantmentProperty p) {
                copy.enchantments(Map.of(p.enchantment(), p.level()));
            }
        }

        if (copy.enchantments() == null || copy.enchantments().isEmpty()) {
            copy.enchanted(user.getAutoBuyItems().contains(sellerItem.material()));
        }
        setReplace(copy, "{material}", sellerItem.material().name());

        setReplace(copy, "{price}", NumberUtils.format(price));
        setReplace(copy, "%price%", NumberUtils.format(price));

        setReplace(copy, "{price_commas}", NumberUtils.formatWithCommas(price));
        setReplace(copy, "%price_commas%", NumberUtils.formatWithCommas(price));

        double priceWithCoefficient = manager.getCoefficientManager().getPriceWithCoefficient(player, price, sellerItem.material());
        setReplace(copy, "{price_with_coefficient}", NumberUtils.format(priceWithCoefficient));
        setReplace(copy, "%price_with_coefficient%", NumberUtils.format(priceWithCoefficient));

        setReplace(copy, "{price_with_coefficient_commas}", NumberUtils.formatWithCommas(priceWithCoefficient));
        setReplace(copy, "%price_with_coefficient_commas%", NumberUtils.formatWithCommas(priceWithCoefficient));

        setReplace(copy, "{auto_sell_toggle_state}",
                user.getAutoBuyItems().contains(sellerItem.material()) ? manager.getCfg().getEnable() : manager.getCfg().getDisable());
        setReplace(copy, "%auto_sell_toggle_state%",
                user.getAutoBuyItems().contains(sellerItem.material()) ? manager.getCfg().getEnable() : manager.getCfg().getDisable());

        return copy;
    }

    public List<Material> getVisibleMaterials() {
        return getInventory().getContents() == null ? List.of() : Arrays.stream(getInventory().getContents())
                .filter(item -> item != null && !item.getType().isAir())
                .filter(item -> !getSellSlots().contains(/* slot */ 0))
                .map(ItemStack::getType)
                .filter(mat -> BuyerManager.MANAGER.getItems().getItemByMaterial(mat) != null)
                .distinct()
                .toList();
    }

}