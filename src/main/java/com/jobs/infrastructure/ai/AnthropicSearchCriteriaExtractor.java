package com.jobs.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jobs.application.port.SearchCriteriaExtractor;
import com.jobs.domain.JobFilter;
import com.jobs.domain.UserProfile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class AnthropicSearchCriteriaExtractor implements SearchCriteriaExtractor {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String MODEL = "claude-haiku-4-5-20251001";
    private static final int MAX_TOKENS = 300;

    private static final String AREAS = "RH, TI, Comercial, Financeiro, Marketing, Logistica, Juridico, "
            + "Atendimento, Engenharia, Outro";
    private static final String SENIORITIES = "Estagio, Auxiliar, Assistente, Junior, Pleno, Senior, "
            + "Nao especificado";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public AnthropicSearchCriteriaExtractor(HttpClient httpClient, ObjectMapper objectMapper, String apiKey) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    @Override
    public JobFilter extract(UserProfile profile) throws IOException, InterruptedException {
        String requestBody = buildRequestBody(profile);

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

    String buildRequestBody(UserProfile profile) throws IOException {
        String prompt = """
                Perfil de busca de vaga, em texto livre: %s

                Extraia critérios de busca desse perfil. Categorias de área válidas: %s.
                Categorias de senioridade válidas: %s.

                Responda APENAS com um JSON no formato exato \
                {"areas": [...], "senioridades": [...], "regioes": [...], "remoto": true|false, "palavrasChave": [...]}, \
                sem nenhum texto antes ou depois.

                - "areas": lista com as áreas relevantes (normalmente 1), usando só os nomes exatos da \
                lista acima. Deixe vazio [] se não der pra inferir.
                - "senioridades": lista de senioridades mencionadas ou implícitas, usando só os nomes \
                exatos da lista acima. Deixe vazio [] se não especificado.
                - "regioes": lista de siglas de estado (ex: "RS") ou nomes de cidade mencionados \
                explicitamente como local aceitável. Deixe vazio [] se não houver cidade/estado específico \
                mencionado (mesmo que a pessoa também aceite remoto).
                - "remoto": true se a pessoa aceita ou quer vagas remotas — inclusive quando ela também \
                menciona uma cidade (ex: "Porto Alegre ou remoto" → regioes=["Porto Alegre"], remoto=true; \
                as duas coisas juntas significam "aceito qualquer uma das duas", não que a vaga precise \
                ser as duas ao mesmo tempo). false se a pessoa não mencionar remoto.
                - "palavrasChave": lista de tecnologias/stack específicas mencionadas (ex: "java", ".net", \
                "python", "react") que devem aparecer no título da vaga. Use termos curtos, prováveis de \
                aparecer literalmente no título. Deixe vazio [] se o perfil não menciona uma tecnologia \
                específica (ex: só fala da área/senioridade, sem citar stack).
                """.formatted(profile.description(), AREAS, SENIORITIES);

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", MODEL);
        root.put("max_tokens", MAX_TOKENS);
        ArrayNode messages = root.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        message.put("content", prompt);

        return objectMapper.writeValueAsString(root);
    }

    JobFilter parseResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = root.path("content").path(0).path("text").asText("");
        JsonNode criteria = objectMapper.readTree(extractJson(text));

        return new JobFilter(
                toList(criteria.path("areas")),
                toList(criteria.path("senioridades")),
                toList(criteria.path("regioes")),
                criteria.path("remoto").asBoolean(false),
                toList(criteria.path("palavrasChave")));
    }

    private List<String> toList(JsonNode arrayNode) {
        List<String> values = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode value : arrayNode) {
                String text = value.asText("").strip();
                if (!text.isBlank()) {
                    values.add(text);
                }
            }
        }
        return values;
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
