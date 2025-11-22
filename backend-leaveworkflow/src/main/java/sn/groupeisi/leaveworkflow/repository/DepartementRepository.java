package sn.groupeisi.leaveworkflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.groupeisi.leaveworkflow.model.Departement;

import java.util.Optional;

@Repository
public interface DepartementRepository extends JpaRepository<Departement, Long> {
    Optional<Departement> findByLibelle(String libelle);
    long countByActiveTrue();
}