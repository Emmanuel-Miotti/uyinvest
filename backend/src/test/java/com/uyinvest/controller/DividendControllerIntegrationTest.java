package com.uyinvest.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uyinvest.AbstractIntegrationTest;
import com.uyinvest.dto.request.AssetRequest;
import com.uyinvest.dto.request.DividendRequest;
import com.uyinvest.dto.request.PortfolioRequest;
import com.uyinvest.dto.request.RegisterRequest;
import com.uyinvest.dto.response.AssetResponse;
import com.uyinvest.dto.response.AuthResponse;
import com.uyinvest.dto.response.PortfolioResponse;
import com.uyinvest.entity.enums.AssetType;
import com.uyinvest.entity.enums.Role;
import com.uyinvest.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class DividendControllerIntegrationTest extends AbstractIntegrationTest {

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

    private void promoteToAdmin(String email) {
        var user = userRepository.findByEmail(email).orElseThrow();
        user.setRole(Role.ADMIN);
        userRepository.saveAndFlush(user);
    }

    private AssetResponse createAsset(String adminToken, String symbol) throws Exception {
        AssetRequest request = new AssetRequest(symbol, symbol + " Inc.", AssetType.STOCK, "USD", "Consumer", true);
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

    private void registerDividend(String token, UUID portfolioId, UUID assetId, String amount, LocalDate date) throws Exception {
        DividendRequest request = new DividendRequest(assetId, new BigDecimal(amount), "USD", date);
        mockMvc.perform(post("/api/v1/portfolios/" + portfolioId + "/dividends")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void registersAndListsDividends() throws Exception {
        String adminToken = registerAndGetToken("admin1@example.com");
        promoteToAdmin("admin1@example.com");
        AssetResponse asset = createAsset(adminToken, "KO");

        String userToken = registerAndGetToken("trader1@example.com");
        PortfolioResponse portfolio = createPortfolio(userToken);

        registerDividend(userToken, portfolio.id(), asset.id(), "25.50", LocalDate.now());

        mockMvc.perform(get("/api/v1/portfolios/" + portfolio.id() + "/dividends")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].asset.symbol").value("KO"));
    }

    @Test
    void filtersByAssetAndDateRange() throws Exception {
        String adminToken = registerAndGetToken("admin2@example.com");
        promoteToAdmin("admin2@example.com");
        AssetResponse ko = createAsset(adminToken, "KO");
        AssetResponse pep = createAsset(adminToken, "PEP");

        String userToken = registerAndGetToken("trader2@example.com");
        PortfolioResponse portfolio = createPortfolio(userToken);

        LocalDate oldDate = LocalDate.now().minusMonths(6);
        registerDividend(userToken, portfolio.id(), ko.id(), "10", oldDate);
        registerDividend(userToken, portfolio.id(), ko.id(), "15", LocalDate.now());
        registerDividend(userToken, portfolio.id(), pep.id(), "20", LocalDate.now());

        mockMvc.perform(get("/api/v1/portfolios/" + portfolio.id() + "/dividends")
                        .param("assetId", ko.id().toString())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/v1/portfolios/" + portfolio.id() + "/dividends")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void summaryReflectsRegisteredDividends() throws Exception {
        String adminToken = registerAndGetToken("admin3@example.com");
        promoteToAdmin("admin3@example.com");
        AssetResponse asset = createAsset(adminToken, "JNJ");

        String userToken = registerAndGetToken("trader3@example.com");
        PortfolioResponse portfolio = createPortfolio(userToken);

        registerDividend(userToken, portfolio.id(), asset.id(), "50", LocalDate.now());
        registerDividend(userToken, portfolio.id(), asset.id(), "30", LocalDate.now().minusYears(2));

        mockMvc.perform(get("/api/v1/portfolios/" + portfolio.id() + "/dividends/summary")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalThisMonth").value(50.0))
                .andExpect(jsonPath("$.totalThisYear").value(50.0))
                .andExpect(jsonPath("$.totalHistorical").value(80.0));
    }

    @Test
    void rejectsInvalidAmount() throws Exception {
        String adminToken = registerAndGetToken("admin4@example.com");
        promoteToAdmin("admin4@example.com");
        AssetResponse asset = createAsset(adminToken, "XOM");

        String userToken = registerAndGetToken("trader4@example.com");
        PortfolioResponse portfolio = createPortfolio(userToken);

        DividendRequest invalidRequest = new DividendRequest(asset.id(), BigDecimal.ZERO, "USD", LocalDate.now());

        mockMvc.perform(post("/api/v1/portfolios/" + portfolio.id() + "/dividends")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void cannotAccessAnotherUsersPortfolioDividends() throws Exception {
        String adminToken = registerAndGetToken("admin5@example.com");
        promoteToAdmin("admin5@example.com");
        AssetResponse asset = createAsset(adminToken, "T");

        String ownerToken = registerAndGetToken("owner5@example.com");
        PortfolioResponse portfolio = createPortfolio(ownerToken);
        registerDividend(ownerToken, portfolio.id(), asset.id(), "10", LocalDate.now());

        String intruderToken = registerAndGetToken("intruder5@example.com");

        mockMvc.perform(get("/api/v1/portfolios/" + portfolio.id() + "/dividends")
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios/" + UUID.randomUUID() + "/dividends"))
                .andExpect(status().isUnauthorized());
    }
}
