package com.vagas;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class Classificador {

    public String classificarArea(Vaga vaga) {
        String texto = normalizar(vaga.titulo() + " " + vaga.departamento());

        if (contem(texto, "rh", "recursos humanos", "recrutamento", "selecao", "departamento pessoal",
                "people", "gente e gestao", "employer branding")) {
            return "RH";
        }
        if (contem(texto, "tecnologia", "desenvolvedor", "programador", "software", "sistemas", "dados", "ti ")) {
            return "TI";
        }
        if (contem(texto, "comercial", "vendas", "vendedor")) {
            return "Comercial";
        }
        if (contem(texto, "financeiro", "contabil", "fiscal")) {
            return "Financeiro";
        }
        if (contem(texto, "marketing", "comunicacao")) {
            return "Marketing";
        }
        if (contem(texto, "logistica", "transporte", "frota")) {
            return "Logistica";
        }
        if (contem(texto, "juridico", "advogado", "compliance")) {
            return "Juridico";
        }
        if (contem(texto, "atendimento", "suporte", "cliente")) {
            return "Atendimento";
        }
        if (contem(texto, "engenharia", "engenheiro")) {
            return "Engenharia";
        }
        return "Outro";
    }

    public String classificarSenioridade(Vaga vaga) {
        String texto = normalizar(vaga.titulo());

        if (contem(texto, "estagio", "estagiario", "jovem aprendiz", "aprendiz")) {
            return "Estagio";
        }
        if (contem(texto, "auxiliar")) {
            return "Auxiliar";
        }
        if (contem(texto, "assistente")) {
            return "Assistente";
        }
        if (contemPalavraExata(texto, "junior", "jr")) {
            return "Junior";
        }
        if (contemPalavraExata(texto, "pleno", "pl")) {
            return "Pleno";
        }
        if (contemPalavraExata(texto, "senior", "sr")) {
            return "Senior";
        }
        return "Nao especificado";
    }

    private boolean contem(String texto, String... trechos) {
        for (String trecho : trechos) {
            if (texto.contains(trecho)) {
                return true;
            }
        }
        return false;
    }

    private boolean contemPalavraExata(String texto, String... palavras) {
        for (String palavra : palavras) {
            if (Pattern.compile("\\b" + Pattern.quote(palavra) + "\\b").matcher(texto).find()) {
                return true;
            }
        }
        return false;
    }

    private String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase();
    }
}
