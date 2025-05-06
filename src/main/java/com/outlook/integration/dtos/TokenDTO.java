package com.outlook.integration.dtos;

public class TokenDTO {

    private String accessToken;
    private String refreshToken;

    // Construtor padrão
    public TokenDTO() {
    }

    // Construtor com parâmetros
    public TokenDTO(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    // Getters e Setters
    public String getAccessToken() {
        return accessToken;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
