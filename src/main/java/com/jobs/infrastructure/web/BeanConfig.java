package com.jobs.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobs.application.SearchJobsUseCase;
import com.jobs.application.port.FitScorer;
import com.jobs.domain.Classifier;
import com.jobs.infrastructure.ai.AnthropicFitScorer;
import com.jobs.infrastructure.config.AnthropicProperties;
import com.jobs.infrastructure.gupy.BuildIdExtractor;
import com.jobs.infrastructure.gupy.GupyClient;
import com.jobs.infrastructure.gupy.GupyJobSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
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
    public SearchJobsUseCase searchJobsUseCase(GupyJobSource gupyJobSource, Classifier classifier) {
        return new SearchJobsUseCase(gupyJobSource, classifier);
    }

    @Bean
    public FitScorer fitScorer(HttpClient httpClient, ObjectMapper objectMapper, AnthropicProperties anthropicProperties) {
        return new AnthropicFitScorer(httpClient, objectMapper, anthropicProperties.apiKey());
    }
}
