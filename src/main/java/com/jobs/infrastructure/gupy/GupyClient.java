package com.jobs.infrastructure.gupy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobs.domain.Company;
import com.jobs.domain.Job;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class GupyClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GupyClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public List<Job> findJobs(Company company, String buildId) throws IOException, InterruptedException {
        JsonNode root = fetchDataJson(company, buildId);
        JsonNode jobs = root.path("pageProps").path("jobs");

        List<Job> result = new ArrayList<>();
        for (JsonNode job : jobs) {
            result.add(toJob(job, company));
        }
        return result;
    }

    private JsonNode fetchDataJson(Company company, String buildId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(company.dataUrl(buildId)))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Falha ao buscar vagas de " + company.subdomain()
                    + " (status " + response.statusCode() + ")");
        }
        return objectMapper.readTree(response.body());
    }

    private Job toJob(JsonNode job, Company company) {
        JsonNode address = job.path("workplace").path("address");

        return new Job(
                job.path("id").asLong(),
                job.path("title").asText("").strip(),
                company.name(),
                job.path("department").asText(""),
                address.path("city").asText(""),
                address.path("stateShortName").asText(""),
                job.path("workplace").path("workplaceType").asText(""),
                company.jobUrl(job.path("id").asLong())
        );
    }
}
