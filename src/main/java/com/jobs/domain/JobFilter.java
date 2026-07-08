package com.jobs.domain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

public record JobFilter(List<String> areas, List<String> seniorities, List<String> regions) {

    public static JobFilter load(Path file) throws IOException {
        if (!Files.exists(file)) {
            return new JobFilter(List.of(), List.of(), List.of());
        }

        List<String> areas = List.of();
        List<String> seniorities = List.of();
        List<String> regions = List.of();

        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String cleanLine = line.strip();
            if (cleanLine.isBlank() || cleanLine.startsWith("#")) {
                continue;
            }

            String[] parts = cleanLine.split("=", 2);
            if (parts.length != 2) {
                continue;
            }

            String key = parts[0].strip().toLowerCase();
            List<String> values = Arrays.stream(parts[1].split(","))
                    .map(String::strip)
                    .filter(value -> !value.isBlank())
                    .toList();

            if (key.equals("area")) {
                areas = values;
            } else if (key.equals("senioridade")) {
                seniorities = values;
            } else if (key.equals("regiao")) {
                regions = values;
            }
        }

        return new JobFilter(areas, seniorities, regions);
    }

    public boolean matches(String area, String seniority, String city, String state) {
        boolean areaOk = areas.isEmpty() || areas.stream().anyMatch(a -> a.equalsIgnoreCase(area));
        boolean seniorityOk = seniorities.isEmpty()
                || seniorities.stream().anyMatch(s -> s.equalsIgnoreCase(seniority));
        boolean regionOk = regions.isEmpty() || regions.stream().anyMatch(r -> matchesRegion(r, city, state));
        return areaOk && seniorityOk && regionOk;
    }

    private boolean matchesRegion(String region, String city, String state) {
        if (region.equalsIgnoreCase(state)) {
            return true;
        }
        return normalize(city).contains(normalize(region));
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase();
    }
}
