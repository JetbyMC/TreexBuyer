package org.jetby.treexBuyer.functions;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.configurations.BoostersConfiguration;
import org.jetby.treexBuyer.models.Boost;
import org.jetby.treexBuyer.models.UserData;
import org.jetby.treexBuyer.storage.score.Score;
import org.jetby.treexBuyer.storage.score.types.CategoryScore;
import org.jetby.treexBuyer.storage.score.types.PerItemScore;

public class CoefficientManager {

    final BuyerManager manager;

    public CoefficientManager(BuyerManager manager) {
        this.manager = manager;
    }

    public double getTotalCoefficient(Player player, Score score) {
        return getTotalCoefficient(player, score.getTotal());
    }

    public double getTotalCoefficient(Player player, double relevantScore) {
        double earned = relevantScore / manager.getCfg().getScores() * manager.getCfg().getCoefficient();
        double base = manager.getCfg().getDefaultCoefficient() + earned;
        double legal = Math.min(base, manager.getCfg().getMaxCoefficient());
        double boost = BoostersConfiguration.BOOSTERS.values().stream()
                .filter(b -> {
                    if (b.permission() == null) return false;

                    BoosterBossBar.BossBarData data = BoosterBossBar.CURRENT_BOOSTERS.stream()
                            .filter(d -> d.getPlayers().contains(player.getUniqueId()))
                            .findFirst()
                            .orElse(null);

                    if (data!=null) {
                        return true;
                    }

                    return player.hasPermission(b.permission());
                })
                .mapToDouble(Boost::coefficient)
                .sum();

        return manager.getCfg().isBoosters_except_legal_coefficient()
                ? legal + boost
                : Math.min(base + boost, manager.getCfg().getMaxCoefficient());
    }

    public double getTotalCoefficientByCategory(Player player, String category) {
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null || !(user.getScore() instanceof CategoryScore cs)) return 0.0;

        CategoryScore isolated = new CategoryScore();
        isolated.set(category, cs.get(category));

        return getTotalCoefficient(player, cs);
    }

    public double getPriceWithCoefficient(Player player, Material material) {
        return getPriceWithCoefficient(player, manager.getItems().getOriginalPrice(material), material);
    }

    public double getPriceWithCoefficient(Player player, double price, Material material) {
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null) return 0.0;

        Score score = user.getScore();
        double relevantScore;

        if (score instanceof PerItemScore s) {
            relevantScore = s.get(material);
        } else if (score instanceof CategoryScore s) {
            relevantScore = s.get(manager.getItems().getCategory(material));
        } else {
            relevantScore = score.getTotal();
        }

        return price * getTotalCoefficient(player, relevantScore);
    }

    public double getTotalScoreByCategory(Player player, String category) {
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null || !(user.getScore() instanceof CategoryScore cs)) return 0.0;
        return cs.get(category);
    }
}