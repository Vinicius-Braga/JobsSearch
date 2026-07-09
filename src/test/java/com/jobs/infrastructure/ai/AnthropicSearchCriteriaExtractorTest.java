package com.jobs.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobs.domain.JobFilter;
import com.jobs.domain.UserProfile;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnthropicSearchCriteriaExtractorTest {

    private final AnthropicSearchCriteriaExtractor extractor = new AnthropicSearchCriteriaExtractor(
            HttpClient.newHttpClient(), new ObjectMapper(), "fake-key");

    @Test
    void parsesFullCriteria() throws Exception {
        String responseBody = """
                {"content":[{"type":"text","text":"{\\"areas\\": [\\"TI\\"], \\"senioridades\\": [\\"Junior\\", \\"Pleno\\"], \\"regioes\\": [], \\"remoto\\": true}"}]}
                """;

        JobFilter result = extractor.parseResponse(responseBody);

        assertEquals(1, result.areas().size());
        assertEquals("TI", result.areas().get(0));
        assertEquals(2, result.seniorities().size());
        assertTrue(result.regions().isEmpty());
        assertTrue(result.remoteOnly());
    }

    @Test
    void parsesEmptyCriteriaWhenProfileIsVague() throws Exception {
        String responseBody = """
                {"content":[{"type":"text","text":"{\\"areas\\": [], \\"senioridades\\": [], \\"regioes\\": [], \\"remoto\\": false}"}]}
                """;

        JobFilter result = extractor.parseResponse(responseBody);

        assertTrue(result.areas().isEmpty());
        assertTrue(result.seniorities().isEmpty());
        assertTrue(result.regions().isEmpty());
        assertFalse(result.remoteOnly());
    }

    @Test
    void requestBodyContainsModelAndProfileAndValidCategories() throws Exception {
        UserProfile profile = new UserProfile("Backend Java, junior ou pleno, remoto.");

        String requestBody = extractor.buildRequestBody(profile);

        assertTrue(requestBody.contains("claude-haiku-4-5-20251001"));
        assertTrue(requestBody.contains("Backend Java, junior ou pleno, remoto."));
        assertTrue(requestBody.contains("TI"));
    }
}
