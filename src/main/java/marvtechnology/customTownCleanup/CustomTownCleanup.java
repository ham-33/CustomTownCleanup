package marvtechnology.customTownCleanup;

import marvtechnology.customTownCleanup.commands.ProtectCommand;
import marvtechnology.customTownCleanup.tasks.CleanupTask;
import marvtechnology.customTownCleanup.util.TimeUtil;
import marvtechnology.customTownCleanup.util.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomTownCleanup extends JavaPlugin {

    private YamlStore configStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configStore = new YamlStore(this, "config.yml");

        /* ---------- コマンド登録 ---------- */
        PluginCommand cmd = getCommand("townprotect");
        if (cmd != null) cmd.setExecutor(new ProtectCommand(this));

        /* ---------- Folia スケジューラ ---------- */
        long periodTick = TimeUtil.toTicks(configStore.getLong("cleanup.interval-seconds", 86_400L));

        // Runnable を Consumer<ScheduledTask> にラップ
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                this,
                task -> new CleanupTask(this, configStore).run(), // ← ★ ラムダでラップ
                0L,
                periodTick
        );

        getLogger().info("[CustomTownCleanup] Enabled.");
    }
}
