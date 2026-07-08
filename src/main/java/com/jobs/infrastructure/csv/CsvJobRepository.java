package com.jobs.infrastructure.csv;

import com.jobs.application.port.JobRepository;
import com.jobs.domain.Job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CsvJobRepository implements JobRepository {

    private final CsvExporter csvExporter;
    private final Path file;

    public CsvJobRepository(CsvExporter csvExporter, Path file) {
        this.csvExporter = csvExporter;
        this.file = file;
    }

    @Override
    public boolean hasHistory() {
        return Files.exists(file);
    }

    @Override
    public List<Job> loadExisting() throws IOException {
        return csvExporter.loadExisting(file);
    }

    @Override
    public void save(List<Job> jobs) throws IOException {
        csvExporter.export(jobs, file);
    }
}
