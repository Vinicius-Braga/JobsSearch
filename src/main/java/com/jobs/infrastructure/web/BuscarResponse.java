package com.jobs.infrastructure.web;

import com.jobs.domain.ScoredJob;

import java.util.List;

public record BuscarResponse(int matchedCount, List<ScoredJob> results, String aviso) {
}
