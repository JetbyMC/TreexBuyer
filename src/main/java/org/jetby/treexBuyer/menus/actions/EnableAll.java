package org.jetby.treexBuyer.menus.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetby.libb.action.Action;
import org.jetby.libb.action.ActionContext;
import org.jetby.libb.action.ActionInput;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.configurations.Items;
import org.jetby.treexBuyer.menus.BuyerGui;
import org.jetby.treexBuyer.models.SellerItem;
import org.jetby.treexBuyer.models.UserData;

public class EnableAll implements Action {
    @Override
    public void execute(@NotNull ActionContext ctx, @NotNull ActionInput input) {
        Player player = ctx.getPlayer();
        if (player == null) return;
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null) return;

        BuyerGui gui = ctx.get(BuyerGui.class);
        if (gui != null) {
            gui.getVisibleMaterials().forEach(user::addAutoBuyMaterial);
        } else {
            user.getAutoBuyItems().addAll(Items.SELLER_ITEMS.values()
                    .stream()
                    .map(SellerItem::material)
                    .toList());
        }
    }
}