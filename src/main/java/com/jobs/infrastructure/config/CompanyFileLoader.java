package com.jobs.infrastructure.config;

import com.jobs.application.port.CompanyLoader;
import com.jobs.domain.Company;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CompanyFileLoader implements CompanyLoader {

    private final Path file;

    public CompanyFileLoader(Path file) {
        this.file = file;
    }

    @Override
    public List<Company> load() throws IOException {
        List<Company> companies = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split(",", 2);
            String subdomain = parts[0].strip();
            String name = parts.length > 1 ? parts[1].strip() : subdomain;
            companies.add(new Company(name, subdomain));
        }
        return companies;
    }
}
