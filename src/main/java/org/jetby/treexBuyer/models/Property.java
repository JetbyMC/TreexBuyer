package org.jetby.treexBuyer.models;

import org.bukkit.inventory.ItemStack;

public interface Property {
    boolean match(ItemStack item);

    double extraPrice();

    double extraScore();
}
