package sn.groupeisi.leaveworkflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sn.groupeisi.leaveworkflow.enums.StatutManager;
import sn.groupeisi.leaveworkflow.model.DemandeConge;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DemandeCongeRepository extends JpaRepository<DemandeConge, Long> {

    List<DemandeConge> findByUserId(Long userId);

    @Query("SELECT d FROM DemandeConge d WHERE d.user.manager.id = :managerId")
    List<DemandeConge> findByUserManagerId(@Param("managerId") Long managerId);

    @Query("SELECT d FROM DemandeConge d WHERE d.user.manager.id = :managerId ORDER BY d.dateCreation DESC")
    List<DemandeConge> findAllForManager(@Param("managerId") Long managerId);

    List<DemandeConge> findByStatutManager(StatutManager statutManager);

    @Query("SELECT d FROM DemandeConge d WHERE d.user.id = :userId AND ((d.dateDebut <= :dateFin AND d.dateFin >= :dateDebut) OR (d.dateDebut >= :dateDebut AND d.dateDebut <= :dateFin)) AND d.statutRh = 'APPROUVE'")
    List<DemandeConge> findOverlappingRequests(@Param("userId") Long userId, @Param("dateDebut") LocalDate dateDebut, @Param("dateFin") LocalDate dateFin);
}