package com.jobs.application;

import com.jobs.application.port.CompanyLoader;
import com.jobs.application.port.JobPublisher;
import com.jobs.application.port.JobRepository;
import com.jobs.application.port.JobSource;
import com.jobs.application.port.Notifier;
import com.jobs.domain.Classifier;
import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.Company;
import com.jobs.domain.Job;
import com.jobs.domain.JobFilter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RunCycleUseCase {

    private final CompanyLoader companyLoader;
    private final JobSource jobSource;
    private final JobRepository jobRepository;
    private final List<JobPublisher> publishers;
    private final Classifier classifier;
    private final Path filterFile;

    public RunCycleUseCase(CompanyLoader companyLoader, JobSource jobSource, JobRepository jobRepository,
            List<JobPublisher> publishers, Classifier classifier, Path filterFile) {
        this.companyLoader = companyLoader;
        this.jobSource = jobSource;
        this.jobRepository = jobRepository;
        this.publishers = publishers;
        this.classifier = classifier;
        this.filterFile = filterFile;
    }

    public boolean hasHistory() {
        return jobRepository.hasHistory();
    }

    public void run(Notifier notifier) throws IOException {
        List<Job> existingJobs = jobRepository.loadExisting();
        Set<Long> seenIds = new HashSet<>();
        for (Job job : existingJobs) {
            seenIds.add(job.id());
        }

        List<Company> companies = companyLoader.load();
        List<Job> newJobs = new ArrayList<>();

        for (Company company : companies) {
            try {
                List<Job> jobs = jobSource.findJobs(company);
                int newFromCompany = 0;
                for (Job job : jobs) {
                    if (seenIds.add(job.id())) {
                        newJobs.add(job);
                        newFromCompany++;
                    }
                }
                System.out.println(company.name() + " -> " + jobs.size() + " vaga(s), "
                        + newFromCompany + " nova(s)");
            } catch (Exception e) {
                System.out.println(company.name() + " -> ERRO: " + e.getMessage());
            }
        }

        List<Job> allJobs = new ArrayList<>(existingJobs);
        allJobs.addAll(newJobs);

        jobRepository.save(allJobs);
        System.out.println(newJobs.size() + " vaga(s) nova(s) adicionada(s). Total acumulado: "
                + allJobs.size() + " vaga(s).");

        classifyAndPublish(allJobs, newJobs, notifier);
    }

    private void classifyAndPublish(List<Job> allJobs, List<Job> newJobs, Notifier notifier) throws IOException {
        JobFilter filter = JobFilter.load(filterFile);

        List<ClassifiedJob> filtered = new ArrayList<>();
        List<ClassifiedJob> filteredNewJobs = new ArrayList<>();
        Set<Long> newIds = new HashSet<>();
        for (Job job : newJobs) {
            newIds.add(job.id());
        }

        for (Job job : allJobs) {
            String area = classifier.classifyArea(job);
            String seniority = classifier.classifySeniority(job);
            if (filter.matches(area, seniority, job.city(), job.state())) {
                ClassifiedJob classifiedJob = new ClassifiedJob(job, area, seniority);
                filtered.add(classifiedJob);
                if (newIds.contains(job.id())) {
                    filteredNewJobs.add(classifiedJob);
                }
            }
        }

        for (JobPublisher publisher : publishers) {
            publisher.publish(filtered);
        }
        System.out.println(filtered.size() + " vaga(s) apos filtro publicadas.");

        if (notifier != null) {
            notifier.send(filteredNewJobs);
            if (!filteredNewJobs.isEmpty()) {
                System.out.println(filteredNewJobs.size() + " vaga(s) nova(s) notificada(s).");
            }
        }
    }
}
