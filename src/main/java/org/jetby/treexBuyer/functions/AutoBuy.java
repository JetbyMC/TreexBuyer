package org.jetby.treexBuyer.functions;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetby.libb.action.ActionContext;
import org.jetby.libb.action.ActionExecute;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.configurations.ItemsConfiguration;
import org.jetby.treexBuyer.manager.SellManager;
import org.jetby.treexBuyer.models.UserData;
import org.jetby.treexBuyer.tools.NumberUtils;

public class AutoBuy {

    final BuyerManager manager;
    private int task;

    public AutoBuy(BuyerManager manager) {
        this.manager = manager;
    }

    public void start() {
        manager.getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(manager.getPlugin(), t -> {
            this.task = t.getTaskId();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (ItemsConfiguration.SELLER_ITEMS.isEmpty()) break;
                UserData user = UserData.findByUuid(player.getUniqueId());
                if (user == null || !user.isAutoBuy() || player.getInventory().getContents().length == 0) continue;
                Bukkit.getScheduler().runTask(manager.getPlugin(), () -> checkItems(player));
            }
        }, 0L, manager.getCfg().getAutoBuyDelay());
    }

    public void stop() {
        Bukkit.getScheduler().cancelTask(task);
    }

    public void checkItems(Player player) {
        UserData user = UserData.getOrCreate(player.getUniqueId(), manager.getItems().createScore());

        if (player.getGameMode() == GameMode.CREATIVE && !player.hasPermission("treexbuyer.autobuy.creative.bypass"))
            return;
        if (manager.getCfg().getDisabledWorlds().contains(player.getWorld().getName()))
            return;

        double totalPrice = 0.0;
        double totalScores = 0.0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            if (!user.getAutoBuyItems().contains(item.getType())) continue;

            totalPrice += SellManager.countPrice(player, item);
            totalScores += SellManager.countScore(item);

            SellManager.sell(player, item, player.getInventory());
        }

        if (totalPrice <= 0) return;

        manager.getEconomy().depositPlayer(player, totalPrice);

        ActionExecute.run(ActionContext.of(player, manager.getPlugin())
                        .replace("%sell_pay%", NumberUtils.format(totalPrice))
                        .replace("%sell_pay_commas%", NumberUtils.formatWithCommas(totalPrice))
                        .replace("%sell_score%", NumberUtils.format(totalScores))
                        .replace("%sell_score_commas%", NumberUtils.formatWithCommas(totalScores)),
                manager.getCfg().getAutoBuyActions());
    }
}