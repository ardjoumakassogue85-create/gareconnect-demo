package com.hackathon.gares.controller;

import com.hackathon.gares.dto.ReclamationDto;
import com.hackathon.gares.dto.ReclamationRequest;
import com.hackathon.gares.service.ReclamationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reclamations")
@RequiredArgsConstructor
public class ReclamationController {

    private final ReclamationService reclamationService;

    @GetMapping("/me")
    public List<ReclamationDto> lister(Authentication authentication) {
        return reclamationService.listerMesReclamations(authentication);
    }

    @PostMapping
    public ReclamationDto demarrer(Authentication authentication, @RequestBody ReclamationRequest request) {
        return reclamationService.demarrer(authentication, request.message());
    }

    @PostMapping("/{id}/messages")
    public ReclamationDto ajouterMessage(Authentication authentication, @PathVariable Long id, @RequestBody ReclamationRequest request) {
        return reclamationService.ajouterMessage(authentication, id, request.message());
    }
}
