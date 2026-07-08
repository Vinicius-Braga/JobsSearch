package com.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobs.application.RunCycleUseCase;
import com.jobs.application.port.Notifier;
import com.jobs.domain.Classifier;
import com.jobs.infrastructure.config.CompanyFileLoader;
import com.jobs.infrastructure.csv.CsvExporter;
import com.jobs.infrastructure.csv.CsvPublisher;
import com.jobs.infrastructure.csv.CsvJobRepository;
import com.jobs.infrastructure.gupy.BuildIdExtractor;
import com.jobs.infrastructure.gupy.GupyClient;
import com.jobs.infrastructure.gupy.GupyJobSource;
import com.jobs.infrastructure.html.HtmlPublisher;
import com.jobs.infrastructure.telegram.TelegramConfig;
import com.jobs.infrastructure.telegram.TelegramNotifier;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class Main {

    private static final Duration CYCLE_INTERVAL = Duration.ofHours(6);

    public static void main(String[] args) throws InterruptedException, IOException {
        // O IPv4 do Telegram é bloqueado nesta rede; força IPv6, que conecta normalmente.
        System.setProperty("java.net.preferIPv6Addresses", "true");

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        ObjectMapper objectMapper = new ObjectMapper();

        RunCycleUseCase useCase = buildUseCase(httpClient, objectMapper);
        Notifier telegramNotifier = configureTelegram(httpClient, objectMapper);

        boolean isFirstEverRun = !useCase.hasHistory();

        while (true) {
            System.out.println("=== Ciclo iniciado em " + LocalDateTime.now() + " ===");
            try {
                useCase.run(isFirstEverRun ? null : telegramNotifier);
                isFirstEverRun = false;
            } catch (Exception e) {
                System.out.println("Ciclo falhou: " + e.getMessage());
            }

            System.out.println("Próximo ciclo em " + CYCLE_INTERVAL.toMinutes() + " min. Aguardando...");
            Thread.sleep(CYCLE_INTERVAL.toMillis());
        }
    }

    private static RunCycleUseCase buildUseCase(HttpClient httpClient, ObjectMapper objectMapper) {
        CsvExporter csvExporter = new CsvExporter();

        return new RunCycleUseCase(
                new CompanyFileLoader(Path.of("empresas.txt")),
                new GupyJobSource(
                        new BuildIdExtractor(httpClient, objectMapper),
                        new GupyClient(httpClient, objectMapper)),
                new CsvJobRepository(csvExporter, Path.of("vagas.csv")),
                List.of(
                        new CsvPublisher(csvExporter, Path.of("vagas_filtradas.csv")),
                        new HtmlPublisher(Path.of("vagas.html"))
                ),
                new Classifier(),
                Path.of("filtro.txt")
        );
    }

    private static Notifier configureTelegram(HttpClient httpClient, ObjectMapper objectMapper) throws IOException {
        Path telegramFile = Path.of("telegram.txt");
        TelegramConfig config = TelegramConfig.load(telegramFile);
        if (config == null) {
            System.out.println("telegram.txt não configurado - notificações desativadas.");
            return null;
        }

        if (config.chatId() != null) {
            return new TelegramNotifier(httpClient, objectMapper, config.token(), config.chatId());
        }

        try {
            String chatId = TelegramNotifier.discoverChatId(httpClient, objectMapper, config.token());
            TelegramConfig.saveChatId(telegramFile, config.token(), chatId);
            System.out.println("Chat do Telegram descoberto e salvo: " + chatId);
            return new TelegramNotifier(httpClient, objectMapper, config.token(), chatId);
        } catch (Exception e) {
            System.out.println("Não foi possível configurar o Telegram: " + e.getMessage());
            return null;
        }
    }
}
