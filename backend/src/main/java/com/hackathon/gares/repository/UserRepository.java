package com.hackathon.gares.repository;

import com.hackathon.gares.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPasswordResetToken(String passwordResetToken);
    boolean existsByEmail(String email);
}
