package com.jobs.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobs.application.port.FitScorer;
import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.FitScore;
import com.jobs.domain.Job;
import com.jobs.domain.UserProfile;
import com.jobs.infrastructure.config.AnthropicProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * Aplicação Spring separada (não faz parte do fluxo real nem dos testes
 * automatizados) pra validar visualmente se as notas do FitScorer fazem
 * sentido antes de confiar na integração. Requer ANTHROPIC_API_KEY no .env.
 * Rodar com: ./gradlew.bat fitScorerPlayground
 */
@SpringBootApplication
@EnableConfigurationProperties(AnthropicProperties.class)
public class FitScorerPlaygroundApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(FitScorerPlaygroundApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Bean
    public FitScorer fitScorer(HttpClient httpClient, ObjectMapper objectMapper, AnthropicProperties anthropicProperties) {
        return new AnthropicFitScorer(httpClient, objectMapper, anthropicProperties.apiKey(), anthropicProperties.model());
    }
}

@Component
class FitScorerPlaygroundRunner implements CommandLineRunner {

    private final FitScorer fitScorer;
    private final AnthropicProperties anthropicProperties;

    FitScorerPlaygroundRunner(FitScorer fitScorer, AnthropicProperties anthropicProperties) {
        this.fitScorer = fitScorer;
        this.anthropicProperties = anthropicProperties;
    }

    @Override
    public void run(String... args) throws Exception {
        if (anthropicProperties.apiKey() == null || anthropicProperties.apiKey().isBlank()) {
            System.out.println(".env não tem ANTHROPIC_API_KEY configurado. "
                    + "Adicione a linha: ANTHROPIC_API_KEY=SUA_CHAVE_AQUI");
            return;
        }

        UserProfile profile = new UserProfile(
                "Procuro vagas de RH, junior ou estagio, em Porto Alegre ou remoto. "
                        + "Prefiro empresas de medio porte.");

        List<ClassifiedJob> sampleJobs = List.of(
                classifiedJob(1, "Analista de RH Junior", "Localiza", "Recursos Humanos",
                        "Porto Alegre", "RS", "hybrid", "RH", "Junior"),
                classifiedJob(2, "Desenvolvedor Senior Java", "Stone", "Tecnologia",
                        "Sao Paulo", "SP", "remote", "TI", "Senior"),
                classifiedJob(3, "Estagiario de RH", "Lojas Renner", "Recursos Humanos",
                        "Porto Alegre", "RS", "hybrid", "RH", "Estagio"),
                classifiedJob(4, "Assistente Administrativo", "Vivo", "Administrativo",
                        "Curitiba", "PR", "onsite", "Outro", "Assistente")
        );

        System.out.println("Perfil: " + profile.description());
        System.out.println();

        for (ClassifiedJob job : sampleJobs) {
            FitScore result = fitScorer.score(profile, job);
            System.out.println(job.job().title() + " (" + job.job().company() + ") -> nota "
                    + result.score() + "/10: " + result.justification());
        }
    }

    private static ClassifiedJob classifiedJob(long id, String title, String company, String department,
            String city, String state, String workMode, String area, String seniority) {
        Job job = new Job(id, title, company, department, city, state, workMode,
                "https://example.com/jobs/" + id);
        return new ClassifiedJob(job, area, seniority);
    }
}
