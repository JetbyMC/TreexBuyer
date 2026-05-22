package org.jetby.treexBuyer.configurations;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.models.Property;
import org.jetby.treexBuyer.models.SellerItem;
import org.jetby.treexBuyer.models.properties.EnchantmentProperty;
import org.jetby.treexBuyer.models.properties.PotionProperty;
import org.jetby.treexBuyer.storage.score.Score;

import java.util.*;

import static org.jetby.treexBuyer.BuyerManager.LOGGER;

@Getter
public class Items {

    private final BuyerManager manager;

    private final Map<String, SellerItem> sellerItems = new LinkedHashMap<>();

    private final Map<Material, String> categories = new LinkedHashMap<>();
    private FileConfiguration config;
    public static final Set<Property> PROPERTIES = new HashSet<>();

    public Items(BuyerManager manager) {
        this.manager = manager;
    }

    public void load() {
        this.config = manager.getPlugin().getFileConfiguration("prices.yml");

        PROPERTIES.clear();
        sellerItems.clear();
        categories.clear();

        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection != null) {
            for (String category : categoriesSection.getKeys(false)) {
                for (String name : categoriesSection.getStringList(category)) {
                    try {
                        Material material = Material.valueOf(name);
                        categories.put(material, category);
                    } catch (IllegalArgumentException e) {
                        LOGGER.error(manager.getPlugin(), "Invalid material in category " + category + ": " + name);
                    }
                }
            }
        }

        for (String key : config.getKeys(false)) {
            if (key.equals("categories")) continue;
            if (key.equals("default-rules")) {
                PROPERTIES.addAll(getProperties(config.getConfigurationSection(key)));
                continue;
            }
            String materialName = config.getString(key + ".material");
            Material material = null;
            if (materialName != null) {
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    LOGGER.error(manager.getPlugin(), "Invalid material in prices.yml: " + key);
                }
            } else {
                try {
                    material = Material.valueOf(key.toUpperCase());
                } catch (IllegalArgumentException ignore) {
                }
            }

            if (sellerItems.containsKey(key) || (sellerItems.get(key) != null && material == sellerItems.get(key).material()))
                continue;

            double price = config.getDouble(key + ".price", 0.0);
            double score = config.getDouble(key + ".add-scores", 0);

            Set<Property> properties = getProperties(config.getConfigurationSection(key));


            sellerItems.put(key, new SellerItem(key,
                    material,
                    categories.getOrDefault(material, "none"),
                    price, score,
                    properties));


        }
    }


    private Set<Property> getProperties(ConfigurationSection config) {
        Set<Property> properties = new HashSet<>();
        ConfigurationSection enchantmentsSection = config.getConfigurationSection("enchantments");
        if (enchantmentsSection != null) {
            for (String enchantName : enchantmentsSection.getKeys(false)) {
                ConfigurationSection enchantmentSection = enchantmentsSection.getConfigurationSection(enchantName);
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.fromString(enchantName));
                for (String level : enchantmentSection.getKeys(false)) {
                    double extraPrice = enchantmentSection.getDouble(level + ".extra-price");
                    double extraScore = enchantmentSection.getDouble(level + ".extra-score");

                    properties.add(new EnchantmentProperty(Integer.parseInt(level), enchantment, extraPrice, extraScore));
                }
            }
        }
        ConfigurationSection potionsSection = config.getConfigurationSection("potions");
        if (potionsSection != null) {
            for (String potionName : potionsSection.getKeys(false)) {
                ConfigurationSection enchantmentSection = potionsSection.getConfigurationSection(potionName);
                PotionEffectType potionType = PotionEffectType.getByKey(NamespacedKey.fromString(potionName));
                for (String level : enchantmentSection.getKeys(false)) {
                    double extraPrice = enchantmentSection.getDouble(level + ".extra-price");
                    double extraScore = enchantmentSection.getDouble(level + ".extra-score");

                    properties.add(new PotionProperty(Integer.parseInt(level), potionType, extraPrice, extraScore));
                }
            }
        }
        return properties;
    }

    public String getCategory(Material material) {
        return categories.getOrDefault(material, "unknown");
    }

    public List<Material> getMaterials(String category) {
        return categories.entrySet().stream()
                .filter(e -> e.getValue().equalsIgnoreCase(category))
                .map(Map.Entry::getKey)
                .toList();
    }

    public SellerItem getItemByMaterial(Material material) {
        return getSellerItems().values()
                .stream()
                .filter(sellerItem -> sellerItem.material() == material)
                .findFirst()
                .orElse(null);
    }

    public double getScoreAmount(Material material) {
        SellerItem item = getSellerItems().values()
                .stream()
                .filter(sellerItem -> sellerItem.material() == material)
                .findFirst()
                .orElse(null);

        return item == null ? 0.0 : item.addScore();
    }

    public double getOriginalPrice(Material material) {
        SellerItem item = getSellerItems().values()
                .stream()
                .filter(sellerItem -> sellerItem.material() == material)
                .findFirst()
                .orElse(null);

        return item == null ? 0.0 : item.price();
    }

    public Score createScore() {
        return manager.getCfg().getType().createScore(categories);
    }

}