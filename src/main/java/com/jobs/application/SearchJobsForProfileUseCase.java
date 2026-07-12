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

    public List<ClassifiedJob> search(List<Company> companies, UserProfile profile) {
        JobFilter filter = extractFilter(profile);
        return searchJobsUseCase.search(companies, filter);
    }

    private JobFilter extractFilter(UserProfile profile) {
        try {
            return searchCriteriaExtractor.extract(profile);
        } catch (Exception e) {
            log.warn("Falha ao extrair critérios de busca do perfil, buscando sem pré-filtro: {}", e.getMessage());
            return new JobFilter(List.of(), List.of(), List.of(), false, List.of());
        }
    }
}
