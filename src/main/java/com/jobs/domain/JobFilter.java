package com.jobs.domain;

import java.text.Normalizer;
import java.util.List;

public record JobFilter(List<String> areas, List<String> seniorities, List<String> regions, boolean remoteOnly,
        List<String> keywords) {

    public boolean matches(String area, String seniority, String city, String state, String workMode, String title) {
        boolean areaOk = areas.isEmpty() || areas.stream().anyMatch(a -> a.equalsIgnoreCase(area));
        boolean seniorityOk = seniorities.isEmpty()
                || seniorities.stream().anyMatch(s -> s.equalsIgnoreCase(seniority));
        boolean keywordOk = keywords.isEmpty() || keywords.stream().anyMatch(k -> normalize(title).contains(normalize(k)));
        return areaOk && seniorityOk && locationOk(city, state, workMode) && keywordOk;
    }

    // Região e "aceita remoto" não são dois requisitos que precisam bater ao mesmo tempo — são
    // alternativas ("Porto Alegre OU remoto"). Vaga remota normalmente não tem cidade/estado
    // preenchidos, então exigir os dois ao mesmo tempo nunca bateria em nenhuma vaga.
    private boolean locationOk(String city, String state, String workMode) {
        if (regions.isEmpty() && !remoteOnly) {
            return true;
        }
        boolean isRemote = workMode != null && normalize(workMode).contains("remote");
        if (remoteOnly && isRemote) {
            return true;
        }
        return !regions.isEmpty() && regions.stream().anyMatch(r -> matchesRegion(r, city, state));
    }

    private boolean matchesRegion(String region, String city, String state) {
        if (region.equalsIgnoreCase(state)) {
            return true;
        }
        return normalize(city).contains(normalize(region));
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase();
    }
}
