package com.hackathon.gares.controller;

import com.hackathon.gares.dto.VitrineDto;
import com.hackathon.gares.service.CompagnieService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vitrines")
@RequiredArgsConstructor
public class VitrineController {

    private final CompagnieService compagnieService;

    @GetMapping("/{compagnie}")
    public VitrineDto obtenirVitrine(@PathVariable String compagnie) {
        return compagnieService.obtenirVitrine(compagnie);
    }

    @GetMapping("/{compagnie}/trajets")
    public java.util.List<com.hackathon.gares.dto.TrajetDto> listerTrajets(@PathVariable String compagnie) {
        return compagnieService.listerTrajetsPublics(compagnie);
    }
}
