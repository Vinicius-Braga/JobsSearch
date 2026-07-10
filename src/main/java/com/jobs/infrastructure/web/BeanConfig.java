package com.jobs.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobs.application.SearchAndScoreJobsUseCase;
import com.jobs.application.SearchJobsForProfileUseCase;
import com.jobs.application.SearchJobsUseCase;
import com.jobs.application.port.CompanyLoader;
import com.jobs.application.port.FitScorer;
import com.jobs.application.port.LinkedInJobSource;
import com.jobs.application.port.SearchCriteriaExtractor;
import com.jobs.domain.Classifier;
import com.jobs.infrastructure.ai.AnthropicFitScorer;
import com.jobs.infrastructure.ai.AnthropicSearchCriteriaExtractor;
import com.jobs.infrastructure.config.AnthropicProperties;
import com.jobs.infrastructure.config.CompanyFileLoader;
import com.jobs.infrastructure.gupy.BuildIdExtractor;
import com.jobs.infrastructure.gupy.GupyClient;
import com.jobs.infrastructure.gupy.GupyJobSource;
import com.jobs.infrastructure.linkedin.LinkedInHttpJobSource;
import com.jobs.infrastructure.linkedin.LinkedInJobParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;

@Configuration
public class BeanConfig {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    public GupyJobSource gupyJobSource(HttpClient httpClient, ObjectMapper objectMapper) {
        return new GupyJobSource(
                new BuildIdExtractor(httpClient, objectMapper),
                new GupyClient(httpClient, objectMapper));
    }

    @Bean
    public Classifier classifier() {
        return new Classifier();
    }

    @Bean
    public LinkedInJobSource linkedInJobSource(HttpClient httpClient) {
        return new LinkedInHttpJobSource(httpClient, new LinkedInJobParser());
    }

    @Bean
    public SearchJobsUseCase searchJobsUseCase(GupyJobSource gupyJobSource, LinkedInJobSource linkedInJobSource,
            Classifier classifier) {
        return new SearchJobsUseCase(gupyJobSource, linkedInJobSource, classifier);
    }

    @Bean
    public FitScorer fitScorer(HttpClient httpClient, ObjectMapper objectMapper, AnthropicProperties anthropicProperties) {
        return new AnthropicFitScorer(httpClient, objectMapper, anthropicProperties.apiKey());
    }

    @Bean
    public CompanyLoader companyLoader() {
        return new CompanyFileLoader(Path.of("empresas.txt"));
    }

    @Bean
    public SearchCriteriaExtractor searchCriteriaExtractor(HttpClient httpClient, ObjectMapper objectMapper,
            AnthropicProperties anthropicProperties) {
        return new AnthropicSearchCriteriaExtractor(httpClient, objectMapper, anthropicProperties.apiKey());
    }

    // Pronto pra religar a pontuação por IA por vaga — hoje não é chamado no fluxo web
    // (ver SearchJobsForProfileUseCase), decisão de custo/simplicidade tomada por enquanto.
    @Bean
    public SearchAndScoreJobsUseCase searchAndScoreJobsUseCase(SearchJobsUseCase searchJobsUseCase, FitScorer fitScorer,
            SearchCriteriaExtractor searchCriteriaExtractor) {
        return new SearchAndScoreJobsUseCase(searchJobsUseCase, fitScorer, searchCriteriaExtractor);
    }

    @Bean
    public SearchJobsForProfileUseCase searchJobsForProfileUseCase(SearchJobsUseCase searchJobsUseCase,
            SearchCriteriaExtractor searchCriteriaExtractor) {
        return new SearchJobsForProfileUseCase(searchJobsUseCase, searchCriteriaExtractor);
    }
}
