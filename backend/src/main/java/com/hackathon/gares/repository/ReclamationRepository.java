package com.hackathon.gares.repository;

import com.hackathon.gares.model.CompagnieProfile;
import com.hackathon.gares.model.Reclamation;
import com.hackathon.gares.model.StatutReclamation;
import com.hackathon.gares.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {
    List<Reclamation> findByClientOrderByMajLeDesc(User client);

    @EntityGraph(attributePaths = {"client", "messages"})
    List<Reclamation> findByStatutOrderByMajLeDesc(StatutReclamation statut);

    @EntityGraph(attributePaths = {"client", "messages"})
    List<Reclamation> findByStatutAndCompagnieOrderByMajLeDesc(StatutReclamation statut, CompagnieProfile compagnie);

    List<Reclamation> findAllByOrderByMajLeDesc();
}
