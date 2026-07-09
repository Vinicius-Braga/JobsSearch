package com.jobs.infrastructure.notification;

import com.jobs.application.port.Notifier;
import com.jobs.domain.ClassifiedJob;

import java.util.List;

public class NoOpNotifier implements Notifier {

    public static final NoOpNotifier INSTANCE = new NoOpNotifier();

    @Override
    public void send(List<ClassifiedJob> newJobs) {
        // Telegram não configurado (ou primeiro ciclo, pra não notificar o histórico inteiro de uma vez).
    }
}
