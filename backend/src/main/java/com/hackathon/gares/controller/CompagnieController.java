package com.hackathon.gares.controller;

import com.hackathon.gares.dto.*;
import com.hackathon.gares.service.CompagnieService;
import com.hackathon.gares.service.ReclamationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compagnies/me")
@RequiredArgsConstructor
public class CompagnieController {

    private final CompagnieService compagnieService;
    private final ReclamationService reclamationService;

    @PutMapping("/vitrine")
    public VitrineDto enregistrerVitrine(Authentication authentication, @RequestBody VitrineRequest request) {
        return compagnieService.enregistrerVitrine(authentication, request);
    }

    @GetMapping("/trajets")
    public List<TrajetDto> listerTrajets(Authentication authentication) {
        return compagnieService.listerTrajets(authentication);
    }

    @GetMapping("/reservations")
    public List<ReservationCompagnieDto> listerReservations(Authentication authentication) {
        return compagnieService.listerReservations(authentication);
    }

    @PostMapping("/trajets")
    public TrajetDto creerTrajet(Authentication authentication, @RequestBody TrajetRequest request) {
        return compagnieService.creerTrajet(authentication, request);
    }

    @PutMapping("/trajets/{id}")
    public TrajetDto modifierTrajet(Authentication authentication, @PathVariable Long id, @RequestBody TrajetRequest request) {
        return compagnieService.modifierTrajet(authentication, id, request);
    }

    @DeleteMapping("/trajets/{id}")
    public void supprimerTrajet(Authentication authentication, @PathVariable Long id) {
        compagnieService.supprimerTrajet(authentication, id);
    }

    @PatchMapping("/trajets/{id}/statut")
    public TrajetDto basculerStatutTrajet(Authentication authentication, @PathVariable Long id) {
        return compagnieService.basculerStatutTrajet(authentication, id);
    }

    @GetMapping("/reclamations")
    public List<ReclamationDto> listerReclamations() {
        return reclamationService.listerPourCompagnie();
    }

    @PatchMapping("/reclamations/{id}")
    public ReclamationDto repondreReclamation(@PathVariable Long id, @RequestBody ReclamationStatutRequest request) {
        return reclamationService.repondre(id, request);
    }
}
