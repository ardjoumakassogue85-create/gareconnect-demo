package com.hackathon.gares.dto;

import com.hackathon.gares.model.AuteurMessage;

public record MessageReclamationDto(String id, AuteurMessage auteur, String texte, String envoyeLe) {}
