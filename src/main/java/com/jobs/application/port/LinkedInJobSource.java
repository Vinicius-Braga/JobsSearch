package com.jobs.application.port;

import com.jobs.domain.Job;
import com.jobs.domain.JobFilter;

import java.util.List;

public interface LinkedInJobSource {
    List<Job> search(JobFilter filter) throws Exception;
}
