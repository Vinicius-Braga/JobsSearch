package com.jobs.application;

import com.jobs.application.port.SearchCriteriaExtractor;
import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.Company;
import com.jobs.domain.JobFilter;
import com.jobs.domain.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Busca vagas usando o pré-filtro (área/senioridade/região/remoto) que a IA extrai
 * do perfil, sem pontuar cada vaga individualmente. A pontuação por IA
 * ({@link SearchAndScoreJobsUseCase}) fica pronta pra religar, mas não é chamada
 * daqui — decisão de custo/simplicidade tomada por enquanto.
 */
public class SearchJobsForProfileUseCase {

    private static final Logger log = LoggerFactory.getLogger(SearchJobsForProfileUseCase.class);

    private final SearchJobsUseCase searchJobsUseCase;
    private final SearchCriteriaExtractor searchCriteriaExtractor;

    public SearchJobsForProfileUseCase(SearchJobsUseCase searchJobsUseCase,
            SearchCriteriaExtractor searchCriteriaExtractor) {
        this.searchJobsUseCase = searchJobsUseCase;
        this.searchCriteriaExtractor = searchCriteriaExtractor;
    }

    public Result search(List<Company> companies, UserProfile profile) {
        boolean filterExtracted = true;
        JobFilter filter;
        try {
            filter = searchCriteriaExtractor.extract(profile);
        } catch (Exception e) {
            log.warn("Falha ao extrair critérios de busca do perfil, buscando sem pré-filtro: {}", e.getMessage());
            filter = new JobFilter(List.of(), List.of(), List.of(), false, List.of());
            filterExtracted = false;
        }

        List<ClassifiedJob> jobs = searchJobsUseCase.search(companies, filter);
        return new Result(jobs, filterExtracted);
    }

    // filterExtracted=false significa que a IA falhou e a busca voltou SEM pré-filtro nenhum
    // (todas as vagas encontradas, de qualquer área/senioridade/região) — quem chama deve avisar
    // a pessoa disso, já que o resultado não reflete o perfil dela.
    public record Result(List<ClassifiedJob> jobs, boolean filterExtracted) {
    }
}
