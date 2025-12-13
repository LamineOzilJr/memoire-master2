package sn.groupeisi.leaveworkflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sn.groupeisi.leaveworkflow.model.SoldeConge;
import sn.groupeisi.leaveworkflow.model.TypeConge;
import sn.groupeisi.leaveworkflow.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface SoldeCongeRepository extends JpaRepository<SoldeConge, Long> {
    Optional<SoldeConge> findByUserAndTypeCongeAndAnnee(User user, TypeConge typeConge, Integer annee);

    @Query("SELECT SUM(s.joursRestants) FROM SoldeConge s WHERE s.user = :user AND s.annee = :annee")
    Double getTotalJoursRestantsByUserAndAnnee(User user, Integer annee);

    // Return all solde entries for a given user
    List<SoldeConge> findAllByUser(User user);
}