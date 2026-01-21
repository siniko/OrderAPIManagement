package com.sinik.orderapimanagement;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "notification.enabled-channels=webhook",
        "notification.webhook.base-url=http://localhost:8089",
        "notification.webhook.path=/notify",
        "notification.retry.max-attempts=3",
        "notification.retry.initial-delay-ms=50"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderNotificationsTest {

    private static WireMockServer wireMock;

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();     // clears stubs + requests
        // OR if you want to keep stubs and only clear recorded requests:
        // wireMock.resetRequests();
    }

    /**
     * IMPORTANT:
     * DynamicPropertySource is executed while the Spring context is building,
     * BEFORE @BeforeAll. So we start WireMock from here (lazily + safely).
     */
    @DynamicPropertySource
    static void wiremockProps(DynamicPropertyRegistry registry) {
        ensureWireMockStarted();

        // This matches your config error: notification.webhook.base-url
        registry.add("notification.webhook.base-url",
                () -> "http://localhost:" + wireMock.port());

        // Optional (only if you want to override in tests; safe either way)
        // registry.add("notification.webhook.path", () -> "/notify");
    }

    private static synchronized void ensureWireMockStarted() {
        if (wireMock != null && wireMock.isRunning()) return;

        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @Test
    void createOrder_shouldCallNotificationService() throws Exception {
        ensureWireMockStarted();

        // No WireMock static imports => no collisions with MockMvc post(...)
        wireMock.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.post(
                        com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/notify")
                ).willReturn(
                        com.github.tomakehurst.wiremock.client.WireMock.aResponse().withStatus(200)
                )
        );

        mockMvc.perform(post("/orders")
                        .with(httpBasic("user", "password"))
                        .contentType("application/json")
                        .content("{\"customerId\":\"c123\"}"))
                .andExpect(status().isCreated());

        // Notification happens on transaction completion; wait a bit to avoid flakiness
        Awaitility.await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> wireMock.verify(
                        1,
                        com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(
                                com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/notify")
                        )
                ));
    }

    @Test
    void createOrder_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType("application/json")
                        .content("{\"customerId\":\"c123\"}"))
                .andExpect(status().isUnauthorized());
    }







    @Test
    void createOrder_missingCustomerId_shouldReturn400() throws Exception {
        mockMvc.perform(post("/orders")
                        .with(httpBasic("user","password"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_blankCustomerId_shouldReturn400() throws Exception {
        mockMvc.perform(post("/orders")
                        .with(httpBasic("user","password"))
                        .contentType("application/json")
                        .content("{\"customerId\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_shouldRetryWebhook_on5xxThenSucceed() throws Exception {
        ensureWireMockStarted();

        // 2 failures then success
        wireMock.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.post(
                                com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/notify"))
                        .inScenario("retry")
                        .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                        .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse().withStatus(500))
                        .willSetStateTo("second")
        );

        wireMock.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.post(
                                com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/notify"))
                        .inScenario("retry")
                        .whenScenarioStateIs("second")
                        .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse().withStatus(500))
                        .willSetStateTo("third")
        );

        wireMock.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.post(
                                com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/notify"))
                        .inScenario("retry")
                        .whenScenarioStateIs("third")
                        .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse().withStatus(200))
        );

        mockMvc.perform(post("/orders")
                        .with(httpBasic("user","password"))
                        .contentType("application/json")
                        .content("{\"customerId\":\"c123\"}"))
                .andExpect(status().isCreated());

        // Expect 3 attempts total
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> wireMock.verify(
                        3,
                        com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(
                                com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/notify"))
                ));
    }

    @Test
    void createOrder_webhookDown_shouldStillReturn201_butNoSuccessfulNotify() throws Exception {
        ensureWireMockStarted();

        // Always fail
        wireMock.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.post(
                                com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/notify"))
                        .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse().withStatus(500))
        );

        mockMvc.perform(post("/orders")
                        .with(httpBasic("user","password"))
                        .contentType("application/json")
                        .content("{\"customerId\":\"c123\"}"))
                .andExpect(status().isCreated());

        // It SHOULD have tried at least once (or multiple if retry enabled)
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> wireMock.verify(
                        com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly(1),
                        com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(
                                com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/notify"))
                ));
    }



}
