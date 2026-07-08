package com.hackathon.gares.service;

import com.hackathon.gares.dto.ReclamationIaRequest;
import com.hackathon.gares.dto.ReclamationIaResponse;

public interface ReclamationIaClient {
    ReclamationIaResponse traiter(ReclamationIaRequest request);
}
