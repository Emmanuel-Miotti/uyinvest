package com.uyinvest.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uyinvest.AbstractIntegrationTest;
import com.uyinvest.dto.request.AssetRequest;
import com.uyinvest.dto.request.RegisterRequest;
import com.uyinvest.dto.response.AssetResponse;
import com.uyinvest.dto.response.AuthResponse;
import com.uyinvest.entity.enums.AssetType;
import com.uyinvest.entity.enums.Role;
import com.uyinvest.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AssetControllerIntegrationTest extends AbstractIntegrationTest {

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

    private String registerAdminAndGetToken(String email) throws Exception {
        String token = registerAndGetToken(email);
        var user = userRepository.findByEmail(email).orElseThrow();
        user.setRole(Role.ADMIN);
        userRepository.saveAndFlush(user);
        return token;
    }

    @Test
    void adminCanCreateAsset() throws Exception {
        String adminToken = registerAdminAndGetToken("admin1@example.com");
        AssetRequest request = new AssetRequest("aapl", "Apple Inc.", AssetType.STOCK, "USD", "Technology", true);

        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    @Test
    void regularUserCannotCreateAsset() throws Exception {
        String userToken = registerAndGetToken("user1@example.com");
        AssetRequest request = new AssetRequest("MSFT", "Microsoft", AssetType.STOCK, "USD", "Technology", true);

        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void creatingAssetWithoutAuthenticationReturnsUnauthorized() throws Exception {
        AssetRequest request = new AssetRequest("TSLA", "Tesla", AssetType.STOCK, "USD", "Automotive", true);

        mockMvc.perform(post("/api/v1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDuplicateSymbolOnCreate() throws Exception {
        String adminToken = registerAdminAndGetToken("admin2@example.com");
        AssetRequest request = new AssetRequest("GOOG", "Alphabet", AssetType.STOCK, "USD", "Technology", true);

        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCanUpdateAsset() throws Exception {
        String adminToken = registerAdminAndGetToken("admin3@example.com");
        AssetRequest createRequest = new AssetRequest("AMZN", "Amazon", AssetType.STOCK, "USD", "Retail", true);

        String body = mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AssetResponse created = objectMapper.readValue(body, AssetResponse.class);
        AssetRequest updateRequest = new AssetRequest("AMZN", "Amazon.com Inc.", AssetType.STOCK, "USD", "E-commerce", false);

        mockMvc.perform(put("/api/v1/assets/" + created.id())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Amazon.com Inc."))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void searchFiltersByTypeAndKeyword() throws Exception {
        String adminToken = registerAdminAndGetToken("admin4@example.com");
        String userToken = registerAndGetToken("user4@example.com");

        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssetRequest("MSFT", "Microsoft Corp", AssetType.STOCK, "USD", "Technology", true))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssetRequest("SPY", "SPDR S&P 500 ETF", AssetType.ETF, "USD", null, true))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/assets")
                        .param("type", "ETF")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].symbol").value("SPY"));

        mockMvc.perform(get("/api/v1/assets")
                        .param("search", "microsoft")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].symbol").value("MSFT"));
    }

    @Test
    void listingAssetsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/assets"))
                .andExpect(status().isUnauthorized());
    }
}
