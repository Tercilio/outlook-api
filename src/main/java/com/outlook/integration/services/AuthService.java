package com.outlook.integration.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class AuthService {

    @Value("${outlook.client.id}")
    private String clientId;

    @Value("${outlook.client.secret}")
    private String clientSecret;

    private final WebClient webClient = WebClient.create("https://login.microsoftonline.com");
    private final WebClient graphClient = WebClient.create("https://graph.microsoft.com");

    public Map<String, Object> generateTokens(String code, String redirectUri) {
        // 1. Solicita tokens usando authorization_code
        Map<String, Object> tokenResponse = webClient.post()
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

        String accessToken = tokenResponse.get("access_token").toString();


        Map<String, Object> userInfo = graphClient.get()
                .uri("/v1.0/me")
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // 3. Extract Email 
        String email = null;
        if (userInfo.get("mail") != null) {
            email = userInfo.get("mail").toString();
        } else if (userInfo.get("userPrincipalName") != null) {
            email = userInfo.get("userPrincipalName").toString();
        }

        return Map.of(
                "access_token", tokenResponse.get("access_token"),
                "refresh_token", tokenResponse.get("refresh_token"),
                "email", email,
                "user_id", userInfo.get("id")
        );
    }

}
