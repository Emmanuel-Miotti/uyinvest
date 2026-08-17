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
import com.uyinvest.service.marketdata.MarketDataProvider;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class PortfolioDashboardControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MarketDataProvider marketDataProvider;

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

    private AssetResponse createAsset(String adminToken, String symbol) throws Exception {
        AssetRequest request = new AssetRequest(symbol, symbol + " Inc.", AssetType.STOCK, "USD", "Technology", true);
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

    private void buy(String token, java.util.UUID portfolioId, java.util.UUID assetId, String quantity, String price) throws Exception {
        TransactionRequest request = new TransactionRequest(
                assetId, TransactionType.BUY, new BigDecimal(quantity), new BigDecimal(price),
                BigDecimal.ZERO, "USD", Instant.now());
        mockMvc.perform(post("/api/v1/portfolios/" + portfolioId + "/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void summaryReflectsBoughtPositionAgainstMockMarketPrice() throws Exception {
        String adminToken = registerAndGetToken("admin1@example.com");
        var admin = userRepository.findByEmail("admin1@example.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.saveAndFlush(admin);

        AssetResponse asset = createAsset(adminToken, "AAPL");
        String userToken = registerAndGetToken("trader1@example.com");
        PortfolioResponse portfolio = createPortfolio(userToken);

        buy(userToken, portfolio.id(), asset.id(), "10", "100");

        BigDecimal currentPrice = marketDataProvider.getCurrentPrice("AAPL");
        BigDecimal expectedValue = currentPrice.multiply(BigDecimal.TEN);

        mockMvc.perform(get("/api/v1/portfolios/" + portfolio.id() + "/summary")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvested").value(1000.0))
                .andExpect(jsonPath("$.currentValue").value(expectedValue.doubleValue()));
    }

    @Test
    void allocationSplitsByAssetType() throws Exception {
        String adminToken = registerAndGetToken("admin2@example.com");
        var admin = userRepository.findByEmail("admin2@example.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.saveAndFlush(admin);

        AssetResponse asset = createAsset(adminToken, "MSFT");
        String userToken = registerAndGetToken("trader2@example.com");
        PortfolioResponse portfolio = createPortfolio(userToken);

        buy(userToken, portfolio.id(), asset.id(), "5", "200");

        mockMvc.perform(get("/api/v1/portfolios/" + portfolio.id() + "/allocation")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assetType").value("STOCK"))
                .andExpect(jsonPath("$[0].percentage").value(100.00));
    }

    @Test
    void performanceReturnsOnePointPerTransaction() throws Exception {
        String adminToken = registerAndGetToken("admin3@example.com");
        var admin = userRepository.findByEmail("admin3@example.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.saveAndFlush(admin);

        AssetResponse asset = createAsset(adminToken, "GOOG");
        String userToken = registerAndGetToken("trader3@example.com");
        PortfolioResponse portfolio = createPortfolio(userToken);

        buy(userToken, portfolio.id(), asset.id(), "2", "100");
        buy(userToken, portfolio.id(), asset.id(), "3", "100");

        mockMvc.perform(get("/api/v1/portfolios/" + portfolio.id() + "/performance")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].totalInvested").value(500.0));
    }

    @Test
    void dashboardEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios/" + java.util.UUID.randomUUID() + "/summary"))
                .andExpect(status().isUnauthorized());
    }
}
