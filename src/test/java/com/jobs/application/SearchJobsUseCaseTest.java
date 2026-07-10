package com.jobs.application;

import com.jobs.application.port.JobSource;
import com.jobs.application.port.LinkedInJobSource;
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

    private static final LinkedInJobSource NO_LINKEDIN_RESULTS = filter -> List.of();

    @Test
    void returnsOnlyJobsMatchingFilter() {
        Company company = new Company("Vivo", "vivo");
        Job rhJunior = new Job(1L, "Analista de RH Junior", "Vivo", "RH", "Porto Alegre", "RS", "Hibrido", "url1");
        Job tiSenior = new Job(2L, "Desenvolvedor Senior", "Vivo", "Tecnologia", "Sao Paulo", "SP", "Remoto", "url2");

        JobSource fakeSource = fakeSourceFor(Map.of(company, List.of(rhJunior, tiSenior)));
        SearchJobsUseCase useCase = new SearchJobsUseCase(fakeSource, NO_LINKEDIN_RESULTS, new Classifier());

        JobFilter filter = new JobFilter(List.of("RH"), List.of(), List.of(), false, List.of());
        List<ClassifiedJob> result = useCase.search(List.of(company), filter);

        assertEquals(1, result.size());
        assertEquals(rhJunior.id(), result.get(0).job().id());
        assertEquals("RH", result.get(0).area());
    }

    @Test
    void filtersByKeywordInTitle() {
        Company company = new Company("Vivo", "vivo");
        Job javaJob = new Job(1L, "Desenvolvedor Java Pleno", "Vivo", "TI", "Sao Paulo", "SP", "remote", "url1");
        Job dotNetJob = new Job(2L, "Software Developer Pleno - .NET", "Vivo", "TI", "Sao Paulo", "SP", "remote", "url2");

        JobSource fakeSource = fakeSourceFor(Map.of(company, List.of(javaJob, dotNetJob)));
        SearchJobsUseCase useCase = new SearchJobsUseCase(fakeSource, NO_LINKEDIN_RESULTS, new Classifier());

        JobFilter filter = new JobFilter(List.of(), List.of(), List.of(), false, List.of("java"));
        List<ClassifiedJob> result = useCase.search(List.of(company), filter);

        assertEquals(1, result.size());
        assertEquals(javaJob.id(), result.get(0).job().id());
    }

    @Test
    void regionAndRemoteAreAlternativesNotBothRequired() {
        Company company = new Company("Vivo", "vivo");
        // Vaga remota real: cidade/estado normalmente vêm vazios da Gupy.
        Job remoteJob = new Job(1L, "Analista de RH Junior", "Vivo", "RH", "", "", "remote", "url1");
        Job poaOnSiteJob = new Job(2L, "Assistente de RH", "Vivo", "RH", "Porto Alegre", "RS", "on-site", "url2");
        Job saoPauloOnSiteJob = new Job(3L, "Auxiliar de RH", "Vivo", "RH", "Sao Paulo", "SP", "on-site", "url3");

        JobSource fakeSource = fakeSourceFor(Map.of(company, List.of(remoteJob, poaOnSiteJob, saoPauloOnSiteJob)));
        SearchJobsUseCase useCase = new SearchJobsUseCase(fakeSource, NO_LINKEDIN_RESULTS, new Classifier());

        // Perfil pediu "Porto Alegre ou remoto" -> regiao=["Porto Alegre"] e remoto=true.
        JobFilter filter = new JobFilter(List.of(), List.of(), List.of("Porto Alegre"), true, List.of());
        List<ClassifiedJob> result = useCase.search(List.of(company), filter);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(cj -> cj.job().id() == 1L), "vaga remota devia bater mesmo sem cidade");
        assertTrue(result.stream().anyMatch(cj -> cj.job().id() == 2L), "vaga presencial em Porto Alegre devia bater");
        assertTrue(result.stream().noneMatch(cj -> cj.job().id() == 3L), "vaga presencial fora de Porto Alegre nao devia bater");
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
        SearchJobsUseCase useCase = new SearchJobsUseCase(fakeSource, NO_LINKEDIN_RESULTS, new Classifier());

        List<ClassifiedJob> result = useCase.search(List.of(broken, ok),
                new JobFilter(List.of(), List.of(), List.of(), false, List.of()));

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(cj -> cj.job().id() == 1L));
    }

    @Test
    void mergesLinkedInResultsWithGupyResults() {
        Company company = new Company("Vivo", "vivo");
        Job gupyJob = new Job(1L, "Analista de RH Junior", "Vivo", "RH", "Porto Alegre", "RS", "Hibrido", "url1");
        Job linkedInJob = new Job(2L, "Assistente de RH", "Outra Empresa", "", "Porto Alegre", "RS", "on-site", "url2");

        JobSource fakeSource = fakeSourceFor(Map.of(company, List.of(gupyJob)));
        LinkedInJobSource fakeLinkedIn = filter -> List.of(linkedInJob);
        SearchJobsUseCase useCase = new SearchJobsUseCase(fakeSource, fakeLinkedIn, new Classifier());

        List<ClassifiedJob> result = useCase.search(List.of(company),
                new JobFilter(List.of(), List.of(), List.of(), false, List.of()));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(cj -> cj.job().id() == 1L));
        assertTrue(result.stream().anyMatch(cj -> cj.job().id() == 2L));
    }

    @Test
    void linkedInFailureDoesNotBreakGupyResults() {
        Company company = new Company("Vivo", "vivo");
        Job gupyJob = new Job(1L, "Analista de RH Junior", "Vivo", "RH", "Porto Alegre", "RS", "Hibrido", "url1");

        JobSource fakeSource = fakeSourceFor(Map.of(company, List.of(gupyJob)));
        LinkedInJobSource brokenLinkedIn = filter -> {
            throw new RuntimeException("LinkedIn indisponivel");
        };
        SearchJobsUseCase useCase = new SearchJobsUseCase(fakeSource, brokenLinkedIn, new Classifier());

        List<ClassifiedJob> result = useCase.search(List.of(company),
                new JobFilter(List.of(), List.of(), List.of(), false, List.of()));

        assertEquals(1, result.size());
        assertEquals(gupyJob.id(), result.get(0).job().id());
    }

    private JobSource fakeSourceFor(Map<Company, List<Job>> jobsByCompany) {
        return company -> jobsByCompany.getOrDefault(company, List.of());
    }
}
