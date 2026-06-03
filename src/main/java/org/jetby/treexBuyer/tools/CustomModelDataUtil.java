package org.jetby.treexBuyer.tools;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.List;
import java.util.Map;

public class CustomModelDataUtil {

    public static void apply(ItemMeta meta, Object raw) {
        if (raw == null) return;

        if (raw instanceof Integer i) {
            meta.setCustomModelData(i);
            return;
        }

        try {
            CustomModelDataComponent component = meta.getCustomModelDataComponent();

            switch (raw) {
                case String s -> component.setStrings(List.of(s));
                case CustomModelDataComponent c -> {
                    meta.setCustomModelDataComponent(c);
                    return;
                }
                case Map<?, ?> map -> {
                    if (map.containsKey("strings"))
                        component.setStrings((List<String>) map.get("strings"));
                    if (map.containsKey("floats"))
                        component.setFloats(((List<Number>) map.get("floats")).stream().map(Number::floatValue).toList());
                    if (map.containsKey("flags"))
                        component.setFlags((List<Boolean>) map.get("flags"));
                }
                case ConfigurationSection section -> {
                    if (section.contains("strings"))
                        component.setStrings(section.getStringList("strings"));
                    if (section.contains("floats"))
                        component.setFloats(section.getDoubleList("floats").stream().map(Double::floatValue).toList());
                    if (section.contains("flags"))
                        component.setFlags(section.getBooleanList("flags"));
                }
                default -> {}
            }

            meta.setCustomModelDataComponent(component);
        } catch (Exception ignored) {
        }
    }
    public static boolean matches(ItemMeta meta, Object model) {
        if (model == null) return true;
        if (meta == null) return false;

        if (model instanceof Integer i) {
            return meta.hasCustomModelData() && meta.getCustomModelData() == i;
        }

        try {
            CustomModelDataComponent component = meta.getCustomModelDataComponent();

            if (model instanceof String s) {
                return component.getStrings().contains(s);
            }
            if (model instanceof ConfigurationSection section) {
                if (section.contains("strings") && !component.getStrings().containsAll(section.getStringList("strings")))
                    return false;
                if (section.contains("flags") && !component.getFlags().containsAll(section.getBooleanList("flags")))
                    return false;
                return !section.contains("floats") || component.getFloats().containsAll(
                        section.getDoubleList("floats").stream().map(Double::floatValue).toList());
            }
        } catch (Exception ignored) {
        }

        return false;
    }
    public static Object parse(ConfigurationSection section, String key) {
        Object raw = section.get(key);
        return switch (raw) {
            case Integer i -> i;
            case String s -> s;
            case ConfigurationSection cmd -> cmd;
            case null, default -> null;
        };

    }
}