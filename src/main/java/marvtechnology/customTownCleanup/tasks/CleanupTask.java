package marvtechnology.customTownCleanup.tasks;

import com.google.gson.JsonObject;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import marvtechnology.customTownCleanup.util.TimeUtil;
import marvtechnology.customTownCleanup.util.WebhookSender;
import marvtechnology.customTownCleanup.util.YamlStore;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.logging.Level;

/**
 * - 非アクティブ Town / Nation を検知<br>
 * - 削除 1 日前に Discord 通知<br>
 * - 24h 後も状況が変わらなければ Towny から削除
 */
@SuppressWarnings("deprecation")    // Towny removeTown/removeNation が deprecated
public final class CleanupTask implements Runnable {

    /* ───────────────── フィールド ───────────────── */

    private final Plugin    plugin;
    private final YamlStore cfg;          // config.yml
    private final YamlStore prot;         // protected.yml
    private final YamlStore unprot;       // unprotected.yml
    private final YamlStore log;          // deletion_log.yml
    private final long      inactiveMs;   // 非アクティブ判定閾値

    /* ───────────────── コンストラクタ ───────────────── */

    public CleanupTask(@NotNull Plugin plugin,
                       @NotNull YamlStore configStore) {
        this.plugin = plugin;
        this.cfg    = configStore;
        this.prot   = new YamlStore(plugin, "protected.yml");
        this.unprot = new YamlStore(plugin, "unprotected.yml");
        this.log    = new YamlStore(plugin, "deletion_log.yml");

        long days = cfg.getLong("inactive_days", 7L);
        this.inactiveMs = days * TimeUtil.MILLIS_PER_DAY;
    }

    /* ─────────────────  Runnable  ───────────────── */

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        // Towny API は同期スレッド必須 → グローバル同期で実行
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            scanTowns(now);
            scanNations(now);
            if (cfg.getBoolean("persist_logs", true)) {
                prot.save();
                unprot.save();
                log.save();
            }
        });
    }

    /* ───────────────── Town 処理 ───────────────── */

    private void scanTowns(long now) {
        for (Town town : TownyUniverse.getInstance().getTowns()) {
            String name = town.getName();
            try {
                if (prot.contains("protected_towns." + name))            continue;
                if (isInGrace("unprotected_towns." + name, now))        continue;
                if (!allResidentsInactive(town.getResidents(), now))    continue;

                deleteOrNotify("towns", name, now,
                        () -> TownyUniverse.getInstance().getDataSource().removeTown(town));

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Town 処理失敗: " + name, e);
            }
        }
    }

    /* ───────────────── Nation 処理 ───────────────── */

    private void scanNations(long now) {
        for (Nation nat : TownyUniverse.getInstance().getNations()) {
            String name = nat.getName();
            try {
                if (prot.contains("protected_nations." + name))          continue;
                if (isInGrace("unprotected_nations." + name, now))      continue;
                if (!allNationResidentsInactive(nat, now))              continue;

                deleteOrNotify("nations", name, now,
                        () -> TownyUniverse.getInstance().getDataSource().removeNation(nat));

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Nation 処理失敗: " + name, e);
            }
        }
    }

    /* ───────────────── 判定ユーティリティ ───────────────── */

    private boolean allResidentsInactive(Collection<Resident> list, long now) {
        for (Resident r : list) {
            if (now - r.getLastOnline() < inactiveMs) return false;
        }
        return true;
    }

    private boolean allNationResidentsInactive(Nation nat, long now) {
        for (Town t : nat.getTowns()) {
            if (!allResidentsInactive(t.getResidents(), now)) return false;
        }
        return true;
    }

    private boolean isInGrace(String path, long now) {
        String ts = unprot.getString(path);
        return ts != null && now - TimeUtil.parse(ts) < inactiveMs;
    }

    /* ───────────────── 通知 & 削除ハンドラ ───────────────── */

    @FunctionalInterface private interface Deleter { void run() throws Exception; }

    private void deleteOrNotify(String type, String name, long now,
                                @NotNull Deleter deleter) throws Exception {

        String keyNotif = "notified_" + type + '.' + name;
        String keyDel   = "deleted_"  + type + '.' + name;

        /* --- まだ通知していなければ警告を送るだけ --- */
        if (!log.contains(keyNotif)) {
            warnDiscord(name);
            log.set(keyNotif, TimeUtil.format(now));
            plugin.getLogger().info("🔔 通知: " + type + ' ' + name);
            return;
        }

        /* --- 通知から 24h 経っていなければ待機 --- */
        if (now - TimeUtil.parse(log.getString(keyNotif)) < TimeUtil.MILLIS_PER_DAY) return;

        /* --- 24h 経過したので削除 --- */
        deleter.run();
        log.set(keyDel,  TimeUtil.format(now));
        log.set(keyNotif, null);
        plugin.getLogger().info("🗑️ 削除: " + type + ' ' + name);
    }

    /* ───────────────── Discord 通知 ───────────────── */

    private void warnDiscord(String target) {
        String url = cfg.getString("discord_webhook_url");
        if (url == null || url.isBlank()) {
            plugin.getLogger().warning("[CleanupTask] webhook URL 未設定");
            return;
        }

        String mode = cfg.getString("message_type", "embed").toLowerCase();

        JsonObject embed = new JsonObject();
        embed.addProperty("title", "⚠️ 自動削除警告");
        embed.addProperty("description",
                '`' + target + "` は非アクティブのため **明日削除** 予定です。");
        embed.addProperty("color", 0xFFAA00);

        WebhookSender.send(url, mode, embed);
    }
}
