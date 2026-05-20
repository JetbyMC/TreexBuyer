package org.jetby.treexBuyer.menus.actions;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetby.libb.action.Action;
import org.jetby.libb.action.ActionContext;
import org.jetby.libb.action.ActionInput;
import org.jetby.libb.gui.item.ItemWrapper;
import org.jetby.treexBuyer.manager.SellManager;
import org.jetby.treexBuyer.menus.BuyerGui;

public class SellItem implements Action {
    @Override
    public void execute(@NotNull ActionContext ctx, @NotNull ActionInput input) {

        String s = input.rawText();

        Player player = ctx.getPlayer();
        if (player == null) return;

        BuyerGui gui = ctx.get(BuyerGui.class);
        ItemWrapper wrapper = ctx.get(ItemWrapper.class);
        if (gui == null || wrapper == null) return;

        int amount;
        try {
            amount = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            if (!s.equalsIgnoreCase("all")) return;
            amount = 0;
            for (ItemStack itemStack : player.getInventory().getContents()) {
                if (itemStack == null) continue;
                if (itemStack.getType() != wrapper.itemStack().getType()) continue;
                if (!SellManager.isRegularItem(itemStack)) continue;
                amount += itemStack.getAmount();
            }
        }

        if (amount <= 0) return;

        player.getInventory().removeItem(new ItemStack(wrapper.itemStack().getType(), amount));

        SellManager.sell(player, wrapper.itemStack(), gui.getInventory());
        gui.refresh();
    }
}