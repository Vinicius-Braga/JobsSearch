package com.jobs.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.FitScore;
import com.jobs.domain.Job;
import com.jobs.domain.UserProfile;
import com.jobs.infrastructure.config.EnvLoader;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Programa manual (não faz parte do fluxo real nem dos testes automatizados)
 * pra validar visualmente se as notas do FitScorer fazem sentido antes de
 * confiar na integração. Requer ANTHROPIC_API_KEY configurado no .env.
 * Rodar com: ./gradlew.bat fitScorerPlayground
 */
public class FitScorerPlayground {

    public static void main(String[] args) throws Exception {
        Map<String, String> env = EnvLoader.load(Path.of(".env"));
        AnthropicConfig config = AnthropicConfig.from(env);
        if (config == null) {
            System.out.println(".env não tem ANTHROPIC_API_KEY configurado. "
                    + "Adicione a linha: ANTHROPIC_API_KEY=SUA_CHAVE_AQUI");
            return;
        }

        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        AnthropicFitScorer scorer = new AnthropicFitScorer(httpClient, new ObjectMapper(), config.apiKey());

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
            FitScore result = scorer.score(profile, job);
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
