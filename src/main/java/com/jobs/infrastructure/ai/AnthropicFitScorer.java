package com.jobs.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jobs.application.port.FitScorer;
import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.FitScore;
import com.jobs.domain.Job;
import com.jobs.domain.UserProfile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AnthropicFitScorer implements FitScorer {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 300;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public AnthropicFitScorer(HttpClient httpClient, ObjectMapper objectMapper, String apiKey, String model) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public FitScore score(UserProfile profile, ClassifiedJob classifiedJob) throws IOException, InterruptedException {
        String requestBody = buildRequestBody(profile, classifiedJob);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(30))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Falha ao chamar a API da Claude (status " + response.statusCode()
                    + "): " + response.body());
        }

        return parseResponse(response.body());
    }

    String buildRequestBody(UserProfile profile, ClassifiedJob classifiedJob) throws IOException {
        Job job = classifiedJob.job();
        String prompt = """
                Perfil da pessoa candidata: %s

                Vaga a avaliar:
                - Titulo: %s
                - Empresa: %s
                - Departamento: %s
                - Local: %s/%s
                - Modelo de trabalho: %s
                - Area classificada: %s
                - Senioridade classificada: %s

                Avalie de 1 a 10 o quanto essa vaga combina com o perfil dessa pessoa, considerando \
                area, senioridade e localizacao. Responda APENAS com um JSON no formato exato \
                {"nota": <numero de 1 a 10>, "justificativa": "<uma frase curta explicando a nota>"}, \
                sem nenhum texto antes ou depois.
                """.formatted(
                profile.description(),
                job.title(),
                job.company(),
                job.department(),
                job.city(), job.state(),
                job.workMode(),
                classifiedJob.area(),
                classifiedJob.seniority());

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", MAX_TOKENS);
        ArrayNode messages = root.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        message.put("content", prompt);

        return objectMapper.writeValueAsString(root);
    }

    FitScore parseResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = root.path("content").path(0).path("text").asText("");
        JsonNode scoreNode = objectMapper.readTree(extractJson(text));
        return new FitScore(scoreNode.path("nota").asInt(), scoreNode.path("justificativa").asText(""));
    }

    private String extractJson(String text) {
        String trimmed = text.strip();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            throw new IllegalStateException("Resposta da IA nao contem JSON valido: " + text);
        }
        return trimmed.substring(start, end + 1);
    }
}
