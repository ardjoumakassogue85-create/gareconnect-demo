package com.hackathon.gares.repository;

import com.hackathon.gares.model.CompagnieProfile;
import com.hackathon.gares.model.StatutTrajet;
import com.hackathon.gares.model.Trajet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TrajetRepository extends JpaRepository<Trajet, Long> {
    List<Trajet> findByCompagnieOrderByDateAscHeureDepartAsc(CompagnieProfile compagnie);
    List<Trajet> findByStatutOrderByDateAscHeureDepartAsc(StatutTrajet statut);
    List<Trajet> findByStatutAndVilleDepartIgnoreCaseAndVilleArriveeIgnoreCaseAndDateOrderByHeureDepartAsc(
            StatutTrajet statut,
            String villeDepart,
            String villeArrivee,
            LocalDate date
    );
}
