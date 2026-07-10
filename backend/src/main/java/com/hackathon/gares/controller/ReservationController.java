package com.hackathon.gares.controller;

import com.hackathon.gares.dto.AvisRequest;
import com.hackathon.gares.dto.ReservationDto;
import com.hackathon.gares.dto.ReservationRequest;
import com.hackathon.gares.service.VoyageurService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final VoyageurService voyageurService;

    @PostMapping
    public ReservationDto creer(Authentication authentication, @RequestBody ReservationRequest request) {
        return voyageurService.creerReservation(authentication, request);
    }

    @GetMapping("/me")
    public List<ReservationDto> lister(Authentication authentication) {
        return voyageurService.listerMesReservations(authentication);
    }

    @GetMapping("/{id}")
    public ReservationDto obtenir(Authentication authentication, @PathVariable Long id) {
        return voyageurService.obtenirReservation(authentication, id);
    }

    @PatchMapping("/{id}/annuler")
    public ReservationDto annuler(Authentication authentication, @PathVariable Long id) {
        return voyageurService.annulerReservation(authentication, id);
    }

    @PostMapping("/{id}/avis")
    public ReservationDto laisserAvis(Authentication authentication, @PathVariable Long id, @RequestBody AvisRequest request) {
        return voyageurService.laisserAvis(authentication, id, request);
    }
}
