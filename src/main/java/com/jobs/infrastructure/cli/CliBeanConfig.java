package com.jobs.infrastructure.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobs.application.RunCycleUseCase;
import com.jobs.application.port.CompanyLoader;
import com.jobs.application.port.JobPublisher;
import com.jobs.application.port.JobRepository;
import com.jobs.application.port.JobSource;
import com.jobs.application.port.Notifier;
import com.jobs.domain.Classifier;
import com.jobs.infrastructure.config.EnvLoader;
import com.jobs.infrastructure.config.TelegramProperties;
import com.jobs.infrastructure.config.CompanyFileLoader;
import com.jobs.infrastructure.csv.CsvExporter;
import com.jobs.infrastructure.csv.CsvJobRepository;
import com.jobs.infrastructure.csv.CsvPublisher;
import com.jobs.infrastructure.gupy.BuildIdExtractor;
import com.jobs.infrastructure.gupy.GupyClient;
import com.jobs.infrastructure.gupy.GupyJobSource;
import com.jobs.infrastructure.html.HtmlPublisher;
import com.jobs.infrastructure.notification.NoOpNotifier;
import com.jobs.infrastructure.telegram.TelegramNotifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Configuration
public class CliBeanConfig {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    public CompanyLoader companyLoader() {
        return new CompanyFileLoader(Path.of("empresas.txt"));
    }

    @Bean
    public JobSource jobSource(HttpClient httpClient, ObjectMapper objectMapper) {
        return new GupyJobSource(
                new BuildIdExtractor(httpClient, objectMapper),
                new GupyClient(httpClient, objectMapper));
    }

    @Bean
    public Classifier classifier() {
        return new Classifier();
    }

    @Bean
    public CsvExporter csvExporter() {
        return new CsvExporter();
    }

    @Bean
    public JobRepository jobRepository(CsvExporter csvExporter) {
        return new CsvJobRepository(csvExporter, Path.of("vagas.csv"));
    }

    @Bean
    public List<JobPublisher> jobPublishers(CsvExporter csvExporter) {
        return List.of(
                new CsvPublisher(csvExporter, Path.of("vagas_filtradas.csv")),
                new HtmlPublisher(Path.of("vagas.html")));
    }

    @Bean
    public RunCycleUseCase runCycleUseCase(CompanyLoader companyLoader, JobSource jobSource,
            JobRepository jobRepository, List<JobPublisher> jobPublishers, Classifier classifier) {
        return new RunCycleUseCase(companyLoader, jobSource, jobRepository, jobPublishers, classifier,
                Path.of("filtro.txt"));
    }

    @Bean
    public Notifier notifier(HttpClient httpClient, ObjectMapper objectMapper, TelegramProperties telegramProperties) {
        if (telegramProperties.token() == null || telegramProperties.token().isBlank()) {
            System.out.println(".env não tem TELEGRAM_TOKEN configurado - notificações desativadas.");
            return NoOpNotifier.INSTANCE;
        }

        if (telegramProperties.chatId() != null && !telegramProperties.chatId().isBlank()) {
            return new TelegramNotifier(httpClient, objectMapper, telegramProperties.token(), telegramProperties.chatId());
        }

        try {
            String chatId = TelegramNotifier.discoverChatId(httpClient, objectMapper, telegramProperties.token());
            saveDiscoveredChatId(telegramProperties.token(), chatId);
            System.out.println("Chat do Telegram descoberto e salvo: " + chatId);
            return new TelegramNotifier(httpClient, objectMapper, telegramProperties.token(), chatId);
        } catch (Exception e) {
            System.out.println("Não foi possível configurar o Telegram: " + e.getMessage());
            return NoOpNotifier.INSTANCE;
        }
    }

    private void saveDiscoveredChatId(String token, String chatId) throws Exception {
        Path envFile = Path.of(".env");
        Map<String, String> values = EnvLoader.load(envFile);
        values.put("TELEGRAM_TOKEN", token);
        values.put("TELEGRAM_CHAT_ID", chatId);
        EnvLoader.save(envFile, values);
    }
}
