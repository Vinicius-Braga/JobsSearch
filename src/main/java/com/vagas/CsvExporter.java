package com.vagas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CsvExporter {

    private static final String[] CABECALHO = {
            "id", "titulo", "empresa", "departamento", "cidade", "estado", "modalidade", "link"
    };

    private static final String[] CABECALHO_CLASSIFICADO = {
            "id", "titulo", "empresa", "departamento", "cidade", "estado", "modalidade",
            "area", "senioridade", "link"
    };

    private static final Pattern LINHA_CSV = Pattern.compile(
            "^(\\d+),\"((?:[^\"]|\"\")*)\",\"((?:[^\"]|\"\")*)\",\"((?:[^\"]|\"\")*)\","
                    + "\"((?:[^\"]|\"\")*)\",\"((?:[^\"]|\"\")*)\",\"((?:[^\"]|\"\")*)\",\"((?:[^\"]|\"\")*)\"$");

    public List<Vaga> carregarExistentes(Path arquivo) throws IOException {
        List<Vaga> vagas = new ArrayList<>();
        if (!Files.exists(arquivo)) {
            return vagas;
        }

        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        for (String linha : linhas.subList(Math.min(1, linhas.size()), linhas.size())) {
            Matcher matcher = LINHA_CSV.matcher(linha);
            if (!matcher.matches()) {
                System.err.println("Aviso: linha do CSV ignorada por não bater com o formato esperado: " + linha);
                continue;
            }
            vagas.add(new Vaga(
                    Long.parseLong(matcher.group(1)),
                    desescapar(matcher.group(2)),
                    desescapar(matcher.group(3)),
                    desescapar(matcher.group(4)),
                    desescapar(matcher.group(5)),
                    desescapar(matcher.group(6)),
                    desescapar(matcher.group(7)),
                    desescapar(matcher.group(8))
            ));
        }
        return vagas;
    }

    private String desescapar(String valor) {
        return valor.replace("\"\"", "\"");
    }

    public void exportar(List<Vaga> vagas, Path destino) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", CABECALHO)).append("\n");

        for (Vaga vaga : vagas) {
            csv.append(linha(vaga)).append("\n");
        }

        Files.writeString(destino, csv.toString(), StandardCharsets.UTF_8);
    }

    private String linha(Vaga vaga) {
        return String.join(",",
                String.valueOf(vaga.id()),
                escapar(vaga.titulo()),
                escapar(vaga.empresa()),
                escapar(vaga.departamento()),
                escapar(vaga.cidade()),
                escapar(vaga.estado()),
                escapar(vaga.modalidade()),
                escapar(vaga.link())
        );
    }

    private String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        String escapado = valor.replace("\"", "\"\"");
        return "\"" + escapado + "\"";
    }

    public void exportarClassificadas(List<VagaClassificada> vagas, Path destino) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", CABECALHO_CLASSIFICADO)).append("\n");

        for (VagaClassificada vagaClassificada : vagas) {
            csv.append(linhaClassificada(vagaClassificada)).append("\n");
        }

        Files.writeString(destino, csv.toString(), StandardCharsets.UTF_8);
    }

    private String linhaClassificada(VagaClassificada vagaClassificada) {
        Vaga vaga = vagaClassificada.vaga();
        return String.join(",",
                String.valueOf(vaga.id()),
                escapar(vaga.titulo()),
                escapar(vaga.empresa()),
                escapar(vaga.departamento()),
                escapar(vaga.cidade()),
                escapar(vaga.estado()),
                escapar(vaga.modalidade()),
                escapar(vagaClassificada.area()),
                escapar(vagaClassificada.senioridade()),
                escapar(vaga.link())
        );
    }
}
