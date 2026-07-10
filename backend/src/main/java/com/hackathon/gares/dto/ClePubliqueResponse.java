package com.hackathon.gares.dto;

/** Cle publique (SPKI base64) pour verifier les billets hors-ligne. */
public record ClePubliqueResponse(
        String cle,
        String algorithme
) {
}
