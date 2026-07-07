package com.hackathon.gares.service;

import com.hackathon.gares.dto.AvisGareRequest;
import com.hackathon.gares.dto.NoteMoyenneGareDto;
import com.hackathon.gares.model.AvisGare;
import com.hackathon.gares.repository.AvisGareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GareService {

    private final AvisGareRepository avisGareRepository;

    @Transactional
    public NoteMoyenneGareDto noter(String codeGare, AvisGareRequest request) {
        String code = codeGare == null || codeGare.isBlank() ? request.codeGare() : codeGare;
        avisGareRepository.save(AvisGare.builder()
                .codeGare(code)
                .note(Math.max(1, Math.min(5, request.note())))
                .commentaire(request.commentaire())
                .build());
        return moyenne(code);
    }

    @Transactional(readOnly = true)
    public NoteMoyenneGareDto moyenne(String codeGare) {
        List<AvisGare> avis = avisGareRepository.findByCodeGareIgnoreCase(codeGare);
        double moyenne = avis.stream().mapToInt(AvisGare::getNote).average().orElse(0);
        return new NoteMoyenneGareDto(codeGare, Math.round(moyenne * 10.0) / 10.0, avis.size());
    }
}
