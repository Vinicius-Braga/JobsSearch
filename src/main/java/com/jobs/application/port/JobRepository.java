package com.jobs.application.port;

import com.jobs.domain.Job;

import java.io.IOException;
import java.util.List;

public interface JobRepository {
    boolean hasHistory();

    List<Job> loadExisting() throws IOException;

    void save(List<Job> jobs) throws IOException;
}
