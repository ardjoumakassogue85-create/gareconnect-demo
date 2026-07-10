package com.hackathon.gares.repository;

import com.hackathon.gares.model.CreneauArrivee;
import com.hackathon.gares.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CreneauArriveeRepository extends JpaRepository<CreneauArrivee, Long> {

    long countByGareIgnoreCaseAndDateVoyageAndFenetre(String gare, LocalDate dateVoyage, String fenetre);

    Optional<CreneauArrivee> findByUserAndTrajetId(User user, Long trajetId);
}
