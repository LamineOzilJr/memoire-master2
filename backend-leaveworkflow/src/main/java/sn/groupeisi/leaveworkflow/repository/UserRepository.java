package sn.groupeisi.leaveworkflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sn.groupeisi.leaveworkflow.enums.Role;
import sn.groupeisi.leaveworkflow.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByMatricule(String matricule);

    boolean existsByEmail(String email);

    boolean existsByMatricule(String matricule);

    List<User> findByRole(Role role);

    List<User> findByManagerId(Long managerId);

    List<User> findByDepartementId(Long departementId);

    /**
     * Counts users by their associated departement id.
     */
    long countByDepartementId(Long departementId);

    List<User> findByActiveTrue();

    @Query("SELECT u FROM User u WHERE u.manager.id = :managerId AND u.active = true")
    List<User> findActiveEmployeesByManager(Long managerId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    Long countByRole(Role role);
}