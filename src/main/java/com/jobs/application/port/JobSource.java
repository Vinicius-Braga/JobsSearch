package com.jobs.application.port;

import com.jobs.domain.Company;
import com.jobs.domain.Job;

import java.util.List;

public interface JobSource {
    List<Job> findJobs(Company company) throws Exception;
}
