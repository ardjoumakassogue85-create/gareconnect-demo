package com.hackathon.gares.repository;

import com.hackathon.gares.model.CompagnieProfile;
import com.hackathon.gares.model.Reservation;
import com.hackathon.gares.model.StatutReservation;
import com.hackathon.gares.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByClientOrderByCreeLeDesc(User client);

    List<Reservation> findByVilleDepartIgnoreCaseAndStatut(String villeDepart, StatutReservation statut);

    List<Reservation> findByStatutAndNoteIsNull(StatutReservation statut);

    List<Reservation> findByCompagnieIgnoreCaseAndNoteNotNull(String compagnie);

    boolean existsByCodeBillet(String codeBillet);

    @Query("""
            select r from Reservation r
            join fetch r.client
            left join fetch r.trajet t
            where t.compagnie = :compagnie
               or lower(r.compagnie) = lower(:nomCompagnie)
            order by r.creeLe desc
            """)
    List<Reservation> findForCompagnieDashboard(
            @Param("compagnie") CompagnieProfile compagnie,
            @Param("nomCompagnie") String nomCompagnie
    );

}
