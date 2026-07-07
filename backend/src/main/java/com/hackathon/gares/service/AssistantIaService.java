package com.hackathon.gares.service;

import com.hackathon.gares.dto.CriteresRecherche;

public interface AssistantIaService {
    CriteresRecherche extraireCriteresRecherche(String texteLibre);
}
