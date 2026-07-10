package com.jobs.infrastructure.web;

import com.jobs.infrastructure.config.AnthropicProperties;
import com.jobs.infrastructure.config.InfinitePayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AnthropicProperties.class, InfinitePayProperties.class})
public class WebApplication {

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv6Addresses", "true");
        SpringApplication.run(WebApplication.class, args);
    }
}
