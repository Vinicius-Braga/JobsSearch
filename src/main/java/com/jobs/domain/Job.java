package com.jobs.domain;

public record Job(
        long id,
        String title,
        String company,
        String department,
        String city,
        String state,
        String workMode,
        String link
) {
}
