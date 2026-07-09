package com.jobs.application;

import com.jobs.application.port.JobSource;
import com.jobs.domain.Classifier;
import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.Company;
import com.jobs.domain.Job;
import com.jobs.domain.JobFilter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchJobsUseCaseTest {

    @Test
    void returnsOnlyJobsMatchingFilter() {
        Company company = new Company("Vivo", "vivo");
        Job rhJunior = new Job(1L, "Analista de RH Junior", "Vivo", "RH", "Porto Alegre", "RS", "Hibrido", "url1");
        Job tiSenior = new Job(2L, "Desenvolvedor Senior", "Vivo", "Tecnologia", "Sao Paulo", "SP", "Remoto", "url2");

        JobSource fakeSource = fakeSourceFor(Map.of(company, List.of(rhJunior, tiSenior)));
        SearchJobsUseCase useCase = new SearchJobsUseCase(fakeSource, new Classifier());

        JobFilter filter = new JobFilter(List.of("RH"), List.of(), List.of(), false);
        List<ClassifiedJob> result = useCase.search(List.of(company), filter);

        assertEquals(1, result.size());
        assertEquals(rhJunior.id(), result.get(0).job().id());
        assertEquals("RH", result.get(0).area());
    }

    @Test
    void skipsCompanyThatFailsWithoutFailingWholeSearch() {
        Company broken = new Company("Broken", "broken");
        Company ok = new Company("Vivo", "vivo");
        Job job = new Job(1L, "Analista de RH Junior", "Vivo", "RH", "Porto Alegre", "RS", "Hibrido", "url1");

        JobSource fakeSource = company -> {
            if (company.equals(broken)) {
                throw new RuntimeException("falha simulada");
            }
            return List.of(job);
        };
        SearchJobsUseCase useCase = new SearchJobsUseCase(fakeSource, new Classifier());

        List<ClassifiedJob> result = useCase.search(List.of(broken, ok), new JobFilter(List.of(), List.of(), List.of(), false));

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(cj -> cj.job().id() == 1L));
    }

    private JobSource fakeSourceFor(Map<Company, List<Job>> jobsByCompany) {
        return company -> jobsByCompany.getOrDefault(company, List.of());
    }
}
