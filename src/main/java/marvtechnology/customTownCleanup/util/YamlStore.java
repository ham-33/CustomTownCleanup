package marvtechnology.customTownCleanup.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * YAML ファイル簡易ラッパー
 */
public final class YamlStore {

    private final Plugin plugin;
    private final File   file;
    private FileConfiguration conf;

    public YamlStore(@NotNull Plugin plugin, @NotNull String fileName) {
        this.plugin = plugin;
        File folder = plugin.getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("フォルダ作成失敗: " + folder.getPath());
        }
        this.file = new File(folder, fileName);
        try {
            if (!file.exists() && !file.createNewFile()) {
                plugin.getLogger().warning("ファイル作成失敗: " + file.getPath());
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "ファイル生成例外", e);
        }
        reload();
    }

    /* ---------- 基本操作 ---------- */

    public void reload()                       { conf = YamlConfiguration.loadConfiguration(file); }
    public void save()                         { try { conf.save(file); } catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "保存失敗", e); } }
    public boolean contains(String p)          { return conf.contains(p); }
    public String  getString(String p)         { return conf.getString(p); }
    public long    getLong(String p, long def) { return conf.getLong(p, def); }
    public boolean getBoolean(String p, boolean def){ return conf.getBoolean(p, def); }
    public void    set(String p, Object v)     { conf.set(p, v); }

    public @NotNull ConfigurationSection getOrCreateSection(String path) {
        ConfigurationSection cs = conf.getConfigurationSection(path);
        return cs != null ? cs : conf.createSection(path);
    }
}
