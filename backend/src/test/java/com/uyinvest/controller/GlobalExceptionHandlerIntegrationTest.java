package com.uyinvest.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uyinvest.AbstractIntegrationTest;
import com.uyinvest.dto.request.RegisterRequest;
import com.uyinvest.dto.response.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class GlobalExceptionHandlerIntegrationTest extends AbstractIntegrationTest {

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
    void malformedJsonBodyReturnsBadRequestNotServerError() throws Exception {
        String token = registerAndGetToken("malformed@example.com");

        mockMvc.perform(post("/api/v1/portfolios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void invalidUuidPathVariableReturnsBadRequestNotServerError() throws Exception {
        String token = registerAndGetToken("baduuid@example.com");

        mockMvc.perform(get("/api/v1/portfolios/not-a-valid-uuid")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void errorResponseNeverExposesStackTraceOrExceptionClassName() throws Exception {
        String token = registerAndGetToken("nostacktrace@example.com");

        String body = mockMvc.perform(get("/api/v1/portfolios/not-a-valid-uuid")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("Exception")
                .doesNotContain("java.lang")
                .doesNotContain("\tat ");
    }

    @Test
    void everyErrorResponseMatchesTheDocumentedShape() throws Exception {
        String token = registerAndGetToken("shapecheck@example.com");

        mockMvc.perform(get("/api/v1/portfolios/" + java.util.UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value(org.hamcrest.Matchers.startsWith("/api/v1/portfolios/")));
    }
}
