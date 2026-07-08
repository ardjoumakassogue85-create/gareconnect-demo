package com.hackathon.gares.controller;

import com.hackathon.gares.dto.CreneauArriveeRequest;
import com.hackathon.gares.dto.CreneauArriveeResponse;
import com.hackathon.gares.service.FileVirtuelleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/file-virtuelle")
@RequiredArgsConstructor
public class FileVirtuelleController {

    private final FileVirtuelleService fileVirtuelleService;

    @PostMapping
    public CreneauArriveeResponse attribuer(Authentication authentication,
                                            @Valid @RequestBody CreneauArriveeRequest request) {
        return fileVirtuelleService.attribuer(authentication, request.trajetId());
    }
}
