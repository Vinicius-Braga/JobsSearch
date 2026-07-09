package com.jobs.application;

import com.jobs.application.port.FitScorer;
import com.jobs.application.port.SearchCriteriaExtractor;
import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.Company;
import com.jobs.domain.FitScore;
import com.jobs.domain.JobFilter;
import com.jobs.domain.ScoredJob;
import com.jobs.domain.UserProfile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SearchAndScoreJobsUseCase {

    // Limite defensivo de custo: cada vaga pontuada é uma chamada à API da Claude.
    private static final int MAX_JOBS_TO_SCORE = 40;

    private final SearchJobsUseCase searchJobsUseCase;
    private final FitScorer fitScorer;
    private final SearchCriteriaExtractor searchCriteriaExtractor;

    public SearchAndScoreJobsUseCase(SearchJobsUseCase searchJobsUseCase, FitScorer fitScorer,
            SearchCriteriaExtractor searchCriteriaExtractor) {
        this.searchJobsUseCase = searchJobsUseCase;
        this.fitScorer = fitScorer;
        this.searchCriteriaExtractor = searchCriteriaExtractor;
    }

    public SearchOutcome search(List<Company> companies, UserProfile profile) {
        JobFilter filter = extractFilter(profile);

        List<ClassifiedJob> matched = searchJobsUseCase.search(companies, filter);
        List<ClassifiedJob> toScore = matched.size() > MAX_JOBS_TO_SCORE
                ? matched.subList(0, MAX_JOBS_TO_SCORE)
                : matched;

        List<ScoredJob> scored = new ArrayList<>();
        for (ClassifiedJob job : toScore) {
            try {
                FitScore fitScore = fitScorer.score(profile, job);
                scored.add(new ScoredJob(job, fitScore));
            } catch (Exception e) {
                System.out.println("Falha ao pontuar vaga \"" + job.job().title() + "\": " + e.getMessage());
            }
        }

        scored.sort(Comparator.comparingInt((ScoredJob s) -> s.fitScore().score()).reversed());
        return new SearchOutcome(matched.size(), scored);
    }

    private JobFilter extractFilter(UserProfile profile) {
        try {
            return searchCriteriaExtractor.extract(profile);
        } catch (Exception e) {
            System.out.println("Falha ao extrair critérios de busca do perfil, buscando sem pré-filtro: "
                    + e.getMessage());
            return new JobFilter(List.of(), List.of(), List.of(), false);
        }
    }
}
