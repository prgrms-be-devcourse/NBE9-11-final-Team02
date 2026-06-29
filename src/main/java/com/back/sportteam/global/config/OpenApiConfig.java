package com.back.sportteam.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class OpenApiConfig {

    private static final String JWT_SCHEME_NAME = "JWT";

    @Bean
    public OpenAPI openAPI(@Value("${app.swagger.aws-url:}") String awsUrl) {
        List<Server> servers = new ArrayList<>();
        servers.add(new Server()
                .url("http://localhost:8090")
                .description("Local Server"));

        if (StringUtils.hasText(awsUrl)) {
            servers.add(new Server()
                    .url(awsUrl)
                    .description("AWS Server"));
        }

        return new OpenAPI()
                .servers(servers)
                .info(new Info()
                        .title("SportTeam API")
                        .description("Sports facility reservation and matching platform API documentation")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(JWT_SCHEME_NAME, new SecurityScheme()
                                .name(JWT_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME_NAME));
    }
}