package com.jobs.infrastructure.csv;

import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.Job;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CsvExporter {

    private static final String[] HEADER = {
            "id", "titulo", "empresa", "departamento", "cidade", "estado", "modalidade", "link"
    };

    private static final String[] CLASSIFIED_HEADER = {
            "id", "titulo", "empresa", "departamento", "cidade", "estado", "modalidade",
            "area", "senioridade", "link"
    };

    private static final Pattern CSV_ROW = Pattern.compile(
            "^(\\d+),\"((?:[^\"]|\"\")*)\",\"((?:[^\"]|\"\")*)\",\"((?:[^\"]|\"\")*)\","
                    + "\"((?:[^\"]|\"\")*)\",\"((?:[^\"]|\"\")*)\",\"((?:[^\"]|\"\")*)\",\"((?:[^\"]|\"\")*)\"$");

    public List<Job> loadExisting(Path file) throws IOException {
        List<Job> jobs = new ArrayList<>();
        if (!Files.exists(file)) {
            return jobs;
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (String line : lines.subList(Math.min(1, lines.size()), lines.size())) {
            Matcher matcher = CSV_ROW.matcher(line);
            if (!matcher.matches()) {
                System.err.println("Aviso: linha do CSV ignorada por não bater com o formato esperado: " + line);
                continue;
            }
            jobs.add(new Job(
                    Long.parseLong(matcher.group(1)),
                    unescape(matcher.group(2)),
                    unescape(matcher.group(3)),
                    unescape(matcher.group(4)),
                    unescape(matcher.group(5)),
                    unescape(matcher.group(6)),
                    unescape(matcher.group(7)),
                    unescape(matcher.group(8))
            ));
        }
        return jobs;
    }

    private String unescape(String value) {
        return value.replace("\"\"", "\"");
    }

    public void export(List<Job> jobs, Path destination) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", HEADER)).append("\n");

        for (Job job : jobs) {
            csv.append(row(job)).append("\n");
        }

        Files.writeString(destination, csv.toString(), StandardCharsets.UTF_8);
    }

    private String row(Job job) {
        return String.join(",",
                String.valueOf(job.id()),
                escape(job.title()),
                escape(job.company()),
                escape(job.department()),
                escape(job.city()),
                escape(job.state()),
                escape(job.workMode()),
                escape(job.link())
        );
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    public void exportClassified(List<ClassifiedJob> jobs, Path destination) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", CLASSIFIED_HEADER)).append("\n");

        for (ClassifiedJob classifiedJob : jobs) {
            csv.append(classifiedRow(classifiedJob)).append("\n");
        }

        Files.writeString(destination, csv.toString(), StandardCharsets.UTF_8);
    }

    private String classifiedRow(ClassifiedJob classifiedJob) {
        Job job = classifiedJob.job();
        return String.join(",",
                String.valueOf(job.id()),
                escape(job.title()),
                escape(job.company()),
                escape(job.department()),
                escape(job.city()),
                escape(job.state()),
                escape(job.workMode()),
                escape(classifiedJob.area()),
                escape(classifiedJob.seniority()),
                escape(job.link())
        );
    }
}
