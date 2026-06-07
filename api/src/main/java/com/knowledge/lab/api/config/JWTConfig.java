package com.knowledge.lab.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JWTConfig {

    @NotBlank(message = "JWT secret must not be blank")
    private String secret;

    @Positive
    private long accessTokenExpiryMs = 900_000L;        // 15 min default

    @Positive
    private long refreshTokenExpiryMs = 604_800_000L;   // 7 days default

    @NotBlank
    private String issuer = "grading_knowledge_api";

}
