package com.hackathon.gares.controller;

import com.hackathon.gares.dto.AvisGareRequest;
import com.hackathon.gares.dto.NoteMoyenneGareDto;
import com.hackathon.gares.service.GareService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gares")
@RequiredArgsConstructor
public class GareController {

    private final GareService gareService;

    @PostMapping("/{code}/avis")
    public NoteMoyenneGareDto noter(@PathVariable String code, @RequestBody AvisGareRequest request) {
        return gareService.noter(code, request);
    }

    @GetMapping("/{code}/note")
    public NoteMoyenneGareDto moyenne(@PathVariable String code) {
        return gareService.moyenne(code);
    }
}
