package com.vagas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

public record FiltroVagas(List<String> areas, List<String> senioridades, List<String> regioes) {

    public static FiltroVagas carregar(Path arquivo) throws IOException {
        if (!Files.exists(arquivo)) {
            return new FiltroVagas(List.of(), List.of(), List.of());
        }

        List<String> areas = List.of();
        List<String> senioridades = List.of();
        List<String> regioes = List.of();

        for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
            String linhaLimpa = linha.strip();
            if (linhaLimpa.isBlank() || linhaLimpa.startsWith("#")) {
                continue;
            }

            String[] partes = linhaLimpa.split("=", 2);
            if (partes.length != 2) {
                continue;
            }

            String chave = partes[0].strip().toLowerCase();
            List<String> valores = Arrays.stream(partes[1].split(","))
                    .map(String::strip)
                    .filter(valor -> !valor.isBlank())
                    .toList();

            if (chave.equals("area")) {
                areas = valores;
            } else if (chave.equals("senioridade")) {
                senioridades = valores;
            } else if (chave.equals("regiao")) {
                regioes = valores;
            }
        }

        return new FiltroVagas(areas, senioridades, regioes);
    }

    public boolean aceita(String area, String senioridade, String cidade, String estado) {
        boolean areaOk = areas.isEmpty() || areas.stream().anyMatch(a -> a.equalsIgnoreCase(area));
        boolean senioridadeOk = senioridades.isEmpty()
                || senioridades.stream().anyMatch(s -> s.equalsIgnoreCase(senioridade));
        boolean regiaoOk = regioes.isEmpty() || regioes.stream().anyMatch(r -> combinaComRegiao(r, cidade, estado));
        return areaOk && senioridadeOk && regiaoOk;
    }

    private boolean combinaComRegiao(String regiao, String cidade, String estado) {
        if (regiao.equalsIgnoreCase(estado)) {
            return true;
        }
        return normalizar(cidade).contains(normalizar(regiao));
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase();
    }
}
