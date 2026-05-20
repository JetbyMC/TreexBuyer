package org.jetby.treexBuyer.models.properties;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetby.treexBuyer.models.Property;

import java.util.HashMap;
import java.util.Map;

public record PotionProperty(int level,
                             PotionEffectType potionType,
                             double extraPrice,
                             double extraScore
) implements Property {

    private static final Map<PotionEffectType, Integer> EFFECT_CACHE = new HashMap<>();

    @Override
    public boolean match(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (item.getType() != Material.POTION) return false;
        if (meta instanceof PotionMeta p) {
            EFFECT_CACHE.clear();

            PotionType base = p.getBasePotionData().getType();
            if (base.getEffectType() != null) {
                EFFECT_CACHE.put(base.getEffectType(), p.getBasePotionData().isUpgraded() ? 2 : 1);
            }

            for (PotionEffect effect : p.getCustomEffects()) {
                EFFECT_CACHE.put(effect.getType(), effect.getAmplifier() + 1);
            }

            int effect_level = EFFECT_CACHE.getOrDefault(potionType, 0);
            return level == effect_level;
        }
        return false;
    }
}
