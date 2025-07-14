package marvtechnology.customTownCleanup.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Discord Webhook 通信用ユーティリティ
 */
public final class WebhookSender {

    // インスタンス化禁止
    private WebhookSender() {}

    /**
     * Webhook にメッセージを送信する
     *
     * @param webhookUrl Discord Webhook URL
     * @param mode       "embed" または "plain"
     * @param embed      embed 情報を格納した JsonObject (plain モードの際は description プロパティを使用)
     */
    public static void send(@NotNull String webhookUrl,
                            @NotNull String mode,
                            @NotNull JsonObject embed) {
        try {
            HttpURLConnection connection = (HttpURLConnection)
                    URI.create(webhookUrl).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            String payload;
            if ("embed".equalsIgnoreCase(mode)) {
                JsonObject root = new JsonObject();
                JsonArray arr = new JsonArray();
                arr.add(embed);
                root.add("embeds", arr);
                payload = root.toString();
            } else {
                String content = embed.has("description")
                        ? embed.get("description").getAsString()
                        : "";
                // Markdownタグ除去
                content = content.replaceAll("\\*", "");
                JsonObject root = new JsonObject();
                root.addProperty("content", content);
                payload = root.toString();
            }

            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            // レスポンス取得で完了
            connection.getInputStream().close();
        } catch (Exception e) {
            // プラグインログが使えないユーティリティクラスのため標準エラー出力
            System.err.println("[WebhookSender] 送信失敗: " + e.getMessage());
        }
    }
}
