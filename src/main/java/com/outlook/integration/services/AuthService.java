package com.outlook.integration.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Map;

@Service
public class AuthService {

    @Value("${outlook.client.id}")
    private String clientId;

    @Value("${outlook.client.secret}")
    private String clientSecret;

    private final WebClient webClient = WebClient.create("https://login.microsoftonline.com");

    public Map<String, Object> generateTokens(String code, String redirectUri) {
        return webClient.post()
                .uri("/common/oauth2/v2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(
                    BodyInserters.fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("grant_type", "authorization_code")
                        .with("code", code)
                        .with("redirect_uri", redirectUri)
                )
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}
