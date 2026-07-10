package com.hackathon.gares.repository;

import com.hackathon.gares.model.Notification;
import com.hackathon.gares.model.TypeNotification;
import com.hackathon.gares.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreeLeDesc(User user);

    long countByUserAndLuFalse(User user);

    boolean existsByReservationIdAndType(Long reservationId, TypeNotification type);

    Optional<Notification> findByReservationIdAndType(Long reservationId, TypeNotification type);

    Optional<Notification> findByIdAndUser(Long id, User user);
}
