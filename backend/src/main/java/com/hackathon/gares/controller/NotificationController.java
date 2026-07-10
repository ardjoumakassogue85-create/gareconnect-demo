package com.hackathon.gares.controller;

import com.hackathon.gares.dto.NotificationDto;
import com.hackathon.gares.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationDto> lister(Authentication authentication) {
        return notificationService.lister(authentication);
    }

    @GetMapping("/compteur")
    public Map<String, Long> compteur(Authentication authentication) {
        return Map.of("nonLues", notificationService.compterNonLues(authentication));
    }

    @PatchMapping("/{id}/lu")
    public void marquerLu(Authentication authentication, @PathVariable Long id) {
        notificationService.marquerLu(authentication, id);
    }
}
