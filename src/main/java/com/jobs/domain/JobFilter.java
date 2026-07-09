package com.jobs.domain;

import java.text.Normalizer;
import java.util.List;

public record JobFilter(List<String> areas, List<String> seniorities, List<String> regions, boolean remoteOnly) {

    public boolean matches(String area, String seniority, String city, String state, String workMode) {
        boolean areaOk = areas.isEmpty() || areas.stream().anyMatch(a -> a.equalsIgnoreCase(area));
        boolean seniorityOk = seniorities.isEmpty()
                || seniorities.stream().anyMatch(s -> s.equalsIgnoreCase(seniority));
        boolean regionOk = regions.isEmpty() || regions.stream().anyMatch(r -> matchesRegion(r, city, state));
        boolean remoteOk = !remoteOnly || (workMode != null && normalize(workMode).contains("remote"));
        return areaOk && seniorityOk && regionOk && remoteOk;
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
