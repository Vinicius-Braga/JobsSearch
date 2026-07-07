package com.vagas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class GupyClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GupyClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public List<Vaga> buscarVagas(Empresa empresa, String buildId) throws IOException, InterruptedException {
        JsonNode root = fetchDataJson(empresa, buildId);
        JsonNode jobs = root.path("pageProps").path("jobs");

        List<Vaga> vagas = new ArrayList<>();
        for (JsonNode job : jobs) {
            vagas.add(paraVaga(job, empresa));
        }
        return vagas;
    }

    private JsonNode fetchDataJson(Empresa empresa, String buildId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(empresa.dataUrl(buildId)))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Falha ao buscar vagas de " + empresa.subdominio()
                    + " (status " + response.statusCode() + ")");
        }
        return objectMapper.readTree(response.body());
    }

    private Vaga paraVaga(JsonNode job, Empresa empresa) {
        JsonNode address = job.path("workplace").path("address");

        return new Vaga(
                job.path("id").asLong(),
                job.path("title").asText("").strip(),
                empresa.nome(),
                job.path("department").asText(""),
                address.path("city").asText(""),
                address.path("stateShortName").asText(""),
                job.path("workplace").path("workplaceType").asText(""),
                empresa.jobUrl(job.path("id").asLong())
        );
    }
}
