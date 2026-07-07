package com.vagas;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.Desktop;
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

    public static void main(String[] args) throws InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();
        BuildIdExtractor extractor = new BuildIdExtractor(httpClient, objectMapper);
        GupyClient gupyClient = new GupyClient(httpClient, objectMapper);
        CsvExporter csvExporter = new CsvExporter();
        HtmlExporter htmlExporter = new HtmlExporter();

        boolean primeiroCiclo = true;

        while (true) {
            System.out.println("=== Ciclo iniciado em " + LocalDateTime.now() + " ===");
            try {
                Path htmlGerado = executarCiclo(extractor, gupyClient, csvExporter, htmlExporter);
                if (primeiroCiclo) {
                    abrirNoNavegador(htmlGerado);
                    primeiroCiclo = false;
                }
            } catch (Exception e) {
                System.out.println("Ciclo falhou: " + e.getMessage());
            }

            System.out.println("Próximo ciclo em " + INTERVALO_ENTRE_CICLOS.toMinutes() + " min. Aguardando...");
            Thread.sleep(INTERVALO_ENTRE_CICLOS.toMillis());
        }
    }

    private static void abrirNoNavegador(Path htmlGerado) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(htmlGerado.toUri());
            }
        } catch (IOException e) {
            System.out.println("Não foi possível abrir o navegador automaticamente: " + e.getMessage());
        }
    }

    private static Path executarCiclo(BuildIdExtractor extractor, GupyClient gupyClient, CsvExporter csvExporter,
            HtmlExporter htmlExporter) throws IOException {
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

        return aplicarFiltro(todasVagas, csvExporter, htmlExporter);
    }

    private static Path aplicarFiltro(List<Vaga> todasVagas, CsvExporter csvExporter, HtmlExporter htmlExporter)
            throws IOException {
        FiltroVagas filtro = FiltroVagas.carregar(Path.of("filtro.txt"));
        Classificador classificador = new Classificador();

        List<VagaClassificada> filtradas = new ArrayList<>();
        for (Vaga vaga : todasVagas) {
            String area = classificador.classificarArea(vaga);
            String senioridade = classificador.classificarSenioridade(vaga);
            if (filtro.aceita(area, senioridade)) {
                filtradas.add(new VagaClassificada(vaga, area, senioridade));
            }
        }

        Path destinoCsv = Path.of("vagas_filtradas.csv");
        Path destinoHtml = Path.of("vagas.html");
        csvExporter.exportarClassificadas(filtradas, destinoCsv);
        htmlExporter.exportar(filtradas, destinoHtml);
        System.out.println(filtradas.size() + " vaga(s) apos filtro em " + destinoCsv.toAbsolutePath()
                + " e " + destinoHtml.toAbsolutePath());

        return destinoHtml;
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
