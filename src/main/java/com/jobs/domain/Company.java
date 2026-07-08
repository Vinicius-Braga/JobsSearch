package com.jobs.domain;

public record Company(String name, String subdomain) {

    public String homeUrl() {
        return "https://" + subdomain + ".gupy.io/";
    }

    public String dataUrl(String buildId) {
        return "https://" + subdomain + ".gupy.io/_next/data/" + buildId + "/pt.json";
    }

    public String jobUrl(long jobId) {
        return "https://" + subdomain + ".gupy.io/jobs/" + jobId + "?jobBoardSource=gupy_public_page";
    }
}
