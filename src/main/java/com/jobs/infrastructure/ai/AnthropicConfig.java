package com.jobs.infrastructure.ai;

import java.util.Map;

public record AnthropicConfig(String apiKey) {

    public static AnthropicConfig from(Map<String, String> env) {
        String apiKey = env.get("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        return new AnthropicConfig(apiKey);
    }
}
