package org.jetby.treexBuyer.models;

import org.bukkit.Material;

import java.util.Set;

public record SellerItem(
        String id,
        Material material,
        String category,
        double price,
        double addScore,
        Set<Property> properties
) {
}
