package marvtechnology.customTownCleanup.util;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 時間関連ユーティリティクラス
 */
public final class TimeUtil {

    /** 1日をミリ秒で表した定数 */
    public static final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;

    /** 日時フォーマット (yyyy-MM-dd HH:mm:ss) JST */
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** JST タイムゾーン */
    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    // インスタンス化禁止
    private TimeUtil() {}

    /**
     * UNIXタイム(ミリ秒)を yyyy-MM-dd HH:mm:ss 形式の文字列に変換
     */
    @NotNull
    public static String format(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), JST)
                .format(FORMATTER);
    }

    /**
     * yyyy-MM-dd HH:mm:ss 形式の文字列を UNIXタイム(ミリ秒)に変換
     */
    public static long parse(@NotNull String dateTime) {
        return LocalDateTime.parse(dateTime, FORMATTER)
                .atZone(JST)
                .toInstant()
                .toEpochMilli();
    }

    /**
     * 現在時刻の文字列(yyyy-MM-dd HH:mm:ss)を取得
     */
    @NotNull
    public static String nowString() {
        return format(System.currentTimeMillis());
    }

    /**
     * 秒数から Minecraft tick に変換 (1秒 = 20tick)
     */
    public static long toTicks(long seconds) {
        return seconds * 20L;
    }
}
