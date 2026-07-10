package com.jobs.infrastructure.linkedin;

import com.jobs.domain.Job;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Faz o parsing manual do HTML devolvido pelo endpoint público (não-oficial) de
 * busca de vagas do LinkedIn. Frágil por natureza (quebra se o LinkedIn mudar o
 * HTML) — os padrões foram calibrados contra uma resposta real do endpoint.
 */
public class LinkedInJobParser {

    private static final String CARD_SEPARATOR = "data-entity-urn=\"urn:li:jobPosting:";
    private static final Pattern ID = Pattern.compile("^(\\d+)\"");
    private static final Pattern TITLE = Pattern.compile("base-search-card__title\">\\s*([^<]+?)\\s*<");
    private static final Pattern COMPANY = Pattern.compile(
            "base-search-card__subtitle\">[\\s\\S]*?<a[^>]*>\\s*([^<]+?)\\s*</a>");
    private static final Pattern LOCATION = Pattern.compile("job-search-card__location\">\\s*([^<]+?)\\s*<");
    private static final Pattern LINK = Pattern.compile("href=\"([^\"?]+)");

    public List<Job> parse(String html, String workMode) {
        List<Job> jobs = new ArrayList<>();
        if (html == null || html.isBlank()) {
            return jobs;
        }

        String[] cards = html.split(java.util.regex.Pattern.quote(CARD_SEPARATOR));
        for (int i = 1; i < cards.length; i++) {
            Job job = parseCard(cards[i], workMode);
            if (job != null) {
                jobs.add(job);
            }
        }
        return jobs;
    }

    private Job parseCard(String card, String workMode) {
        Long id = extractLong(ID, card);
        String title = extractText(TITLE, card);
        String company = extractText(COMPANY, card);
        if (id == null || title == null || company == null) {
            return null;
        }

        String location = extractText(LOCATION, card);
        String link = extractText(LINK, card);

        return new Job(
                id,
                title,
                company,
                "",
                location != null ? location : "",
                "",
                workMode,
                link != null ? link : "https://www.linkedin.com/jobs/view/" + id);
    }

    private Long extractLong(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractText(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return unescapeHtml(matcher.group(1));
    }

    private String unescapeHtml(String text) {
        return text.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&#39;", "'")
                .replace("&quot;", "\"")
                .strip();
    }
}
