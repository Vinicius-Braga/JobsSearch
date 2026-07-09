package com.jobs.application.port;

import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.FitScore;
import com.jobs.domain.UserProfile;

public interface FitScorer {
    FitScore score(UserProfile profile, ClassifiedJob job) throws Exception;
}
