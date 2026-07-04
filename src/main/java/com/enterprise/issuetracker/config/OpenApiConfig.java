package com.enterprise.issuetracker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Swagger / OpenAPI v3 documentation.
 * Defines project info, metadata, and security schemes (JWT Bearer tokens).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .name("Bearer Authentication")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter Bearer Token in the format: Bearer <access_token>")))
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
}
