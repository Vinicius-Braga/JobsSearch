package com.vagas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class TelegramNotifier {

    private static final int LIMITE_CARACTERES_POR_MENSAGEM = 3500;
    private static final int MAX_TENTATIVAS = 3;
    private static final Duration ESPERA_ENTRE_TENTATIVAS = Duration.ofSeconds(3);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String token;
    private final String chatId;

    public TelegramNotifier(HttpClient httpClient, ObjectMapper objectMapper, String token, String chatId) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.token = token;
        this.chatId = chatId;
    }

    public static String descobrirChatId(HttpClient httpClient, ObjectMapper objectMapper, String token)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.telegram.org/bot" + token + "/getUpdates"))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode resultados = root.path("result");

        if (!resultados.isArray() || resultados.isEmpty()) {
            throw new IllegalStateException(
                    "Nenhuma mensagem encontrada. Mande uma mensagem pro bot no Telegram antes de rodar isso.");
        }

        JsonNode ultimaMensagem = resultados.get(resultados.size() - 1);
        return ultimaMensagem.path("message").path("chat").path("id").asText();
    }

    public void notificarVagasNovas(List<VagaClassificada> vagasNovas) {
        if (vagasNovas.isEmpty()) {
            return;
        }

        StringBuilder lote = new StringBuilder();
        lote.append("📋 <b>").append(vagasNovas.size()).append(" vaga(s) nova(s)</b>\n\n");

        for (VagaClassificada vc : vagasNovas) {
            String bloco = formatarVaga(vc);

            if (lote.length() + bloco.length() > LIMITE_CARACTERES_POR_MENSAGEM) {
                enviarMensagem(lote.toString());
                lote = new StringBuilder();
            }
            lote.append(bloco);
        }

        if (!lote.isEmpty()) {
            enviarMensagem(lote.toString());
        }
    }

    private String formatarVaga(VagaClassificada vc) {
        Vaga vaga = vc.vaga();
        return "🆕 <a href=\"" + vaga.link() + "\"><b>" + escaparHtml(vaga.titulo()) + "</b></a>\n"
                + "🏢 " + escaparHtml(vaga.empresa())
                + "  ·  📍 " + escaparHtml(vaga.cidade()) + "/" + escaparHtml(vaga.estado())
                + "  ·  🎯 " + escaparHtml(vc.senioridade()) + "\n\n";
    }

    private String escaparHtml(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void enviarMensagem(String texto) {
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                if (tentarEnviar(texto)) {
                    return;
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Falha ao enviar notificação no Telegram (tentativa " + tentativa
                        + "/" + MAX_TENTATIVAS + "): " + e.getMessage());
            }

            if (tentativa < MAX_TENTATIVAS) {
                dormir(ESPERA_ENTRE_TENTATIVAS);
            }
        }
        System.out.println("Desisti de enviar a notificação no Telegram após " + MAX_TENTATIVAS + " tentativas.");
    }

    private boolean tentarEnviar(String texto) throws IOException, InterruptedException {
        String textoCodificado = URLEncoder.encode(texto, StandardCharsets.UTF_8);
        String url = "https://api.telegram.org/bot" + token + "/sendMessage"
                + "?chat_id=" + chatId
                + "&text=" + textoCodificado
                + "&parse_mode=HTML"
                + "&disable_web_page_preview=true";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.out.println("Falha ao enviar notificação no Telegram (status " + response.statusCode()
                    + "): " + response.body());
            return false;
        }
        return true;
    }

    private void dormir(Duration duracao) {
        try {
            Thread.sleep(duracao.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
