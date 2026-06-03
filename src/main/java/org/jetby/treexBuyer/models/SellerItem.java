package org.jetby.treexBuyer.models;

import org.bukkit.Material;

import java.util.LinkedHashSet;

public record SellerItem(
        String id,
        Material material,
        Object model,
        String category,
        double price,
        double addScore,
        LinkedHashSet<Property> properties
) {
}
