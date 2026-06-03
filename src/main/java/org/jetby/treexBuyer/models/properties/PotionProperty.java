package org.jetby.treexBuyer.models.properties;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetby.treexBuyer.models.Property;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record PotionProperty(int level,
                             PotionEffectType potionType,
                             double extraPrice,
                             double extraScore
) implements Property {

    private static final Map<PotionEffectType, Integer> EFFECT_CACHE = new HashMap<>();

    @Override
    public boolean match(ItemStack item) {
        Material type = item.getType();
        boolean isPotion = type == Material.POTION || type==Material.SPLASH_POTION || type == Material.LINGERING_POTION;
        boolean isOminous = type.name().equalsIgnoreCase("OMINOUS_BOTTLE");

        if (!isPotion && !isOminous) return false;

        ItemMeta meta = item.getItemMeta();

        // OMINOUS_BOTTLE has its own metadata (OminousBottleMeta) not PotionMeta.
        if (isOminous) {
            return matchOminousBottle(meta);
        }

        if (meta instanceof PotionMeta p) {
            EFFECT_CACHE.clear();

            try {
                // 1.20.5+
                Method m = p.getClass().getMethod("getBasePotionType");
                Object base = m.invoke(p);
                if (base != null) {
                    Method getEffects = base.getClass().getMethod("getPotionEffects");
                    java.util.List<PotionEffect> effects = (List<PotionEffect>) getEffects.invoke(base);
                    if (effects != null) {
                        for (PotionEffect effect : effects) {
                            EFFECT_CACHE.put(effect.getType(), effect.getAmplifier() + 1);
                        }
                    }
                }
            } catch (NoSuchMethodException e) {
                // < 1.20.5
                PotionType base = p.getBasePotionData().getType();
                if (base.getEffectType() != null) {
                    EFFECT_CACHE.put(base.getEffectType(), p.getBasePotionData().isUpgraded() ? 2 : 1);
                }
            } catch (Exception ignore) {
            }

            for (PotionEffect effect : p.getCustomEffects()) {
                EFFECT_CACHE.put(effect.getType(), effect.getAmplifier() + 1);
            }

            int effect_level = EFFECT_CACHE.getOrDefault(potionType, 0);
            return level == effect_level;
        }

        return false;
    }

    // OminousBottleMeta has only existed since 1.21 that is why I am using reflection.
    private boolean matchOminousBottle(ItemMeta meta) {
        try {
            Class<?> ominousMetaClass = Class.forName("org.bukkit.inventory.meta.OminousBottleMeta");
            if (!ominousMetaClass.isInstance(meta)) return false;

            int amplifier = (int) ominousMetaClass.getMethod("getAmplifier").invoke(meta);
            return level == amplifier + 1;
        } catch (Exception e) {
            return false;
        }
    }
}
