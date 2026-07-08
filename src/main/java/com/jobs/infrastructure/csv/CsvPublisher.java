package com.jobs.infrastructure.csv;

import com.jobs.application.port.JobPublisher;
import com.jobs.domain.ClassifiedJob;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class CsvPublisher implements JobPublisher {

    private final CsvExporter csvExporter;
    private final Path destination;

    public CsvPublisher(CsvExporter csvExporter, Path destination) {
        this.csvExporter = csvExporter;
        this.destination = destination;
    }

    @Override
    public void publish(List<ClassifiedJob> jobs) throws IOException {
        csvExporter.exportClassified(jobs, destination);
    }
}
