package com.jobs.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobs.application.port.PaymentGateway;
import com.jobs.application.port.PaymentStore;
import com.jobs.application.port.SubscriptionStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
public class PaymentController {

    private static final long PLUS_PRICE_CENTS = 500; // R$5,00 — mesmo valor de DashboardController.PLUS_PRICE
    private static final String PLUS_DESCRIPTION = "Assinatura JobSearch PLUS - 1 mes";

    private final PaymentGateway paymentGateway;
    private final PaymentStore paymentStore;
    private final SubscriptionStore subscriptionStore;
    private final String baseUrl;

    public PaymentController(PaymentGateway paymentGateway, PaymentStore paymentStore,
            SubscriptionStore subscriptionStore, @Value("${app.base-url}") String baseUrl) {
        this.paymentGateway = paymentGateway;
        this.paymentStore = paymentStore;
        this.subscriptionStore = subscriptionStore;
        this.baseUrl = baseUrl;
    }

    // Cria o link de checkout na InfinitePay e redireciona pra página de pagamento hospedada por eles.
    @GetMapping("/assinar")
    public String assinar(Principal principal) throws IOException, InterruptedException {
        String orderNsu = UUID.randomUUID().toString();
        paymentStore.createPending(orderNsu, principal.getName());

        String redirectUrl = baseUrl + "/?assinatura=processando";
        String webhookUrl = baseUrl + "/webhooks/infinitepay";
        String checkoutUrl = paymentGateway.createCheckoutLink(orderNsu, PLUS_PRICE_CENTS, PLUS_DESCRIPTION,
                redirectUrl, webhookUrl);

        return "redirect:" + checkoutUrl;
    }

    // Chamado pela InfinitePay quando um pagamento é concluído. Nunca confiamos só no corpo do
    // webhook — payment_check confirma o pagamento diretamente com a InfinitePay antes de liberar o plano.
    @PostMapping(value = "/webhooks/infinitepay", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> webhook(@RequestBody JsonNode payload) {
        String orderNsu = payload.path("order_nsu").asText("");
        String transactionNsu = payload.path("transaction_nsu").asText("");
        String slug = payload.path("invoice_slug").asText("");

        Optional<String> username = paymentStore.findUsernameByOrderNsu(orderNsu);
        if (username.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "pedido desconhecido"));
        }

        boolean verified;
        try {
            verified = paymentGateway.verifyPayment(orderNsu, transactionNsu, slug);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(502).body(Map.of("success", false, "message", "falha ao verificar pagamento"));
        } catch (IOException e) {
            return ResponseEntity.status(502).body(Map.of("success", false, "message", "falha ao verificar pagamento"));
        }

        if (!verified) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "pagamento nao confirmado"));
        }

        paymentStore.markPaid(orderNsu, transactionNsu);
        subscriptionStore.upgradeToPlus(username.get());

        return ResponseEntity.ok(Map.of("success", true, "message", ""));
    }
}
