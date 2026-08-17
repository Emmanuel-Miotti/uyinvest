package com.uyinvest.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uyinvest.AbstractIntegrationTest;
import com.uyinvest.dto.request.GoalRequest;
import com.uyinvest.dto.request.RegisterRequest;
import com.uyinvest.dto.response.AuthResponse;
import com.uyinvest.dto.response.GoalResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class GoalControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String email) throws Exception {
        RegisterRequest request = new RegisterRequest("Test User", email, "password123");
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(body, AuthResponse.class).accessToken();
    }

    private GoalResponse createGoal(String token, String name, String target, String current) throws Exception {
        GoalRequest request = new GoalRequest(name, new BigDecimal(target), new BigDecimal(current), "USD", null);
        String body = mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(body, GoalResponse.class);
    }

    @Test
    void createsAndListsGoalsWithComputedProgress() throws Exception {
        String token = registerAndGetToken("goal1@example.com");

        createGoal(token, "Comprar auto", "20000", "12500");

        mockMvc.perform(get("/api/v1/goals").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].progressPercentage").value(62.50));
    }

    @Test
    void updatesOwnGoal() throws Exception {
        String token = registerAndGetToken("goal2@example.com");
        GoalResponse created = createGoal(token, "Vacaciones", "3000", "0");

        GoalRequest updateRequest = new GoalRequest("Vacaciones en Europa", new BigDecimal("4000"), new BigDecimal("1000"), "EUR", null);

        mockMvc.perform(put("/api/v1/goals/" + created.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Vacaciones en Europa"))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void deletesOwnGoal() throws Exception {
        String token = registerAndGetToken("goal3@example.com");
        GoalResponse created = createGoal(token, "Fondo de emergencia", "5000", "0");

        mockMvc.perform(delete("/api/v1/goals/" + created.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/goals/" + created.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotAccessAnotherUsersGoal() throws Exception {
        String ownerToken = registerAndGetToken("goalowner@example.com");
        GoalResponse created = createGoal(ownerToken, "Meta privada", "1000", "0");

        String intruderToken = registerAndGetToken("goalintruder@example.com");

        mockMvc.perform(get("/api/v1/goals/" + created.id())
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/goals/" + created.id())
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidTargetAmount() throws Exception {
        String token = registerAndGetToken("goal4@example.com");
        GoalRequest invalidRequest = new GoalRequest("Meta invalida", BigDecimal.ZERO, null, "USD", null);

        mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/goals"))
                .andExpect(status().isUnauthorized());
    }
}
