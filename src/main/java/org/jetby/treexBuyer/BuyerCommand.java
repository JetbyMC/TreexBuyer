package org.jetby.treexBuyer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetby.libb.command.AdvancedCommand;
import org.jetby.libb.command.annotations.Permission;
import org.jetby.libb.command.annotations.SubCommand;
import org.jetby.libb.command.annotations.TabComplete;
import org.jetby.libb.command.annotations.messages.InsufficientArgs;
import org.jetby.libb.platform.PlatformSender;
import org.jetby.treexBuyer.configurations.BoostersConfiguration;
import org.jetby.treexBuyer.configurations.GeneralConfiguration;
import org.jetby.treexBuyer.configurations.GuiLoader;
import org.jetby.treexBuyer.configurations.ItemsConfiguration;
import org.jetby.treexBuyer.functions.BoosterBossBar;
import org.jetby.treexBuyer.menus.BuyerGui;
import org.jetby.treexBuyer.models.SellerItem;
import org.jetby.treexBuyer.models.UserData;
import org.jetby.treexBuyer.storage.score.Score;
import org.jetby.treexBuyer.storage.score.ScoreType;
import org.jetby.treexBuyer.storage.score.types.CategoryScore;
import org.jetby.treexBuyer.storage.score.types.GlobalScore;
import org.jetby.treexBuyer.storage.score.types.PerItemScore;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.jetby.treexBuyer.BuyerManager.LOGGER;

public class BuyerCommand extends AdvancedCommand {

    private final BuyerManager manager;
    private final GeneralConfiguration config;

    public BuyerCommand(BuyerManager manager) {
        super(manager.getPlugin().getCommand("treexbuyer"), manager.getPlugin(), false);
        this.manager = manager;
        this.config = manager.getCfg();
    }

    @SubCommand({"open"})
    @Permission("treexbuyer.admin")
    @InsufficientArgs("<#EF473A>Usage: /treexbuyer open <menu> [player]")
    public void open(CommandSender sender, String menuName) {
        if (!GuiLoader.ALL_GUIS.containsKey(menuName)) {
            PlatformSender.sendMessage(sender, GeneralConfiguration.SERIALIZER.deserialize("<#EF473A>Menu not found."));
            return;
        }
        Player target = sender instanceof Player p ? p : null;
        if (target == null) {
            PlatformSender.sendMessage(sender, GeneralConfiguration.SERIALIZER.deserialize("<#EF473A>Specify a player for console."));
            return;
        }
        UserData user = UserData.getOrCreate(target.getUniqueId(), manager.getItems().createScore());
        new BuyerGui(target, user, GuiLoader.ALL_GUIS.get(menuName), manager).open(target);
    }


    @SubCommand("booster")
    @Permission("treexbuyer.admin")
    @InsufficientArgs("<#EF473A>Usage: /treexbuyer booster <player/global> <booster> <duration seconds>")
    public void booster(CommandSender sender, String[] args) {
        if (args.length != 3) return;
        String playerName = args[0];
        String booster = args[1].toLowerCase();
        int time = Integer.parseInt(args[2]);
        if (playerName.equalsIgnoreCase("global")) {
            if (BoostersConfiguration.BOOSTERS.containsKey(booster)) {
                BoosterBossBar.run(BoostersConfiguration.BOOSTERS.get(booster), time);
                PlatformSender.sendMessage(sender, GeneralConfiguration.SERIALIZER.deserialize("&aSuccessfully started Global boost for the next " + time + " seconds"));
            } else {
                PlatformSender.sendMessage(sender, GeneralConfiguration.SERIALIZER.deserialize("<#EF473A>Booster not found"));
            }
        } else {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            if (BoostersConfiguration.BOOSTERS.containsKey(booster)) {
                BoosterBossBar.run(player, BoostersConfiguration.BOOSTERS.get(booster), time);
                PlatformSender.sendMessage(sender, GeneralConfiguration.SERIALIZER.deserialize("&aPlayer " + playerName + " just got boost for the next " + time + " seconds"));
            } else {
                PlatformSender.sendMessage(sender, GeneralConfiguration.SERIALIZER.deserialize("<#EF473A>Booster not found"));
            }
        }

    }

    @TabComplete("booster")
    public List<String> boosterTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        } else if (args.length == 2) {
            return BoostersConfiguration.BOOSTERS.keySet().stream().toList();
        } else if (args.length == 3) {
            return List.of("100", "300", "500", "1000", "3600", "86400");
        }
        return List.of();
    }

    @SubCommand({"open"})
    @Permission("treexbuyer.admin")
    @InsufficientArgs("<#EF473A>Usage: /treexbuyer open <menu> [player]")
    public void openFor(CommandSender sender, String menuName, Player target) {
        if (!GuiLoader.ALL_GUIS.containsKey(menuName)) {
            PlatformSender.sendMessage(sender, GeneralConfiguration.SERIALIZER.deserialize("<#EF473A>Menu not found."));
            return;
        }
        UserData user = UserData.getOrCreate(target.getUniqueId(), manager.getItems().createScore());
        new BuyerGui(target, user, GuiLoader.ALL_GUIS.get(menuName), manager).open(target);
    }

    @TabComplete({"open"})
    public List<String> tabOpen(CommandSender sender, String[] args) {
        if (args.length == 1) return new ArrayList<>(GuiLoader.ALL_GUIS.keySet());
        if (args.length == 2) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return List.of();
    }


    @SubCommand({"reload"})
    @Permission("treexbuyer.admin")
    public void reload(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(manager.getPlugin(), () -> {
            long start = System.currentTimeMillis();
            try {
                manager.getCfg().load();
                manager.getItems().load();
                manager.getStorage().shutdown();
                manager.getStorage().init();
                manager.getGuiLoader().loadGuis();
                if (manager.isProtocol())
                    manager.getInventoryPrice().load();
                manager.getActionBarUtil().start();

            } catch (Exception ex) {
                LOGGER.error("Error with config reloading: " + ex);
                PlatformSender.sendMessage(sender, GeneralConfiguration.SERIALIZER.deserialize("<#EF473A>Error: " + ex.getMessage()));
                return;
            }
            PlatformSender.sendMessage(sender, GeneralConfiguration.SERIALIZER.deserialize("<#82FB16>Reloaded in " + (System.currentTimeMillis() - start) + "ms."));
        });
    }

    @SubCommand({"score", "give"})
    @Permission("treexbuyer.admin")
    @InsufficientArgs("<#EF473A>Usage: /treexbuyer score give <player> <key> <amount>")
    public void scoreGive(CommandSender sender, String playerName, String[] args) {
        handleScore(sender, "give", playerName, args[0], args);
    }

    @SubCommand({"score", "take"})
    @Permission("treexbuyer.admin")
    @InsufficientArgs("<#EF473A>Usage: /treexbuyer score take <player> <key> <amount>")
    public void scoreTake(CommandSender sender, String playerName, String[] args) {
        handleScore(sender, "take", playerName, args[0], args);
    }

    @SubCommand({"score", "set"})
    @Permission("treexbuyer.admin")
    @InsufficientArgs("<#EF473A>Usage: /treexbuyer score set <player> <key> <amount>")
    public void scoreSet(CommandSender sender, String playerName, String[] args) {
        handleScore(sender, "set", playerName, args[0], args);
    }

    @TabComplete({"score", "give"})
    public List<String> tabScoreGive(CommandSender sender, String[] args) {
        return tabScoreArgs(args);
    }

    @TabComplete({"score", "take"})
    public List<String> tabScoreTake(CommandSender sender, String[] args) {
        return tabScoreArgs(args);
    }

    @TabComplete({"score", "set"})
    public List<String> tabScoreSet(CommandSender sender, String[] args) {
        return tabScoreArgs(args);
    }

    private List<String> tabScoreArgs(String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        if (args.length == 2) {
            ScoreType type = config.getType();
            if (type == ScoreType.GLOBAL) return List.of();
            if (type == ScoreType.ITEM)
                return Arrays.stream(Material.values()).map(m -> m.name().toLowerCase()).toList();
            if (type == ScoreType.CATEGORY)
                return ItemsConfiguration.SELLER_ITEMS.values().stream().map(SellerItem::category).toList();
        }
        return List.of();
    }

    private void handleScore(CommandSender sender, String action, String playerName, String key, String[] args) {
        ScoreType scoreType = config.getType();

        Player onlinePlayer = Bukkit.getPlayer(playerName);
        UUID uuid = onlinePlayer != null
                ? onlinePlayer.getUniqueId()
                : UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));

        double amount = 0;

        if (scoreType == ScoreType.GLOBAL) {
            // Command: /tb score give <player> <amount>
            // key == args[0] == the amount string
            try {
                amount = Double.parseDouble(key);
            } catch (NumberFormatException e) {
                PlatformSender.sendMessage(sender, GeneralConfiguration.SERIALIZER.deserialize("<#EF473A>Amount must be a non-negative number."));
                return;
            }
        } else {
            boolean keyIsAmount = false;
            try {
                amount = Double.parseDouble(key);
                if (amount < 0) throw new NumberFormatException();
                keyIsAmount = true;
            } catch (NumberFormatException ignored) {
            }

            if (keyIsAmount) {
                // /tb score take <player> <amount> — no key
                key = null;
            } else {
                // /tb score take <player> <key> <amount>
                key = key.toLowerCase();
                if (args.length < 2) {
                    PlatformSender.sendMessage(sender, GeneralConfiguration.SERIALIZER.deserialize("<#EF473A>Usage: /treexbuyer score " + action + " <player> <key> <amount>"));
                    return;
                }
                try {
                    amount = Double.parseDouble(args[1]);
                    if (amount < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    PlatformSender.sendMessage(sender, GeneralConfiguration.SERIALIZER.deserialize("<#EF473A>Amount must be a non-negative number."));
                    return;
                }
                if (scoreType == ScoreType.ITEM) {
                    try {
                        Material.valueOf(key.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        PlatformSender.sendMessage(sender, GeneralConfiguration.SERIALIZER.deserialize("<#EF473A>Invalid material: " + key));
                        return;
                    }
                } else if (scoreType == ScoreType.CATEGORY && !ItemsConfiguration.CATEGORIES.containsValue(key)) {
                    PlatformSender.sendMessage(sender, GeneralConfiguration.SERIALIZER.deserialize("<#EF473A>Invalid category key: " + key));
                    return;
                }
            }
        }

        UserData user = UserData.getOrCreate(uuid, manager.getItems().createScore());
        Score score = user.getScore();
        String finalKey = key;
        double finalAmount = amount;

        switch (action) {
            case "give" -> {
                if (score instanceof CategoryScore s) {
                    if (finalKey == null) {
                        String biggest = s.getRawScores().entrySet().stream()
                                .max(Map.Entry.comparingByValue())
                                .map(Map.Entry::getKey)
                                .orElse(ItemsConfiguration.CATEGORIES.values().stream().findFirst().get());
                        s.add(biggest, finalAmount);
                    } else {
                        s.add(finalKey, finalAmount);
                    }
                } else if (score instanceof GlobalScore s) {
                    s.add(finalAmount);
                } else if (score instanceof PerItemScore s) {
                    if (finalKey == null) {
                        Material biggest = s.getRawScores().entrySet().stream()
                                .max(Map.Entry.comparingByValue())
                                .map(Map.Entry::getKey)
                                .orElse(ItemsConfiguration.CATEGORIES.keySet().stream().findFirst().get());
                        s.add(biggest, finalAmount);
                    } else {
                        s.add(Material.valueOf(finalKey.toUpperCase()), finalAmount);
                    }
                }
            }
            case "set" -> {
                if (score instanceof CategoryScore s) {
                    if (finalKey == null) {
                        Map<String, Double> raw = s.getRawScores();
                        double total = raw.values().stream().mapToDouble(d -> d).sum();
                        if (total == 0) {
                            double each = finalAmount / raw.size();
                            raw.keySet().forEach(k -> s.set(k, each));
                        } else {
                            raw.forEach((k, v) -> s.set(k, finalAmount * (v / total)));
                        }
                    } else {
                        s.set(finalKey, finalAmount);
                    }
                } else if (score instanceof GlobalScore s) {
                    s.set(finalAmount);
                } else if (score instanceof PerItemScore s) {
                    if (finalKey == null) {
                        Map<Material, Double> raw = s.getRawScores();
                        double total = raw.values().stream().mapToDouble(d -> d).sum();
                        if (total == 0) {
                            double each = finalAmount / raw.size();
                            raw.keySet().forEach(k -> s.set(k, each));
                        } else {
                            raw.forEach((k, v) -> s.set(k, finalAmount * (v / total)));
                        }
                    } else {
                        s.set(Material.valueOf(finalKey.toUpperCase()), finalAmount);
                    }
                }
            }
            case "take" -> {
                if (score instanceof CategoryScore s) {
                    if (finalKey == null) {
                        double remaining = finalAmount;
                        for (Map.Entry<String, Double> entry : s.getRawScores().entrySet()) {
                            if (remaining <= 0) break;
                            double cur = entry.getValue();
                            double deduct = Math.min(cur, remaining);
                            s.take(entry.getKey(), deduct);
                            remaining -= deduct;
                        }
                    } else {
                        s.take(finalKey, finalAmount);
                    }
                } else if (score instanceof GlobalScore s) {
                    s.take(finalAmount);
                } else if (score instanceof PerItemScore s) {
                    if (finalKey == null) {
                        double remaining = finalAmount;
                        for (Map.Entry<Material, Double> entry : s.getRawScores().entrySet()) {
                            if (remaining <= 0) break;
                            double deduct = Math.min(entry.getValue(), remaining);
                            s.take(entry.getKey(), deduct);
                            remaining -= deduct;
                        }
                    } else {
                        s.take(Material.valueOf(finalKey.toUpperCase()), finalAmount);
                    }
                }
            }
        }

        PlatformSender.sendMessage(sender, GeneralConfiguration.SERIALIZER.deserialize("<#82FB16>Successfully " + action + "d " + finalAmount + " score for " + playerName));
    }
}