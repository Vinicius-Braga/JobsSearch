package com.jobs.infrastructure.linkedin;

import com.jobs.domain.Job;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedInJobParserTest {

    private final LinkedInJobParser parser = new LinkedInJobParser();

    // Estrutura real (simplificada) devolvida pelo endpoint jobs-guest em jul/2026 —
    // calibrada contra uma resposta de verdade antes de escrever o parser.
    private static final String SAMPLE_HTML = """
            <ul>
              <li>
                <div class="base-card relative w-full hover:no-underline focus:no-underline base-card--link base-search-card base-search-card--link job-search-card"
                     data-entity-urn="urn:li:jobPosting:4437461678" data-impression-id="jobs-search-result-0"
                     data-reference-id="abc==" data-tracking-id="def==" data-column="1" data-row="1">
                  <a class="base-card__full-link" href="https://www.linkedin.com/jobs/view/software-engineer-early-career-at-notion-4437461678?position=1&amp;pageNum=0"
                     data-tracking-control-name="public_jobs_jserp-result_search-card">
                    <span class="sr-only">Software Engineer, Early Career</span>
                  </a>
                  <div class="base-search-card__info">
                    <h3 class="base-search-card__title">
                      Software Engineer, Early Career
                    </h3>
                    <h4 class="base-search-card__subtitle">
                      <a class="hidden-nested-link" href="https://www.linkedin.com/company/notionhq">
                        Notion
                      </a>
                    </h4>
                    <div class="base-search-card__metadata">
                      <span class="job-search-card__location">
                        San Francisco, CA
                      </span>
                      <time class="job-search-card__listdate" datetime="2026-07-06">3 days ago</time>
                    </div>
                  </div>
                </div>
              </li>
              <li>
                <div class="base-card" data-entity-urn="urn:li:jobPosting:4437481325" data-column="1" data-row="2">
                  <a class="base-card__full-link" href="https://www.linkedin.com/jobs/view/software-engineer-i-at-uber-4437481325?position=2">
                    <span class="sr-only">Software Engineer I</span>
                  </a>
                  <div class="base-search-card__info">
                    <h3 class="base-search-card__title">
                      Software Engineer I
                    </h3>
                    <h4 class="base-search-card__subtitle">
                      <a class="hidden-nested-link" href="https://www.linkedin.com/company/uber">
                        Uber
                      </a>
                    </h4>
                    <div class="base-search-card__metadata">
                      <span class="job-search-card__location">
                        Seattle, WA
                      </span>
                    </div>
                  </div>
                </div>
              </li>
            </ul>
            """;

    @Test
    void parsesRealCardStructure() {
        List<Job> jobs = parser.parse(SAMPLE_HTML, "remote");

        assertEquals(2, jobs.size());

        Job first = jobs.get(0);
        assertEquals(4437461678L, first.id());
        assertEquals("Software Engineer, Early Career", first.title());
        assertEquals("Notion", first.company());
        assertEquals("San Francisco, CA", first.city());
        assertEquals("remote", first.workMode());
        assertTrue(first.link().startsWith("https://www.linkedin.com/jobs/view/"));
        assertTrue(first.link().indexOf('?') < 0, "link deveria ser limpo, sem parametros de tracking");

        Job second = jobs.get(1);
        assertEquals(4437481325L, second.id());
        assertEquals("Uber", second.company());
    }

    @Test
    void returnsEmptyListForBlankOrMalformedHtml() {
        assertEquals(0, parser.parse("", "remote").size());
        assertEquals(0, parser.parse(null, "remote").size());
        assertEquals(0, parser.parse("<html><body>sem vagas aqui</body></html>", "remote").size());
    }
}
