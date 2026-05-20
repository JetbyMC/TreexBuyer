package org.jetby.treexBuyer.menus.actions;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetby.libb.action.Action;
import org.jetby.libb.action.ActionContext;
import org.jetby.libb.action.ActionInput;
import org.jetby.libb.gui.item.ItemWrapper;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.models.SellerItem;
import org.jetby.treexBuyer.models.UserData;

public class AutoBuyItemToggle implements Action {
    @Override
    public void execute(@NotNull ActionContext ctx, @NotNull ActionInput input) {
        Player player = ctx.getPlayer();
        if (player == null) return;
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null) return;

        ItemWrapper wrapper = ctx.get(ItemWrapper.class);
        if (wrapper == null || wrapper.itemStack() == null) return;

        Material material = wrapper.itemStack().getType();
        SellerItem sellerItem = BuyerManager.MANAGER.getItems().getItemByMaterial(material);
        if (sellerItem == null) return;

        if (user.getAutoBuyItems().contains(material)) {
            user.removeAutoBuyMaterial(material);
        } else {
            user.addAutoBuyMaterial(material);
        }
    }
}