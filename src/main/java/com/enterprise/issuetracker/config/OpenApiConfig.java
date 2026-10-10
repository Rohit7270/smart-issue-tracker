package com.enterprise.issuetracker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * Configuration class for Swagger / OpenAPI v3 documentation.
 * Defines project info, metadata, and security schemes (JWT Bearer tokens).
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT access token")))
                .info(new Info()
                        .title("Smart Issue Tracking & Ticket Management System APIs")
                        .version("1.0.0")
                        .description("Enterprise-grade REST APIs for ticket creation, user authentication, role-based controls, dashboard metrics, and audit log tracking.")
                        .contact(new Contact()
                                .name("Backend Engineering Team")
                                .email("support@enterprise.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }

    /**
     * Overrides security requirement for public authentication endpoints (/api/v1/auth/**)
     * so Swagger UI does not require credentials for registration, login, and refresh.
     */
    @Bean
    public OpenApiCustomizer publicEndpointCustomizer() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((path, pathItem) -> {
                    if (path.startsWith("/api/v1/auth")) {
                        pathItem.readOperations().forEach(operation -> operation.setSecurity(Collections.emptyList()));
                    }
                });
            }
        };
    }
}
