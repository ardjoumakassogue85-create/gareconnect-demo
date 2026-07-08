package com.hackathon.gares.controller;

import com.hackathon.gares.dto.AffluenceGareDto;
import com.hackathon.gares.dto.ContexteAffluenceDto;
import com.hackathon.gares.service.AffluenceService;
import com.hackathon.gares.service.ContexteAffluenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/affluence")
@RequiredArgsConstructor
public class AffluenceController {

    private final AffluenceService affluenceService;
    private final ContexteAffluenceService contexteAffluenceService;

    @GetMapping("/gare")
    public AffluenceGareDto affluenceGare(
            @RequestParam String ville,
            @RequestParam(required = false) String date) {
        return affluenceService.affluenceGare(ville, parseDate(date));
    }

    @GetMapping("/contexte")
    public ContexteAffluenceDto contexte(
            @RequestParam String ville,
            @RequestParam(required = false) String date) {
        return contexteAffluenceService.pour(ville, parseDate(date));
    }

    private static LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        try {
            return LocalDate.parse(date.trim());
        } catch (Exception ignore) {
            return null;
        }
    }
}
