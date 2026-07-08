package com.jobs.application.port;

import com.jobs.domain.ClassifiedJob;

import java.util.List;

public interface Notifier {
    void send(List<ClassifiedJob> newJobs);
}
