package com.jobs.infrastructure.web;

import com.jobs.domain.ClassifiedJob;

import java.util.List;

public record BuscarResponse(List<ClassifiedJob> results, int lockedCount, String aviso) {
}
