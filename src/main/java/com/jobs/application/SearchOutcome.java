package com.jobs.application;

import com.jobs.domain.ScoredJob;

import java.util.List;

public record SearchOutcome(int matchedCount, List<ScoredJob> scored) {

    public boolean hasUnscoredMatches() {
        return matchedCount > scored.size();
    }
}
