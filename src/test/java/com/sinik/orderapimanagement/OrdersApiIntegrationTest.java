package com.sinik.orderapimanagement;

import com.sinik.orderapimanagement.api.dto.CreateOrderRequest;
import com.sinik.orderapimanagement.repo.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import  com.jayway.jsonpath.JsonPath;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
class OrdersApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    tools.jackson.databind.ObjectMapper objectMapper;
    @Autowired
    private OrderRepository orderRepository;

    private static final String USER = "user";
    private static final String PASS = "password";

    @BeforeEach
    void cleanDb() {
        orderRepository.deleteAll();
    }

    @Test
    void authMissing_returns401() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAndGetOrder_byId_returns200() throws Exception {
        String id = createOrderAndGetId();

        mockMvc.perform(get("/orders/{id}", id)
                        .header("Authorization", basicAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // Adjust if your response uses a different field name (e.g. orderId)
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void getOrder_missing_returns404() throws Exception {
        String missingId = "00000000-0000-0000-0000-000000000000";

        mockMvc.perform(get("/orders/{id}", missingId)
                        .header("Authorization", basicAuthHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateStatus_validTransition_returns200() throws Exception {
        String id = createOrderAndGetId();

        mockMvc.perform(patch("/orders/{id}/status", id)
                        .header("Authorization", basicAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void updateStatus_invalidTransition_returns409() throws Exception {
        String id = createOrderAndGetId();

        // CREATED -> COMPLETED
        mockMvc.perform(patch("/orders/{id}/status", id)
                        .header("Authorization", basicAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());

        // COMPLETED -> CANCELLED (expected invalid example)
        mockMvc.perform(patch("/orders/{id}/status", id)
                        .header("Authorization", basicAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updateStatus_invalidEnum_returns400() throws Exception {
        String id = createOrderAndGetId();

        mockMvc.perform(patch("/orders/{id}/status", id)
                        .header("Authorization", basicAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"WRONG\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void search_byStatus_withPagination_returnsPage() throws Exception {
        // Arrange: create 3 CREATED orders
        createOrderAndGetId();
        createOrderAndGetId();
        createOrderAndGetId();

        // Act + Assert
        mockMvc.perform(get("/orders")
                        .header("Authorization", basicAuthHeader())
                        .param("status", "CREATED")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                // New stable page contract
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))

                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))

                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }


    // ---------------- Helpers ----------------

    private String basicAuthHeader() {
        String token = Base64.getEncoder()
                .encodeToString((USER + ":" + PASS).getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    private String createOrderAndGetId() throws Exception {
        var req = new CreateOrderRequest("cust-1");
        var result = mockMvc.perform(post("/orders")
                        .header("Authorization", basicAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }
}
