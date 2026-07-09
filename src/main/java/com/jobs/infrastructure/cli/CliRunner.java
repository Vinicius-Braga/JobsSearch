package com.jobs.infrastructure.cli;

import com.jobs.application.RunCycleUseCase;
import com.jobs.application.port.Notifier;
import com.jobs.infrastructure.notification.NoOpNotifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class CliRunner implements CommandLineRunner {

    private static final Duration CYCLE_INTERVAL = Duration.ofHours(6);

    private final RunCycleUseCase useCase;
    private final Notifier notifier;

    public CliRunner(RunCycleUseCase useCase, Notifier notifier) {
        this.useCase = useCase;
        this.notifier = notifier;
    }

    @Override
    public void run(String... args) throws Exception {
        boolean isFirstEverRun = !useCase.hasHistory();

        while (true) {
            System.out.println("=== Ciclo iniciado em " + LocalDateTime.now() + " ===");
            try {
                useCase.run(isFirstEverRun ? NoOpNotifier.INSTANCE : notifier);
                isFirstEverRun = false;
            } catch (Exception e) {
                System.out.println("Ciclo falhou: " + e.getMessage());
            }

            System.out.println("Próximo ciclo em " + CYCLE_INTERVAL.toMinutes() + " min. Aguardando...");
            Thread.sleep(CYCLE_INTERVAL.toMillis());
        }
    }
}
