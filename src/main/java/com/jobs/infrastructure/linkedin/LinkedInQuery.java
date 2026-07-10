package com.jobs.infrastructure.linkedin;

// fWt: null = qualquer modalidade, "2" = remoto (código usado pelo próprio LinkedIn).
// workModeLabel: o que gravamos em Job.workMode() pros resultados dessa query —
// só sabemos com certeza quando a própria busca já foi restrita a remoto.
record LinkedInQuery(String keywords, String location, String fWt, String workModeLabel) {
}
