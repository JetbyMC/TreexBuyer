package org.jetby.treexBuyer.models;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.jetby.libb.action.record.ActionBlock;

import javax.annotation.Nullable;

public record Boost(
        String name,
        String permission,
        double coefficient,
        boolean boosters_except_legal_coefficient,
        ActionBlock on_start_player,
        ActionBlock on_start_global,
       @Nullable BossBarInfo bossBarInfo

) {
    public record BossBarInfo(
            boolean enable,
            String title,
            BarStyle style,
            BarColor color
    ) {}
}