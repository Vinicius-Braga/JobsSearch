package com.jobs.infrastructure.web;

import com.jobs.application.port.PaymentGateway;
import com.jobs.application.port.PaymentStore;
import com.jobs.application.port.SubscriptionStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentGateway paymentGateway;

    @MockBean
    private PaymentStore paymentStore;

    @MockBean
    private SubscriptionStore subscriptionStore;

    private final Principal principal = () -> "usuario";

    @Test
    void assinarCreatesPendingPaymentAndRedirectsToCheckout() throws Exception {
        when(paymentGateway.createCheckoutLink(anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn("https://checkout.infinitepay.io/abc");

        mockMvc.perform(get("/assinar").principal(principal))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("https://checkout.infinitepay.io/*"));

        verify(paymentStore).createPending(anyString(), eq("usuario"));
    }

    @Test
    void webhookRejectsUnknownOrderWithoutCallingGateway() throws Exception {
        when(paymentStore.findUsernameByOrderNsu("pedido-desconhecido")).thenReturn(Optional.empty());

        mockMvc.perform(post("/webhooks/infinitepay")
                        .contentType("application/json")
                        .content("{\"order_nsu\": \"pedido-desconhecido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(paymentGateway, never()).verifyPayment(anyString(), anyString(), anyString());
        verify(subscriptionStore, never()).upgradeToPlus(anyString());
    }

    @Test
    void webhookDoesNotUpgradePlanWhenPaymentIsNotVerified() throws Exception {
        when(paymentStore.findUsernameByOrderNsu("pedido-1")).thenReturn(Optional.of("usuario"));
        when(paymentGateway.verifyPayment("pedido-1", "tx-1", "slug-1")).thenReturn(false);

        String body = """
                {"order_nsu": "pedido-1", "transaction_nsu": "tx-1", "invoice_slug": "slug-1"}
                """;

        mockMvc.perform(post("/webhooks/infinitepay")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(subscriptionStore, never()).upgradeToPlus(anyString());
    }

    @Test
    void webhookUpgradesToPlusWhenPaymentIsVerified() throws Exception {
        when(paymentStore.findUsernameByOrderNsu("pedido-1")).thenReturn(Optional.of("usuario"));
        when(paymentGateway.verifyPayment("pedido-1", "tx-1", "slug-1")).thenReturn(true);

        String body = """
                {"order_nsu": "pedido-1", "transaction_nsu": "tx-1", "invoice_slug": "slug-1"}
                """;

        mockMvc.perform(post("/webhooks/infinitepay")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(paymentStore).markPaid("pedido-1", "tx-1");
        verify(subscriptionStore).upgradeToPlus("usuario");
    }
}
