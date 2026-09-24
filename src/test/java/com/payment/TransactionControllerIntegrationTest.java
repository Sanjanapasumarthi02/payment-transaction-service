package com.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.dto.TransactionRequest;
import com.payment.dto.UserDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should verify pre-loaded users from data.sql")
    void testPreloadedUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$[0].username", is("alice")))
                .andExpect(jsonPath("$[0].balance", is(5000.0)))
                .andExpect(jsonPath("$[1].username", is("bob")))
                .andExpect(jsonPath("$[1].balance", is(5000.0)));
    }

    @Test
    @DisplayName("Should fetch user by ID")
    void testGetUserById() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("alice")))
                .andExpect(jsonPath("$.email", is("alice@example.com")))
                .andExpect(jsonPath("$.balance", is(5000.0)));
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void testGetUserNotFound() throws Exception {
        mockMvc.perform(get("/api/users/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should register new user with default 10,000 balance")
    void testRegisterUser() throws Exception {
        UserDTO newUser = new UserDTO("frank", "frank@example.com");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("frank")))
                .andExpect(jsonPath("$.email", is("frank@example.com")))
                .andExpect(jsonPath("$.balance", is(10000.0)));
    }

    @Test
    @DisplayName("Should complete payment transfer and query transaction history")
    void testTransferMoneyAndHistory() throws Exception {
        TransactionRequest request = new TransactionRequest(
                1L,
                2L,
                new BigDecimal("1000.00"),
                "Rent payment"
        );

        MvcResult result = mockMvc.perform(post("/api/transactions/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderId", is(1)))
                .andExpect(jsonPath("$.recipientId", is(2)))
                .andExpect(jsonPath("$.amount", is(1000.0)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        String transactionId = objectMapper.readTree(responseJson).get("id").asText();

        // Check transaction details endpoint
        mockMvc.perform(get("/api/transactions/" + transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(transactionId)))
                .andExpect(jsonPath("$.amount", is(1000.0)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        // Check user history endpoint
        mockMvc.perform(get("/api/transactions/history/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // Check status filter endpoint
        mockMvc.perform(get("/api/transactions/status/COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when balance is insufficient")
    void testInsufficientFundsTransfer() throws Exception {
        TransactionRequest request = new TransactionRequest(
                3L,
                4L,
                new BigDecimal("999999.00"),
                "Exorbitant transfer"
        );

        mockMvc.perform(post("/api/transactions/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("insufficient funds")));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when sender sends to themselves")
    void testSelfTransfer() throws Exception {
        TransactionRequest request = new TransactionRequest(
                3L,
                3L,
                new BigDecimal("100.00"),
                "Self transfer"
        );

        mockMvc.perform(post("/api/transactions/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Sender cannot send money to themselves")));
    }
}
