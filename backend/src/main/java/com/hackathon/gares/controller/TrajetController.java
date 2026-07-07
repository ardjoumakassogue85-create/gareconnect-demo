package com.hackathon.gares.controller;

import com.hackathon.gares.dto.TrajetDto;
import com.hackathon.gares.service.VoyageurService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trajets")
@RequiredArgsConstructor
public class TrajetController {

    private final VoyageurService voyageurService;

    @GetMapping("/recherche")
    public List<TrajetDto> rechercher(
            @RequestParam(required = false) String villeDepart,
            @RequestParam(required = false) String villeArrivee,
            @RequestParam(required = false) String date
    ) {
        return voyageurService.rechercher(villeDepart, villeArrivee, date);
    }

    @GetMapping("/{id}")
    public TrajetDto obtenir(@PathVariable Long id) {
        return voyageurService.obtenirTrajet(id);
    }
}
