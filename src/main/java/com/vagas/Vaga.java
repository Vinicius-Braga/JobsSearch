package com.vagas;

public record Vaga(
        long id,
        String titulo,
        String empresa,
        String departamento,
        String cidade,
        String estado,
        String modalidade,
        String link
) {
}
