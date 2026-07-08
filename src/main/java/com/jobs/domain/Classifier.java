package com.jobs.domain;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class Classifier {

    public String classifyArea(Job job) {
        String text = normalize(job.title() + " " + job.department());

        if (contains(text, "rh", "recursos humanos", "recrutamento", "selecao", "departamento pessoal",
                "people", "gente e gestao", "employer branding")) {
            return "RH";
        }
        if (contains(text, "tecnologia", "desenvolvedor", "programador", "software", "sistemas", "dados", "ti ")) {
            return "TI";
        }
        if (contains(text, "comercial", "vendas", "vendedor")) {
            return "Comercial";
        }
        if (contains(text, "financeiro", "contabil", "fiscal")) {
            return "Financeiro";
        }
        if (contains(text, "marketing", "comunicacao")) {
            return "Marketing";
        }
        if (contains(text, "logistica", "transporte", "frota")) {
            return "Logistica";
        }
        if (contains(text, "juridico", "advogado", "compliance")) {
            return "Juridico";
        }
        if (contains(text, "atendimento", "suporte", "cliente")) {
            return "Atendimento";
        }
        if (contains(text, "engenharia", "engenheiro")) {
            return "Engenharia";
        }
        return "Outro";
    }

    public String classifySeniority(Job job) {
        String text = normalize(job.title());

        if (contains(text, "estagio", "estagiario", "jovem aprendiz", "aprendiz")) {
            return "Estagio";
        }
        if (contains(text, "auxiliar")) {
            return "Auxiliar";
        }
        if (contains(text, "assistente")) {
            return "Assistente";
        }
        if (containsExactWord(text, "junior", "jr")) {
            return "Junior";
        }
        if (containsExactWord(text, "pleno", "pl")) {
            return "Pleno";
        }
        if (containsExactWord(text, "senior", "sr")) {
            return "Senior";
        }
        return "Nao especificado";
    }

    private boolean contains(String text, String... snippets) {
        for (String snippet : snippets) {
            if (text.contains(snippet)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsExactWord(String text, String... words) {
        for (String word : words) {
            if (Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase();
    }
}
