package com.jobs.application;

import com.jobs.application.port.JobSource;
import com.jobs.domain.Classifier;
import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.Company;
import com.jobs.domain.Job;
import com.jobs.domain.JobFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchJobsUseCase {

    private final JobSource jobSource;
    private final Classifier classifier;

    public SearchJobsUseCase(JobSource jobSource, Classifier classifier) {
        this.jobSource = jobSource;
        this.classifier = classifier;
    }

    public List<ClassifiedJob> search(List<Company> companies, JobFilter filter) {
        List<ClassifiedJob> matched = Collections.synchronizedList(new ArrayList<>());

        // Cada empresa é uma chamada de rede independente — roda em paralelo (virtual threads,
        // baratas pra I/O bloqueante) em vez de esperar uma empresa terminar pra começar a próxima.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Company company : companies) {
                executor.submit(() -> searchCompany(company, filter, matched));
            }
        }

        return new ArrayList<>(matched);
    }

    private void searchCompany(Company company, JobFilter filter, List<ClassifiedJob> matched) {
        try {
            List<Job> jobs = jobSource.findJobs(company);
            for (Job job : jobs) {
                String area = classifier.classifyArea(job);
                String seniority = classifier.classifySeniority(job);
                if (filter.matches(area, seniority, job.city(), job.state(), job.workMode(), job.title())) {
                    matched.add(new ClassifiedJob(job, area, seniority));
                }
            }
        } catch (Exception e) {
            System.out.println(company.name() + " -> ERRO: " + e.getMessage());
        }
    }
}
