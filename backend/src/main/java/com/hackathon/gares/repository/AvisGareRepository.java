package com.hackathon.gares.repository;

import com.hackathon.gares.model.AvisGare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvisGareRepository extends JpaRepository<AvisGare, Long> {
    List<AvisGare> findByCodeGareIgnoreCase(String codeGare);
}
