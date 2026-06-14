package com.ticket.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Ticket Management System API",
        version = "0.1.0",
        description = "Enterprise ticket management system — REST API for creating, tracking, and managing support tickets. "
                    + "Supports three roles: USER (submit tickets), AGENT (process tickets), ADMIN (full system control).",
        contact = @Contact(name = "Ticket System Team")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local development server")
    }
)
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Enter your JWT access token obtained from POST /api/auth/login"
)
public class OpenApiConfig {
}
