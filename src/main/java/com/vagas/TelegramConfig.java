package com.vagas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record TelegramConfig(String token, String chatId) {

    public static TelegramConfig carregar(Path arquivo) throws IOException {
        if (!Files.exists(arquivo)) {
            return null;
        }

        String token = null;
        String chatId = null;

        for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
            String linhaLimpa = linha.strip();
            if (linhaLimpa.isBlank() || linhaLimpa.startsWith("#")) {
                continue;
            }
            String[] partes = linhaLimpa.split("=", 2);
            if (partes.length != 2) {
                continue;
            }
            String chave = partes[0].strip().toLowerCase();
            String valor = partes[1].strip();
            if (chave.equals("token")) {
                token = valor;
            } else if (chave.equals("chat_id")) {
                chatId = valor;
            }
        }

        if (token == null || token.isBlank()) {
            return null;
        }
        return new TelegramConfig(token, (chatId == null || chatId.isBlank()) ? null : chatId);
    }

    public static void salvarChatId(Path arquivo, String token, String chatId) throws IOException {
        List<String> linhas = List.of(
                "# Configuração do bot do Telegram. Não compartilhe este arquivo (contém credencial).",
                "token=" + token,
                "chat_id=" + chatId
        );
        Files.write(arquivo, linhas, StandardCharsets.UTF_8);
    }
}
