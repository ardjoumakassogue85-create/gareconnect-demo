package com.hackathon.gares.controller;

import com.hackathon.gares.dto.BilletTokenResponse;
import com.hackathon.gares.dto.ClePubliqueResponse;
import com.hackathon.gares.dto.ValiderBilletRequest;
import com.hackathon.gares.dto.ValidationBilletResponse;
import com.hackathon.gares.service.BilletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billets")
@RequiredArgsConstructor
public class BilletController {

    private final BilletService billetService;

    /** Jeton signe a encoder dans le QR du billet (client, sur sa propre reservation). */
    @GetMapping("/token/{reservationId}")
    public BilletTokenResponse token(Authentication authentication, @PathVariable Long reservationId) {
        return billetService.genererTokenBillet(authentication, reservationId);
    }

    /** Controle d'un billet a l'embarquement (agent compagnie). */
    @PostMapping("/valider")
    public ValidationBilletResponse valider(Authentication authentication,
                                            @Valid @RequestBody ValiderBilletRequest request) {
        return billetService.valider(authentication, request.token());
    }

    /** Cle publique pour la verification hors-ligne (agent compagnie). */
    @GetMapping("/cle-publique")
    public ClePubliqueResponse clePublique() {
        return billetService.clePublique();
    }
}
