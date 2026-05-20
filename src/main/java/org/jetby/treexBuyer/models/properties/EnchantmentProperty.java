package org.jetby.treexBuyer.models.properties;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetby.treexBuyer.models.Property;

public record EnchantmentProperty(int level,
                                  Enchantment enchantment,
                                  double extraPrice,
                                  double extraScore
) implements Property {

    @Override
    public boolean match(ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        if (!meta.hasEnchants()) return false;

        return meta.getEnchantLevel(enchantment) == level;

    }
}
