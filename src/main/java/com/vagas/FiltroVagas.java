package com.vagas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public record FiltroVagas(List<String> areas, List<String> senioridades) {

    public static FiltroVagas carregar(Path arquivo) throws IOException {
        if (!Files.exists(arquivo)) {
            return new FiltroVagas(List.of(), List.of());
        }

        List<String> areas = List.of();
        List<String> senioridades = List.of();

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
            }
        }

        return new FiltroVagas(areas, senioridades);
    }

    public boolean aceita(String area, String senioridade) {
        boolean areaOk = areas.isEmpty() || areas.stream().anyMatch(a -> a.equalsIgnoreCase(area));
        boolean senioridadeOk = senioridades.isEmpty()
                || senioridades.stream().anyMatch(s -> s.equalsIgnoreCase(senioridade));
        return areaOk && senioridadeOk;
    }
}
