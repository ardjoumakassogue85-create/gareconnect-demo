package com.hackathon.gares.repository;

import com.hackathon.gares.model.CompagnieProfile;
import com.hackathon.gares.model.StatutTrajet;
import com.hackathon.gares.model.Trajet;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrajetRepository extends JpaRepository<Trajet, Long> {
    List<Trajet> findByCompagnieOrderByDateAscHeureDepartAsc(CompagnieProfile compagnie);

    List<Trajet> findByCompagnieAndStatutOrderByDateAscHeureDepartAsc(CompagnieProfile compagnie, StatutTrajet statut);

    List<Trajet> findByStatutOrderByDateAscHeureDepartAsc(StatutTrajet statut);

    List<Trajet> findByStatutAndVilleDepartIgnoreCaseAndVilleArriveeIgnoreCaseAndDateOrderByHeureDepartAsc(StatutTrajet statut, String villeDepart, String villeArrivee, LocalDate date);
}