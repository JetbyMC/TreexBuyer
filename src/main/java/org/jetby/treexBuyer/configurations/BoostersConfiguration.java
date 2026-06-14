package org.jetby.treexBuyer.configurations;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetby.libb.action.record.ActionBlock;
import org.jetby.libb.gui.parser.ParseUtil;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.models.Boost;

import java.util.HashMap;
import java.util.Map;

public class BoostersConfiguration {

    public static final Map<String, Boost> BOOSTERS = new HashMap<>();

    private final FileConfiguration configuration;

    public BoostersConfiguration(BuyerManager manager) {
        this.configuration = manager.getPlugin().getFileConfiguration("boosters.yml");
    }

    public void load() {
        BOOSTERS.clear();

        for (String key : configuration.getKeys(false)) {
            ConfigurationSection section = configuration.getConfigurationSection(key);
            if (section == null) continue;
            String permission = section.getString("permission");
            double coefficient = section.getDouble("external-coefficient", 0.0);
            boolean boosters_except_legal_coefficient = section.getBoolean("boosters_except_legal_coefficient");
            ActionBlock on_start_player = ParseUtil.getActionBlock(section.getList("on_start_player"));
            ActionBlock on_start_global = ParseUtil.getActionBlock(section.getList("on_start_global"));

            ConfigurationSection bossbar = section.getConfigurationSection("bossbar");
            Boost.BossBarInfo bossBarInfo = null;
            if (bossbar != null) {
                boolean enable = bossbar.getBoolean("enable", false);
                String title = bossbar.getString("title");
                BarStyle style = BarStyle.valueOf(bossbar.getString("style", "SOLID").toUpperCase());
                BarColor color = BarColor.valueOf(bossbar.getString("color", "RED").toUpperCase());
                bossBarInfo = new Boost.BossBarInfo(enable, title, style, color);
            }

            BOOSTERS.put(key, new Boost(
                    key.toLowerCase(),
                    permission,
                    coefficient,
                    boosters_except_legal_coefficient,
                    on_start_player,
                    on_start_global,
                    bossBarInfo
            ));
        }
    }

}
