package com.jobs.application;

import com.jobs.application.port.JobSource;
import com.jobs.domain.Classifier;
import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.Company;
import com.jobs.domain.Job;
import com.jobs.domain.JobFilter;

import java.util.ArrayList;
import java.util.List;

public class SearchJobsUseCase {

    private final JobSource jobSource;
    private final Classifier classifier;

    public SearchJobsUseCase(JobSource jobSource, Classifier classifier) {
        this.jobSource = jobSource;
        this.classifier = classifier;
    }

    public List<ClassifiedJob> search(List<Company> companies, JobFilter filter) {
        List<ClassifiedJob> matched = new ArrayList<>();

        for (Company company : companies) {
            try {
                List<Job> jobs = jobSource.findJobs(company);
                for (Job job : jobs) {
                    String area = classifier.classifyArea(job);
                    String seniority = classifier.classifySeniority(job);
                    if (filter.matches(area, seniority, job.city(), job.state(), job.workMode())) {
                        matched.add(new ClassifiedJob(job, area, seniority));
                    }
                }
            } catch (Exception e) {
                System.out.println(company.name() + " -> ERRO: " + e.getMessage());
            }
        }

        return matched;
    }
}
