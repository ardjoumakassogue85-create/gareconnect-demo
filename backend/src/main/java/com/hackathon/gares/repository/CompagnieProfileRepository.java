package com.hackathon.gares.repository;

import com.hackathon.gares.model.CompagnieProfile;
import com.hackathon.gares.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompagnieProfileRepository extends JpaRepository<CompagnieProfile, Long> {
    Optional<CompagnieProfile> findBySlug(String slug);
    Optional<CompagnieProfile> findByNomIgnoreCase(String nom);
    Optional<CompagnieProfile> findByUser(User user);
}
