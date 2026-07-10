package com.jobs.infrastructure.linkedin;

import com.jobs.application.port.LinkedInJobSource;
import com.jobs.domain.Job;
import com.jobs.domain.JobFilter;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Busca vagas no endpoint público não-autenticado do LinkedIn
 * (jobs-guest/jobs/api/seeMoreJobPostings/search). Não é uma API oficial — é
 * scraping de uma rota interna, contra os Termos de Serviço do LinkedIn. Mais
 * frágil que a Gupy (quebra se o LinkedIn mudar o HTML) e sujeito a rate
 * limiting agressivo (por isso o retry com backoff e o volume de páginas
 * limitado). Ver docs/ROADMAP_V2.md pro contexto completo dessa decisão.
 */
public class LinkedInHttpJobSource implements LinkedInJobSource {

    private static final String SEARCH_URL = "https://www.linkedin.com/jobs-guest/jobs/api/seeMoreJobPostings/search";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0 Safari/537.36";
    private static final int MAX_ATTEMPTS = 4;
    private static final Duration BASE_DELAY = Duration.ofMillis(500);
    private static final Duration MAX_DELAY = Duration.ofSeconds(8);
    private static final int RESULTS_PER_PAGE = 10;
    private static final int MAX_PAGES_PER_QUERY = 2;
    // "Brazil" em inglês, não "Brasil" — testado contra o endpoint real: com "Brasil" o LinkedIn
    // ignora o parâmetro location e devolve vagas dos EUA. Cidade/estado específicos (filter.regions())
    // funcionam normalmente em português.
    private static final String DEFAULT_LOCATION = "Brazil";

    private final HttpClient httpClient;
    private final LinkedInJobParser parser;

    public LinkedInHttpJobSource(HttpClient httpClient, LinkedInJobParser parser) {
        this.httpClient = httpClient;
        this.parser = parser;
    }

    @Override
    public List<Job> search(JobFilter filter) throws IOException, InterruptedException {
        List<Job> jobs = new ArrayList<>();
        for (LinkedInQuery query : buildQueries(filter)) {
            for (int page = 0; page < MAX_PAGES_PER_QUERY; page++) {
                String html = fetchPage(query, page * RESULTS_PER_PAGE);
                List<Job> pageJobs = parser.parse(html, query.workModeLabel());
                if (pageJobs.isEmpty()) {
                    break;
                }
                jobs.addAll(pageJobs);
            }
        }
        return jobs;
    }

    private List<LinkedInQuery> buildQueries(JobFilter filter) {
        String keywords = buildKeywords(filter);
        List<LinkedInQuery> queries = new ArrayList<>();

        if (filter.remoteOnly()) {
            queries.add(new LinkedInQuery(keywords, DEFAULT_LOCATION, "2", "remote"));
        }
        if (!filter.regions().isEmpty()) {
            queries.add(new LinkedInQuery(keywords, filter.regions().get(0), null, ""));
        }
        if (queries.isEmpty()) {
            queries.add(new LinkedInQuery(keywords, DEFAULT_LOCATION, null, ""));
        }
        return queries;
    }

    private String buildKeywords(JobFilter filter) {
        if (!filter.keywords().isEmpty()) {
            return String.join(" ", filter.keywords());
        }
        if (!filter.areas().isEmpty()) {
            return String.join(" ", filter.areas());
        }
        return "";
    }

    private String fetchPage(LinkedInQuery query, int start) throws IOException, InterruptedException {
        String url = buildUrl(query, start);
        Duration delay = BASE_DELAY;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            }
            if (response.statusCode() == 404) {
                return "";
            }
            if (response.statusCode() == 429 || response.statusCode() >= 500) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new IOException("LinkedIn respondeu " + response.statusCode() + " após "
                            + MAX_ATTEMPTS + " tentativas");
                }
                sleep(delay);
                delay = nextDelay(delay);
                continue;
            }
            throw new IOException("LinkedIn respondeu " + response.statusCode());
        }
        return "";
    }

    private String buildUrl(LinkedInQuery query, int start) {
        StringBuilder url = new StringBuilder(SEARCH_URL)
                .append("?keywords=").append(encode(query.keywords()))
                .append("&location=").append(encode(query.location()));
        if (query.fWt() != null) {
            url.append("&f_WT=").append(query.fWt());
        }
        url.append("&start=").append(start);
        return url.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private Duration nextDelay(Duration current) {
        long jitterMillis = ThreadLocalRandom.current().nextLong(0, 200);
        Duration next = current.multipliedBy(2).plusMillis(jitterMillis);
        return next.compareTo(MAX_DELAY) > 0 ? MAX_DELAY : next;
    }

    private void sleep(Duration duration) throws InterruptedException {
        Thread.sleep(duration.toMillis());
    }
}
