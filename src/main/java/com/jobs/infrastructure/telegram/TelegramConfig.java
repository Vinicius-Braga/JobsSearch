package com.jobs.infrastructure.telegram;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record TelegramConfig(String token, String chatId) {

    public static TelegramConfig load(Path file) throws IOException {
        if (!Files.exists(file)) {
            return null;
        }

        String token = null;
        String chatId = null;

        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String cleanLine = line.strip();
            if (cleanLine.isBlank() || cleanLine.startsWith("#")) {
                continue;
            }
            String[] parts = cleanLine.split("=", 2);
            if (parts.length != 2) {
                continue;
            }
            String key = parts[0].strip().toLowerCase();
            String value = parts[1].strip();
            if (key.equals("token")) {
                token = value;
            } else if (key.equals("chat_id")) {
                chatId = value;
            }
        }

        if (token == null || token.isBlank()) {
            return null;
        }
        return new TelegramConfig(token, (chatId == null || chatId.isBlank()) ? null : chatId);
    }

    public static void saveChatId(Path file, String token, String chatId) throws IOException {
        List<String> lines = List.of(
                "# Configuração do bot do Telegram. Não compartilhe este arquivo (contém credencial).",
                "token=" + token,
                "chat_id=" + chatId
        );
        Files.write(file, lines, StandardCharsets.UTF_8);
    }
}
