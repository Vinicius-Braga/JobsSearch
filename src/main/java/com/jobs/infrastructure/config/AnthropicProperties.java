package com.jobs.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.anthropic")
public record AnthropicProperties(String apiKey, String model) {
}
