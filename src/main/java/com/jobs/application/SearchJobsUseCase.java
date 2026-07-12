package com.jobs.application;

import com.jobs.application.port.JobSource;
import com.jobs.application.port.LinkedInJobSource;
import com.jobs.domain.Classifier;
import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.Company;
import com.jobs.domain.Job;
import com.jobs.domain.JobFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchJobsUseCase {

    private static final Logger log = LoggerFactory.getLogger(SearchJobsUseCase.class);

    private final JobSource jobSource;
    private final LinkedInJobSource linkedInJobSource;
    private final Classifier classifier;

    public SearchJobsUseCase(JobSource jobSource, LinkedInJobSource linkedInJobSource, Classifier classifier) {
        this.jobSource = jobSource;
        this.linkedInJobSource = linkedInJobSource;
        this.classifier = classifier;
    }

    public List<ClassifiedJob> search(List<Company> companies, JobFilter filter) {
        List<ClassifiedJob> matched = Collections.synchronizedList(new ArrayList<>());

        // Cada empresa (Gupy) e a busca no LinkedIn são chamadas de rede independentes —
        // rodam em paralelo (virtual threads, baratas pra I/O bloqueante) em vez de esperar
        // uma terminar pra começar a próxima.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Company company : companies) {
                executor.submit(() -> searchCompany(company, filter, matched));
            }
            executor.submit(() -> searchLinkedIn(filter, matched));
        }

        return new ArrayList<>(matched);
    }

    private void searchCompany(Company company, JobFilter filter, List<ClassifiedJob> matched) {
        try {
            List<Job> jobs = jobSource.findJobs(company);
            classifyAndAdd(jobs, filter, matched);
        } catch (Exception e) {
            log.warn("{} -> ERRO: {}", company.name(), e.getMessage());
        }
    }

    private void searchLinkedIn(JobFilter filter, List<ClassifiedJob> matched) {
        try {
            List<Job> jobs = linkedInJobSource.search(filter);
            classifyAndAdd(jobs, filter, matched);
        } catch (Exception e) {
            log.warn("LinkedIn -> ERRO: {}", e.getMessage());
        }
    }

    private void classifyAndAdd(List<Job> jobs, JobFilter filter, List<ClassifiedJob> matched) {
        for (Job job : jobs) {
            String area = classifier.classifyArea(job);
            String seniority = classifier.classifySeniority(job);
            if (filter.matches(area, seniority, job.city(), job.state(), job.workMode(), job.title())) {
                matched.add(new ClassifiedJob(job, area, seniority));
            }
        }
    }
}
