package com.jobs.infrastructure.gupy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobs.domain.Company;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildIdExtractor {

    private static final Pattern NEXT_DATA_SCRIPT = Pattern.compile(
            "<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>",
            Pattern.DOTALL);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BuildIdExtractor(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public String extract(Company company) throws IOException, InterruptedException {
        String html = fetchHomeHtml(company);
        String nextDataJson = extractNextDataJson(html, company);
        return extractBuildId(nextDataJson, company);
    }

    private String fetchHomeHtml(Company company) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(company.homeUrl()))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Falha ao buscar HTML de " + company.subdomain()
                    + " (status " + response.statusCode() + ")");
        }
        return response.body();
    }

    private String extractNextDataJson(String html, Company company) {
        Matcher matcher = NEXT_DATA_SCRIPT.matcher(html);
        if (!matcher.find()) {
            throw new IllegalStateException("Tag __NEXT_DATA__ não encontrada para " + company.subdomain());
        }
        return matcher.group(1);
    }

    private String extractBuildId(String nextDataJson, Company company) throws IOException {
        JsonNode root = objectMapper.readTree(nextDataJson);
        JsonNode buildIdNode = root.get("buildId");
        if (buildIdNode == null || buildIdNode.asText().isBlank()) {
            throw new IllegalStateException("Campo buildId não encontrado para " + company.subdomain());
        }
        return buildIdNode.asText();
    }
}
