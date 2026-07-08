package com.vagas;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {

    private static final Duration INTERVALO_ENTRE_CICLOS = Duration.ofHours(6);

    public static void main(String[] args) throws InterruptedException, IOException {
        // O IPv4 do Telegram é bloqueado nesta rede; força IPv6, que conecta normalmente.
        System.setProperty("java.net.preferIPv6Addresses", "true");

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        ObjectMapper objectMapper = new ObjectMapper();
        BuildIdExtractor extractor = new BuildIdExtractor(httpClient, objectMapper);
        GupyClient gupyClient = new GupyClient(httpClient, objectMapper);
        CsvExporter csvExporter = new CsvExporter();
        HtmlExporter htmlExporter = new HtmlExporter();
        TelegramNotifier telegramNotifier = configurarTelegram(httpClient, objectMapper);

        boolean primeiroCiclo = true;

        while (true) {
            System.out.println("=== Ciclo iniciado em " + LocalDateTime.now() + " ===");
            try {
                executarCiclo(extractor, gupyClient, csvExporter, htmlExporter,
                        primeiroCiclo ? null : telegramNotifier);
                primeiroCiclo = false;
            } catch (Exception e) {
                System.out.println("Ciclo falhou: " + e.getMessage());
            }

            System.out.println("Próximo ciclo em " + INTERVALO_ENTRE_CICLOS.toMinutes() + " min. Aguardando...");
            Thread.sleep(INTERVALO_ENTRE_CICLOS.toMillis());
        }
    }

    private static TelegramNotifier configurarTelegram(HttpClient httpClient, ObjectMapper objectMapper)
            throws IOException {
        Path arquivoTelegram = Path.of("telegram.txt");
        TelegramConfig config = TelegramConfig.carregar(arquivoTelegram);
        if (config == null) {
            System.out.println("telegram.txt não configurado - notificações desativadas.");
            return null;
        }

        if (config.chatId() != null) {
            return new TelegramNotifier(httpClient, objectMapper, config.token(), config.chatId());
        }

        try {
            String chatId = TelegramNotifier.descobrirChatId(httpClient, objectMapper, config.token());
            TelegramConfig.salvarChatId(arquivoTelegram, config.token(), chatId);
            System.out.println("Chat do Telegram descoberto e salvo: " + chatId);
            return new TelegramNotifier(httpClient, objectMapper, config.token(), chatId);
        } catch (Exception e) {
            System.out.println("Não foi possível configurar o Telegram: " + e.getMessage());
            return null;
        }
    }

    private static void executarCiclo(BuildIdExtractor extractor, GupyClient gupyClient, CsvExporter csvExporter,
            HtmlExporter htmlExporter, TelegramNotifier telegramNotifier) throws IOException {
        Path destino = Path.of("vagas.csv");
        List<Vaga> vagasExistentes = csvExporter.carregarExistentes(destino);
        Set<Long> idsVistos = new HashSet<>();
        for (Vaga vaga : vagasExistentes) {
            idsVistos.add(vaga.id());
        }

        List<Empresa> empresas = carregarEmpresas(Path.of("empresas.txt"));
        List<Vaga> vagasNovas = new ArrayList<>();

        for (Empresa empresa : empresas) {
            try {
                String buildId = extractor.extract(empresa);
                List<Vaga> vagas = gupyClient.buscarVagas(empresa, buildId);
                int novasDaEmpresa = 0;
                for (Vaga vaga : vagas) {
                    if (idsVistos.add(vaga.id())) {
                        vagasNovas.add(vaga);
                        novasDaEmpresa++;
                    }
                }
                System.out.println(empresa.nome() + " -> " + vagas.size() + " vaga(s), "
                        + novasDaEmpresa + " nova(s)");
            } catch (Exception e) {
                System.out.println(empresa.nome() + " -> ERRO: " + e.getMessage());
            }
        }

        List<Vaga> todasVagas = new ArrayList<>(vagasExistentes);
        todasVagas.addAll(vagasNovas);

        csvExporter.exportar(todasVagas, destino);
        System.out.println(vagasNovas.size() + " vaga(s) nova(s) adicionada(s). Total acumulado: "
                + todasVagas.size() + " vaga(s) em " + destino.toAbsolutePath());

        aplicarFiltro(todasVagas, vagasNovas, csvExporter, htmlExporter, telegramNotifier);
    }

    private static void aplicarFiltro(List<Vaga> todasVagas, List<Vaga> vagasNovas, CsvExporter csvExporter,
            HtmlExporter htmlExporter, TelegramNotifier telegramNotifier) throws IOException {
        FiltroVagas filtro = FiltroVagas.carregar(Path.of("filtro.txt"));
        Classificador classificador = new Classificador();

        List<VagaClassificada> filtradas = new ArrayList<>();
        List<VagaClassificada> filtradasNovas = new ArrayList<>();
        Set<Long> idsNovas = new HashSet<>();
        for (Vaga vaga : vagasNovas) {
            idsNovas.add(vaga.id());
        }

        for (Vaga vaga : todasVagas) {
            String area = classificador.classificarArea(vaga);
            String senioridade = classificador.classificarSenioridade(vaga);
            if (filtro.aceita(area, senioridade, vaga.cidade(), vaga.estado())) {
                VagaClassificada vagaClassificada = new VagaClassificada(vaga, area, senioridade);
                filtradas.add(vagaClassificada);
                if (idsNovas.contains(vaga.id())) {
                    filtradasNovas.add(vagaClassificada);
                }
            }
        }

        Path destinoCsv = Path.of("vagas_filtradas.csv");
        Path destinoHtml = Path.of("vagas.html");
        csvExporter.exportarClassificadas(filtradas, destinoCsv);
        htmlExporter.exportar(filtradas, destinoHtml);
        System.out.println(filtradas.size() + " vaga(s) apos filtro em " + destinoCsv.toAbsolutePath()
                + " e " + destinoHtml.toAbsolutePath());

        if (telegramNotifier != null) {
            telegramNotifier.notificarVagasNovas(filtradasNovas);
            if (!filtradasNovas.isEmpty()) {
                System.out.println(filtradasNovas.size() + " vaga(s) nova(s) notificada(s) no Telegram.");
            }
        }
    }

    private static List<Empresa> carregarEmpresas(Path arquivo) throws IOException {
        List<Empresa> empresas = new ArrayList<>();
        for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
            if (linha.isBlank()) {
                continue;
            }
            String[] partes = linha.split(",", 2);
            String subdominio = partes[0].strip();
            String nome = partes.length > 1 ? partes[1].strip() : subdominio;
            empresas.add(new Empresa(nome, subdominio));
        }
        return empresas;
    }
}
