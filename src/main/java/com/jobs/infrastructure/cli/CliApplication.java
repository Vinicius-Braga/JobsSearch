package com.jobs.infrastructure.cli;

import com.jobs.infrastructure.config.TelegramProperties;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TelegramProperties.class)
public class CliApplication {

    public static void main(String[] args) {
        // O IPv4 do Telegram é bloqueado nesta rede; força IPv6, que conecta normalmente.
        System.setProperty("java.net.preferIPv6Addresses", "true");

        new SpringApplicationBuilder(CliApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
