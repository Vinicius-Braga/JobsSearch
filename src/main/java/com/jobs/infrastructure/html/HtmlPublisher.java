package com.jobs.infrastructure.html;

import com.jobs.application.port.JobPublisher;
import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.Job;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class HtmlPublisher implements JobPublisher {

    private final Path destination;

    public HtmlPublisher(Path destination) {
        this.destination = destination;
    }

    @Override
    public void publish(List<ClassifiedJob> jobs) throws IOException {
        StringBuilder rows = new StringBuilder();
        for (ClassifiedJob classifiedJob : jobs) {
            Job job = classifiedJob.job();
            rows.append("<tr>")
                    .append("<td><a href=\"").append(escape(job.link())).append("\" target=\"_blank\">")
                    .append(escape(job.title())).append("</a></td>")
                    .append("<td>").append(escape(job.company())).append("</td>")
                    .append("<td>").append(escape(job.department())).append("</td>")
                    .append("<td>").append(escape(job.city())).append("/").append(escape(job.state())).append("</td>")
                    .append("<td>").append(escape(job.workMode())).append("</td>")
                    .append("<td>").append(escape(classifiedJob.area())).append("</td>")
                    .append("<td>").append(escape(classifiedJob.seniority())).append("</td>")
                    .append("</tr>\n");
        }

        String html = HTML_TEMPLATE
                .replace("{{TOTAL}}", String.valueOf(jobs.size()))
                .replace("{{ROWS}}", rows.toString());

        Files.writeString(destination, html, StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static final String HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="pt-br">
            <head>
            <meta charset="UTF-8">
            <title>Vagas</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
                h1 { font-size: 20px; }
                #busca { padding: 8px; width: 100%; max-width: 400px; margin-bottom: 12px; box-sizing: border-box; }
                table { width: 100%; border-collapse: collapse; background: white; box-shadow: 0 1px 3px rgba(0,0,0,0.15); }
                th, td { padding: 8px 10px; border-bottom: 1px solid #ddd; text-align: left; font-size: 14px; }
                th { background: #2d2d2d; color: white; cursor: pointer; user-select: none; }
                th:hover { background: #444; }
                tr:hover { background: #f0f7ff; }
                a { color: #1a5fb4; text-decoration: none; }
                a:hover { text-decoration: underline; }
            </style>
            </head>
            <body>
            <h1>Vagas (<span id="contador">{{TOTAL}}</span> exibidas)</h1>
            <input type="text" id="busca" placeholder="Buscar por título, empresa, cidade...">
            <table id="tabela">
            <thead>
            <tr>
                <th onclick="ordenar(0)">Título</th>
                <th onclick="ordenar(1)">Empresa</th>
                <th onclick="ordenar(2)">Departamento</th>
                <th onclick="ordenar(3)">Cidade/UF</th>
                <th onclick="ordenar(4)">Modalidade</th>
                <th onclick="ordenar(5)">Área</th>
                <th onclick="ordenar(6)">Senioridade</th>
            </tr>
            </thead>
            <tbody>
            {{ROWS}}
            </tbody>
            </table>
            <script>
                const busca = document.getElementById('busca');
                const tabela = document.getElementById('tabela');
                const contador = document.getElementById('contador');
                const tbody = tabela.querySelector('tbody');

                busca.addEventListener('input', () => {
                    const termo = busca.value.toLowerCase();
                    let visiveis = 0;
                    for (const linha of tbody.rows) {
                        const bate = linha.textContent.toLowerCase().includes(termo);
                        linha.style.display = bate ? '' : 'none';
                        if (bate) visiveis++;
                    }
                    contador.textContent = visiveis;
                });

                let colunaAtual = -1;
                let ascendente = true;

                function ordenar(coluna) {
                    const linhas = Array.from(tbody.rows);
                    ascendente = (colunaAtual === coluna) ? !ascendente : true;
                    colunaAtual = coluna;

                    linhas.sort((a, b) => {
                        const va = a.cells[coluna].textContent.trim().toLowerCase();
                        const vb = b.cells[coluna].textContent.trim().toLowerCase();
                        return ascendente ? va.localeCompare(vb) : vb.localeCompare(va);
                    });

                    for (const linha of linhas) {
                        tbody.appendChild(linha);
                    }
                }
            </script>
            </body>
            </html>
            """;
}
