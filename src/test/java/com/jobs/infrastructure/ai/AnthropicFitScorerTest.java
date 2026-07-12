package com.jobs.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.FitScore;
import com.jobs.domain.Job;
import com.jobs.domain.UserProfile;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnthropicFitScorerTest {

    private final AnthropicFitScorer scorer = new AnthropicFitScorer(
            HttpClient.newHttpClient(), new ObjectMapper(), "fake-key", "claude-haiku-4-5-20251001");

    @Test
    void parsesPlainJsonResponse() throws Exception {
        String responseBody = """
                {"content":[{"type":"text","text":"{\\"nota\\": 8, \\"justificativa\\": \\"Bom match de area e regiao\\"}"}]}
                """;

        FitScore result = scorer.parseResponse(responseBody);

        assertEquals(8, result.score());
        assertEquals("Bom match de area e regiao", result.justification());
    }

    @Test
    void parsesResponseWrappedInMarkdownFence() throws Exception {
        String responseBody = """
                {"content":[{"type":"text","text":"```json\\n{\\"nota\\": 3, \\"justificativa\\": \\"Area diferente do perfil\\"}\\n```"}]}
                """;

        FitScore result = scorer.parseResponse(responseBody);

        assertEquals(3, result.score());
        assertEquals("Area diferente do perfil", result.justification());
    }

    @Test
    void requestBodyContainsModelProfileAndJobData() throws Exception {
        UserProfile profile = new UserProfile("Procuro vagas de RH junior em Porto Alegre.");
        Job job = new Job(1L, "Analista de RH Junior", "Vivo", "RH", "Porto Alegre", "RS", "hybrid", "url1");
        ClassifiedJob classifiedJob = new ClassifiedJob(job, "RH", "Junior");

        String requestBody = scorer.buildRequestBody(profile, classifiedJob);

        assertTrue(requestBody.contains("claude-haiku-4-5-20251001"));
        assertTrue(requestBody.contains("Analista de RH Junior"));
        assertTrue(requestBody.contains("Procuro vagas de RH junior em Porto Alegre."));
    }
}
