package com.jobs.infrastructure.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobs.application.port.Notifier;
import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.Job;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class TelegramNotifier implements Notifier {

    private static final int MAX_CHARACTERS_PER_MESSAGE = 3500;
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration WAIT_BETWEEN_ATTEMPTS = Duration.ofSeconds(3);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String token;
    private final String chatId;

    public TelegramNotifier(HttpClient httpClient, ObjectMapper objectMapper, String token, String chatId) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.token = token;
        this.chatId = chatId;
    }

    public static String discoverChatId(HttpClient httpClient, ObjectMapper objectMapper, String token)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.telegram.org/bot" + token + "/getUpdates"))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode results = root.path("result");

        if (!results.isArray() || results.isEmpty()) {
            throw new IllegalStateException(
                    "Nenhuma mensagem encontrada. Mande uma mensagem pro bot no Telegram antes de rodar isso.");
        }

        JsonNode lastMessage = results.get(results.size() - 1);
        return lastMessage.path("message").path("chat").path("id").asText();
    }

    @Override
    public void send(List<ClassifiedJob> newJobs) {
        if (newJobs.isEmpty()) {
            return;
        }

        StringBuilder batch = new StringBuilder();
        batch.append("📋 <b>").append(newJobs.size()).append(" vaga(s) nova(s)</b>\n\n");

        for (ClassifiedJob classifiedJob : newJobs) {
            String block = formatJob(classifiedJob);

            if (batch.length() + block.length() > MAX_CHARACTERS_PER_MESSAGE) {
                sendMessage(batch.toString());
                batch = new StringBuilder();
            }
            batch.append(block);
        }

        if (!batch.isEmpty()) {
            sendMessage(batch.toString());
        }
    }

    private String formatJob(ClassifiedJob classifiedJob) {
        Job job = classifiedJob.job();
        return "🆕 <a href=\"" + job.link() + "\"><b>" + escapeHtml(job.title()) + "</b></a>\n"
                + "🏢 " + escapeHtml(job.company())
                + "  ·  📍 " + escapeHtml(job.city()) + "/" + escapeHtml(job.state())
                + "  ·  🎯 " + escapeHtml(classifiedJob.seniority()) + "\n\n";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void sendMessage(String text) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                if (tryToSend(text)) {
                    return;
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Falha ao enviar notificação no Telegram (tentativa " + attempt
                        + "/" + MAX_ATTEMPTS + "): " + e.getMessage());
            }

            if (attempt < MAX_ATTEMPTS) {
                sleep(WAIT_BETWEEN_ATTEMPTS);
            }
        }
        System.out.println("Desisti de enviar a notificação no Telegram após " + MAX_ATTEMPTS + " tentativas.");
    }

    private boolean tryToSend(String text) throws IOException, InterruptedException {
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String url = "https://api.telegram.org/bot" + token + "/sendMessage"
                + "?chat_id=" + chatId
                + "&text=" + encodedText
                + "&parse_mode=HTML"
                + "&disable_web_page_preview=true";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.out.println("Falha ao enviar notificação no Telegram (status " + response.statusCode()
                    + "): " + response.body());
            return false;
        }
        return true;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
