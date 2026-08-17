package com.uyinvest.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uyinvest.AbstractIntegrationTest;
import com.uyinvest.dto.request.AssetRequest;
import com.uyinvest.dto.request.PortfolioRequest;
import com.uyinvest.dto.request.RegisterRequest;
import com.uyinvest.dto.request.TransactionRequest;
import com.uyinvest.dto.response.AssetResponse;
import com.uyinvest.dto.response.AuthResponse;
import com.uyinvest.dto.response.PortfolioResponse;
import com.uyinvest.entity.enums.AssetType;
import com.uyinvest.entity.enums.Role;
import com.uyinvest.entity.enums.TransactionType;
import com.uyinvest.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class TransactionControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

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

    private String promoteToAdmin(String email) {
        var user = userRepository.findByEmail(email).orElseThrow();
        user.setRole(Role.ADMIN);
        userRepository.saveAndFlush(user);
        return email;
    }

    private AssetResponse createAsset(String adminToken, String symbol, boolean active) throws Exception {
        AssetRequest request = new AssetRequest(symbol, symbol + " Inc.", AssetType.STOCK, "USD", "Technology", active);
        String body = mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(body, AssetResponse.class);
    }

    private PortfolioResponse createPortfolio(String token) throws Exception {
        PortfolioRequest request = new PortfolioRequest("Cartera", "desc", "USD");
        String body = mockMvc.perform(post("/api/v1/portfolios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(body, PortfolioResponse.class);
    }

    @Test
    void buysAndListsTransactions() throws Exception {
        String adminToken = registerAndGetToken("admin1@example.com");
        promoteToAdmin("admin1@example.com");
        AssetResponse asset = createAsset(adminToken, "AAPL", true);

        String userToken = registerAndGetToken("trader1@example.com");
        PortfolioResponse portfolio = createPortfolio(userToken);

        TransactionRequest buyRequest = new TransactionRequest(
                asset.id(), TransactionType.BUY, new BigDecimal("10"), new BigDecimal("100.00"),
                new BigDecimal("5.00"), "USD", Instant.now());

        mockMvc.perform(post("/api/v1/portfolios/" + portfolio.id() + "/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("BUY"))
                .andExpect(jsonPath("$.asset.symbol").value("AAPL"));

        mockMvc.perform(get("/api/v1/portfolios/" + portfolio.id() + "/transactions")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rejectsSellingMoreThanAvailable() throws Exception {
        String adminToken = registerAndGetToken("admin2@example.com");
        promoteToAdmin("admin2@example.com");
        AssetResponse asset = createAsset(adminToken, "MSFT", true);

        String userToken = registerAndGetToken("trader2@example.com");
        PortfolioResponse portfolio = createPortfolio(userToken);

        TransactionRequest buyRequest = new TransactionRequest(
                asset.id(), TransactionType.BUY, new BigDecimal("5"), new BigDecimal("100.00"),
                BigDecimal.ZERO, "USD", Instant.now());
        mockMvc.perform(post("/api/v1/portfolios/" + portfolio.id() + "/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyRequest)))
                .andExpect(status().isCreated());

        TransactionRequest sellRequest = new TransactionRequest(
                asset.id(), TransactionType.SELL, new BigDecimal("10"), new BigDecimal("110.00"),
                BigDecimal.ZERO, "USD", Instant.now());
        mockMvc.perform(post("/api/v1/portfolios/" + portfolio.id() + "/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void rejectsTradingInactiveAsset() throws Exception {
        String adminToken = registerAndGetToken("admin3@example.com");
        promoteToAdmin("admin3@example.com");
        AssetResponse asset = createAsset(adminToken, "OLDCO", false);

        String userToken = registerAndGetToken("trader3@example.com");
        PortfolioResponse portfolio = createPortfolio(userToken);

        TransactionRequest buyRequest = new TransactionRequest(
                asset.id(), TransactionType.BUY, BigDecimal.TEN, new BigDecimal("50.00"),
                BigDecimal.ZERO, "USD", Instant.now());

        mockMvc.perform(post("/api/v1/portfolios/" + portfolio.id() + "/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyRequest)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejectsInvalidQuantityAndPrice() throws Exception {
        String adminToken = registerAndGetToken("admin4@example.com");
        promoteToAdmin("admin4@example.com");
        AssetResponse asset = createAsset(adminToken, "GOOG", true);

        String userToken = registerAndGetToken("trader4@example.com");
        PortfolioResponse portfolio = createPortfolio(userToken);

        TransactionRequest invalidRequest = new TransactionRequest(
                asset.id(), TransactionType.BUY, new BigDecimal("-1"), BigDecimal.ZERO,
                BigDecimal.ZERO, "USD", Instant.now());

        mockMvc.perform(post("/api/v1/portfolios/" + portfolio.id() + "/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void cannotOperateOnAnotherUsersPortfolio() throws Exception {
        String adminToken = registerAndGetToken("admin5@example.com");
        promoteToAdmin("admin5@example.com");
        AssetResponse asset = createAsset(adminToken, "AMZN", true);

        String ownerToken = registerAndGetToken("owner5@example.com");
        PortfolioResponse portfolio = createPortfolio(ownerToken);

        String intruderToken = registerAndGetToken("intruder5@example.com");
        TransactionRequest buyRequest = new TransactionRequest(
                asset.id(), TransactionType.BUY, BigDecimal.TEN, new BigDecimal("50.00"),
                BigDecimal.ZERO, "USD", Instant.now());

        mockMvc.perform(post("/api/v1/portfolios/" + portfolio.id() + "/transactions")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios/" + java.util.UUID.randomUUID() + "/transactions"))
                .andExpect(status().isUnauthorized());
    }
}
