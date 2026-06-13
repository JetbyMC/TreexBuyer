package org.jetby.treexBuyer.functions;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetby.libb.AdventureReflect;
import org.jetby.libb.action.ActionContext;
import org.jetby.libb.action.ActionExecute;
import org.jetby.libb.color.Serializer;
import org.jetby.treexBuyer.TreexBuyer;
import org.jetby.treexBuyer.models.Boost;
import org.jetby.treexBuyer.tools.TimerFormat;

import java.util.*;

public class BoosterBossBar {

    public static final List<BossBarData> CURRENT_BOOSTERS = new ArrayList<>();

    // global boost
    public static void run(Boost boost, int time) {
        BossBarData data = CURRENT_BOOSTERS.stream().filter(d -> d.boost == boost).findFirst().orElse(null);
        if (data != null) {
            data.time = time;
            return;
        }

        Boost.BossBarInfo info = boost.bossBarInfo();
        if (info == null) return;
        if (!info.enable()) return;


        BossBarData newData = new BossBarData(boost, time);
        newData.global(true);

        CURRENT_BOOSTERS.add(newData);
    }

    // per player boost
    public static void run(OfflinePlayer player, Boost boost, int time) {

        BossBarData data = CURRENT_BOOSTERS.stream().filter(d -> d.boost == boost).findFirst().orElse(null);
        if (data != null) {
            data.addPlayer(player);
            return;
        }

        Boost.BossBarInfo info = boost.bossBarInfo();
        if (info == null) return;
        if (!info.enable()) return;


        BossBarData newData = new BossBarData(boost, time);
        newData.addPlayer(player);

        CURRENT_BOOSTERS.add(newData);

    }

    public static class BossBarData implements Listener {

        private final BossBar bossBar;
        private int time;
        private final Boost boost;

        @Getter
        private boolean global = false;

        public void global(boolean global) {
            this.global = global;
            for (Player player : Bukkit.getOnlinePlayers()) {
                addPlayer(player);
            }
        }

        @Getter
        private final Set<UUID> players = new HashSet<>();

        public BossBarData(Boost boost, int time) {
            this.time = time;
            this.boost = boost;

            Boost.BossBarInfo info = boost.bossBarInfo();
            if (info == null)
                throw new RuntimeException("Boss Bar information is null. (Error location: BoosterBossBar/BossBarData/new");
            if (!info.enable()) throw new RuntimeException("Boss Bar is not enabled");

            this.bossBar = Bukkit.createBossBar(info.title(), info.color(), info.style());
            this.bossBar.setVisible(true);

            Bukkit.getPluginManager().registerEvents(this, TreexBuyer.INSTANCE);

            start();

        }

        public void start() {

            if (isGlobal()) {
                ActionExecute.run(ActionContext.of(null, TreexBuyer.INSTANCE), boost.on_start_global());
            }

            Bukkit.getScheduler().runTaskTimer(TreexBuyer.INSTANCE, task -> {

                if (time > 0) {
                    update();
                    time -= 1;
                } else {
                    CURRENT_BOOSTERS.remove(this);
                    bossBar.removeAll();
                    task.cancel();
                }

            }, 0L, 20L);
        }

        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            if (!isGlobal() || !players.contains(player.getUniqueId())) return;
            bossBar.addPlayer(player);
        }

        public void update() {
            String title = boost.bossBarInfo().title().replace("{time}", TimerFormat.get(time));

            title = title.replace("{coefficient}", String.valueOf(boost.coefficient()));

            bossBar.setTitle(AdventureReflect.toLegacySection(Serializer.UNIFIED.deserialize(title)));
        }

        public void addPlayer(OfflinePlayer player) {
            players.add(player.getUniqueId());

        }
        public void addPlayer(Player player) {
            players.add(player.getUniqueId());
            bossBar.addPlayer(player);

            ActionExecute.run(ActionContext.of(player, TreexBuyer.INSTANCE)
                            .replace("{coefficient}", String.valueOf(boost.coefficient()))
                            .replace("{time}", TimerFormat.get(time)),
                    boost.on_start_player());

        }

        public void removePlayer(Player player) {
            players.remove(player.getUniqueId());
            bossBar.removePlayer(player);
        }

    }

}
