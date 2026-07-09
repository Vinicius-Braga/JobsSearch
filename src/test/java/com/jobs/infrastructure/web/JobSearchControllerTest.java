package com.jobs.infrastructure.web;

import com.jobs.application.SearchJobsUseCase;
import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.Company;
import com.jobs.domain.Job;
import com.jobs.domain.JobFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Security fica fora do escopo deste teste (que valida serialização/roteamento do controller);
// /api/** é permitAll e sem CSRF por design em SecurityConfig, testado manualmente via curl.
@WebMvcTest(JobSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class JobSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchJobsUseCase searchJobsUseCase;

    @Test
    void searchReturnsMatchedJobsAsJson() throws Exception {
        Job job = new Job(1L, "Analista de RH Junior", "Vivo", "RH", "Porto Alegre", "RS", "Hibrido", "url1");
        ClassifiedJob classifiedJob = new ClassifiedJob(job, "RH", "Junior");
        when(searchJobsUseCase.search(any(), any())).thenReturn(List.of(classifiedJob));

        String body = """
                {
                  "empresas": [{"subdominio": "vivo", "nome": "Vivo"}],
                  "filtro": {"area": ["RH"], "senioridade": [], "regiao": []}
                }
                """;

        mockMvc.perform(post("/api/search")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].job.title").value("Analista de RH Junior"))
                .andExpect(jsonPath("$[0].area").value("RH"));
    }

    @Test
    void searchWithoutFiltroUsesEmptyFilter() throws Exception {
        when(searchJobsUseCase.search(any(), any())).thenReturn(List.of());

        String body = """
                {
                  "empresas": [{"subdominio": "vivo", "nome": "Vivo"}]
                }
                """;

        mockMvc.perform(post("/api/search")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
