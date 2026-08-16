package com.uyinvest.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uyinvest.AbstractIntegrationTest;
import com.uyinvest.dto.request.PortfolioRequest;
import com.uyinvest.dto.request.RegisterRequest;
import com.uyinvest.dto.response.AuthResponse;
import com.uyinvest.dto.response.PortfolioResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class PortfolioControllerIntegrationTest extends AbstractIntegrationTest {

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

    @Test
    void createAndFetchOwnPortfolio() throws Exception {
        String token = registerAndGetToken("portfolio1@example.com");
        PortfolioRequest request = new PortfolioRequest("Cartera principal", "Largo plazo", "USD");

        String body = mockMvc.perform(post("/api/v1/portfolios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cartera principal"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PortfolioResponse created = objectMapper.readValue(body, PortfolioResponse.class);

        mockMvc.perform(get("/api/v1/portfolios/" + created.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value("USD"));

        mockMvc.perform(get("/api/v1/portfolios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void updatesOwnPortfolio() throws Exception {
        String token = registerAndGetToken("portfolio2@example.com");
        PortfolioRequest createRequest = new PortfolioRequest("Cartera", "desc", "USD");

        String body = mockMvc.perform(post("/api/v1/portfolios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PortfolioResponse created = objectMapper.readValue(body, PortfolioResponse.class);
        PortfolioRequest updateRequest = new PortfolioRequest("Cartera actualizada", "nueva desc", "EUR");

        mockMvc.perform(put("/api/v1/portfolios/" + created.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cartera actualizada"))
                .andExpect(jsonPath("$.baseCurrency").value("EUR"));
    }

    @Test
    void deletesOwnPortfolio() throws Exception {
        String token = registerAndGetToken("portfolio3@example.com");
        PortfolioRequest createRequest = new PortfolioRequest("Cartera", "desc", "USD");

        String body = mockMvc.perform(post("/api/v1/portfolios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PortfolioResponse created = objectMapper.readValue(body, PortfolioResponse.class);

        mockMvc.perform(delete("/api/v1/portfolios/" + created.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/portfolios/" + created.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotAccessAnotherUsersPortfolio() throws Exception {
        String ownerToken = registerAndGetToken("owner@example.com");
        String intruderToken = registerAndGetToken("intruder@example.com");

        PortfolioRequest createRequest = new PortfolioRequest("Cartera privada", "desc", "USD");
        String body = mockMvc.perform(post("/api/v1/portfolios")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PortfolioResponse created = objectMapper.readValue(body, PortfolioResponse.class);

        mockMvc.perform(get("/api/v1/portfolios/" + created.id())
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/portfolios/" + created.id())
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/portfolios")
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidBaseCurrency() throws Exception {
        String token = registerAndGetToken("portfolio4@example.com");
        PortfolioRequest request = new PortfolioRequest("Cartera", "desc", "dollars");

        mockMvc.perform(post("/api/v1/portfolios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
