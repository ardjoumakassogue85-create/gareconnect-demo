package com.hackathon.gares.repository;

import com.hackathon.gares.model.Reclamation;
import com.hackathon.gares.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {
    List<Reclamation> findByClientOrderByMajLeDesc(User client);
    List<Reclamation> findAllByOrderByMajLeDesc();
}
