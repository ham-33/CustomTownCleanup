package marvtechnology.customTownCleanup.commands;

import marvtechnology.customTownCleanup.util.TimeUtil;
import marvtechnology.customTownCleanup.util.YamlStore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ProtectCommand implements CommandExecutor {

    private final YamlStore protectedS;
    private final YamlStore unprotectedS;

    public ProtectCommand(@NotNull org.bukkit.plugin.Plugin plugin) {
        protectedS   = new YamlStore(plugin, "protected.yml");
        unprotectedS = new YamlStore(plugin, "unprotected.yml");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command cmd,
                             @NotNull String lbl,
                             @NotNull String[] args) {

        if (!sender.hasPermission("customcleanup.protect")) {
            sender.sendMessage("§c権限がありません"); return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§e/townprotect <add|remove|list> <Name>"); return true;
        }

        String sub = args[0].toLowerCase();
        if ("list".equals(sub)) {
            sender.sendMessage("§a保護中: " +
                    protectedS.getOrCreateSection("protected_towns").getKeys(false));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§c対象名を指定してください"); return true;
        }
        String name = args[1];
        String now  = TimeUtil.nowString();

        switch (sub) {
            case "add" -> {
                protectedS.set("protected_towns." + name, now);
                protectedS.set("protected_nations." + name, now);
                unprotectedS.set("unprotected_towns." + name, null);
                unprotectedS.set("unprotected_nations." + name, null);
                sender.sendMessage("§b保護を追加: " + name);
            }
            case "remove" -> {
                protectedS.set("protected_towns." + name, null);
                protectedS.set("protected_nations." + name, null);
                unprotectedS.set("unprotected_towns." + name, now);
                unprotectedS.set("unprotected_nations." + name, now);
                sender.sendMessage("§b保護を解除: " + name);
            }
            default -> sender.sendMessage("§c不明なサブコマンド");
        }
        protectedS.save(); unprotectedS.save();
        return true;
    }
}
