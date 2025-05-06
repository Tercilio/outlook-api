package com.outlook.integration.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class TokenUtil {

    @Value("${outlook.client.id}")
    private String clientId;

    @Value("${outlook.client.secret}")
    private String clientSecret;

    private final WebClient webClient = WebClient.create("https://login.microsoftonline.com");

    public String generateAccessTokenFromRefreshToken(String refreshToken) {
        Map<String, Object> response = webClient.post()
                .uri("/common/oauth2/v2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(
                        BodyInserters.fromFormData("client_id", clientId)
                                .with("client_secret", clientSecret)
                                .with("grant_type", "refresh_token")
                                .with("refresh_token", refreshToken)
                                .with("scope", "https://graph.microsoft.com/.default")
                )
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return response.get("access_token").toString();
    }
}
