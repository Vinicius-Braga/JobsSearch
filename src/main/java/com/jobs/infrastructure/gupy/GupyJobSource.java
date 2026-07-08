package com.jobs.infrastructure.gupy;

import com.jobs.application.port.JobSource;
import com.jobs.domain.Company;
import com.jobs.domain.Job;

import java.util.List;

public class GupyJobSource implements JobSource {

    private final BuildIdExtractor buildIdExtractor;
    private final GupyClient gupyClient;

    public GupyJobSource(BuildIdExtractor buildIdExtractor, GupyClient gupyClient) {
        this.buildIdExtractor = buildIdExtractor;
        this.gupyClient = gupyClient;
    }

    @Override
    public List<Job> findJobs(Company company) throws Exception {
        String buildId = buildIdExtractor.extract(company);
        return gupyClient.findJobs(company, buildId);
    }
}
