package com.jobs.infrastructure.telegram;

import com.jobs.infrastructure.config.EnvLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public record TelegramConfig(String token, String chatId) {

    public static TelegramConfig from(Map<String, String> env) {
        String token = env.get("TELEGRAM_TOKEN");
        if (token == null || token.isBlank()) {
            return null;
        }
        String chatId = env.get("TELEGRAM_CHAT_ID");
        return new TelegramConfig(token, (chatId == null || chatId.isBlank()) ? null : chatId);
    }

    public static void saveChatId(Path envFile, Map<String, String> env, String token, String chatId)
            throws IOException {
        Map<String, String> updated = new LinkedHashMap<>(env);
        updated.put("TELEGRAM_TOKEN", token);
        updated.put("TELEGRAM_CHAT_ID", chatId);
        EnvLoader.save(envFile, updated);
    }
}
