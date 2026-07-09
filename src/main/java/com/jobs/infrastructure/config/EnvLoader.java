package com.jobs.infrastructure.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EnvLoader {

    private EnvLoader() {
    }

    public static Map<String, String> load(Path file) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return values;
        }

        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String cleanLine = line.strip();
            if (cleanLine.isBlank() || cleanLine.startsWith("#")) {
                continue;
            }
            String[] parts = cleanLine.split("=", 2);
            if (parts.length != 2) {
                continue;
            }
            values.put(parts[0].strip(), parts[1].strip());
        }
        return values;
    }

    public static void save(Path file, Map<String, String> values) throws IOException {
        List<String> lines = values.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toList();
        Files.write(file, lines, StandardCharsets.UTF_8);
    }
}
