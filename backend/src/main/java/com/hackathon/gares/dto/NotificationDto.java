package com.hackathon.gares.dto;

public record NotificationDto(
        String id,
        String type,
        String titre,
        String message,
        String reservationId,
        String compagnie,
        boolean lu,
        String creeLe
) {
}
