package com.jobs.application.port;

import com.jobs.domain.ClassifiedJob;

import java.io.IOException;
import java.util.List;

public interface JobPublisher {
    void publish(List<ClassifiedJob> jobs) throws IOException;
}
