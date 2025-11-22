package sn.groupeisi.leaveworkflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.groupeisi.leaveworkflow.model.Absence;
import sn.groupeisi.leaveworkflow.model.User;

import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {
    List<Absence> findByUser(User user);
}

