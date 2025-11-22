package sn.groupeisi.leaveworkflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.groupeisi.leaveworkflow.model.TypeConge;

import java.util.Optional;

@Repository
public interface TypeCongeRepository extends JpaRepository<TypeConge, Long> {
    Optional<TypeConge> findByLibelle(String libelle);
}