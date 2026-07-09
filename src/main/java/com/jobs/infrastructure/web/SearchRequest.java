package com.jobs.infrastructure.web;

import java.util.List;

public record SearchRequest(List<CompanyDto> empresas, FiltroDto filtro) {

    public record CompanyDto(String subdominio, String nome) {
    }

    public record FiltroDto(List<String> area, List<String> senioridade, List<String> regiao, Boolean remoto) {
    }
}
