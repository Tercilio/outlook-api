package com.outlook.integration.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.outlook.integration.services.AuthService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/oauth")
@Tag(
	    name = "Auth Code",
	    description = "Endpoints para autenticação com Outlook e geração de access_token e refresh_token usando Authorization Code Flow"
	)
public class AuthController {

	 @Autowired
	    private AuthService authService;

	    @PostMapping("/token")
	    public Map<String, Object> getTokens(@RequestParam String code, @RequestParam String redirectUri) {
	        return authService.generateTokens(code, redirectUri);
	    }
}
