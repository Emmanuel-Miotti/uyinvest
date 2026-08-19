package com.uyinvest.config;

import com.uyinvest.exception.ErrorResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";
    private static final String ERROR_SCHEMA = "ErrorResponse";

    @Bean
    public OpenAPI uyinvestOpenAPI() {
        Schema<?> errorSchema = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(ErrorResponse.class))
                .schema;

        return new OpenAPI()
                .info(new Info()
                        .title("UYInvest API")
                        .description("Investment portfolio management platform")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSchemas(ERROR_SCHEMA, errorSchema))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    public OperationCustomizer commonErrorResponses() {
        return (operation, handlerMethod) -> {
            Content errorContent = new Content().addMediaType("application/json",
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA)));

            operation.getResponses()
                    .addApiResponse("400", new ApiResponse().description("Validation error").content(errorContent))
                    .addApiResponse("401", new ApiResponse().description("Authentication required").content(errorContent))
                    .addApiResponse("500", new ApiResponse().description("Unexpected server error").content(errorContent));
            return operation;
        };
    }
}
