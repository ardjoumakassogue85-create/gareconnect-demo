package com.hackathon.gares.controller;

import com.hackathon.gares.dto.AccountResponse;
import com.hackathon.gares.dto.AuthResponse;
import com.hackathon.gares.dto.UpdateAccountRequest;
import com.hackathon.gares.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public AccountResponse obtenirCompte(Authentication authentication) {
        return userService.obtenirCompte(authentication);
    }

    @PutMapping
    public AuthResponse mettreAJourCompte(Authentication authentication,
                                          @Valid @RequestBody UpdateAccountRequest request) {
        return userService.mettreAJourCompte(authentication, request);
    }
}
