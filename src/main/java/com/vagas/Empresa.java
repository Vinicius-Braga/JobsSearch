package com.vagas;

public record Empresa(String nome, String subdominio) {

    public String homeUrl() {
        return "https://" + subdominio + ".gupy.io/";
    }

    public String dataUrl(String buildId) {
        return "https://" + subdominio + ".gupy.io/_next/data/" + buildId + "/pt.json";
    }

    public String jobUrl(long jobId) {
        return "https://" + subdominio + ".gupy.io/jobs/" + jobId + "?jobBoardSource=gupy_public_page";
    }
}
