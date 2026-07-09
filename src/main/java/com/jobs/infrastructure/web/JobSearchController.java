package com.jobs.infrastructure.web;

import com.jobs.application.SearchJobsUseCase;
import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.Company;
import com.jobs.domain.JobFilter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class JobSearchController {

    private final SearchJobsUseCase searchJobsUseCase;

    public JobSearchController(SearchJobsUseCase searchJobsUseCase) {
        this.searchJobsUseCase = searchJobsUseCase;
    }

    @PostMapping("/search")
    public List<ClassifiedJob> search(@RequestBody SearchRequest request) {
        List<Company> companies = request.empresas().stream()
                .map(dto -> new Company(dto.nome() != null ? dto.nome() : dto.subdominio(), dto.subdominio()))
                .toList();
        JobFilter filter = toFilter(request.filtro());
        return searchJobsUseCase.search(companies, filter);
    }

    private JobFilter toFilter(SearchRequest.FiltroDto dto) {
        if (dto == null) {
            return new JobFilter(List.of(), List.of(), List.of(), false);
        }
        return new JobFilter(
                dto.area() != null ? dto.area() : List.of(),
                dto.senioridade() != null ? dto.senioridade() : List.of(),
                dto.regiao() != null ? dto.regiao() : List.of(),
                dto.remoto() != null && dto.remoto());
    }
}
