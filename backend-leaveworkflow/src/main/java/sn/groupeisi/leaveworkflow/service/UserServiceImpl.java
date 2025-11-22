package sn.groupeisi.leaveworkflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.groupeisi.leaveworkflow.dto.UserDto;
import sn.groupeisi.leaveworkflow.dto.ManagerStatsDto;
import sn.groupeisi.leaveworkflow.exception.DuplicateResourceException;
import sn.groupeisi.leaveworkflow.exception.InvalidDataException;
import sn.groupeisi.leaveworkflow.exception.ResourceNotFoundException;
import sn.groupeisi.leaveworkflow.model.Departement;
import sn.groupeisi.leaveworkflow.model.User;
import sn.groupeisi.leaveworkflow.repository.DepartementRepository;
import sn.groupeisi.leaveworkflow.repository.UserRepository;
import sn.groupeisi.leaveworkflow.repository.DemandeCongeRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service("customUserDetailsService")
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DepartementRepository departementRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemandeCongeRepository demandeCongeRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'email: " + username));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        log.debug("Fetching user by id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToDto(user);
    }

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        log.info("Creating new user with email: {}", userDto.getEmail());

        // Validate unique email
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new DuplicateResourceException("Un utilisateur avec cet email existe déjà");
        }

        // Validate unique matricule
        if (userRepository.existsByMatricule(userDto.getMatricule())) {
            throw new DuplicateResourceException("Un utilisateur avec ce matricule existe déjà");
        }

        // Validate password
        if (userDto.getPassword() == null || userDto.getPassword().trim().isEmpty()) {
            throw new InvalidDataException("Le mot de passe est requis pour la création d'un utilisateur");
        }

        // Fetch departement if provided
        Departement departement = null;
        if (userDto.getDepartementId() != null) {
            departement = departementRepository.findById(userDto.getDepartementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Departement", "id", userDto.getDepartementId()));
        }

        // Fetch manager if provided
        User manager = null;
        if (userDto.getManagerId() != null) {
            manager = userRepository.findById(userDto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", "id", userDto.getManagerId()));

            // Validate manager is not the same as employee
            if (manager.getId().equals(userDto.getId())) {
                throw new InvalidDataException("Un utilisateur ne peut pas être son propre manager");
            }
        }

        User user = User.builder()
                .prenom(userDto.getPrenom())
                .nom(userDto.getNom())
                .email(userDto.getEmail())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .telephone(userDto.getTelephone())
                .adresse(userDto.getAdresse())
                .role(userDto.getRole())
                .poste(userDto.getPoste())
                .matricule(userDto.getMatricule())
                .departement(departement)
                .manager(manager)
                .active(userDto.getActive() != null ? userDto.getActive() : true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully with id: {}", savedUser.getId());

        return mapToDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long id, UserDto userDto) {
        log.info("Updating user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Validate email uniqueness if changed
        if (userDto.getEmail() != null && !user.getEmail().equals(userDto.getEmail())) {
            if (userRepository.existsByEmail(userDto.getEmail())) {
                throw new DuplicateResourceException("Un utilisateur avec cet email existe déjà");
            }
            user.setEmail(userDto.getEmail());
        }

        // Validate matricule uniqueness if changed
        if (userDto.getMatricule() != null && !user.getMatricule().equals(userDto.getMatricule())) {
            if (userRepository.existsByMatricule(userDto.getMatricule())) {
                throw new DuplicateResourceException("Un utilisateur avec ce matricule existe déjà");
            }
            user.setMatricule(userDto.getMatricule());
        }

        // Update fields
        if (userDto.getPrenom() != null) user.setPrenom(userDto.getPrenom());
        if (userDto.getNom() != null) user.setNom(userDto.getNom());
        if (userDto.getTelephone() != null) user.setTelephone(userDto.getTelephone());
        if (userDto.getAdresse() != null) user.setAdresse(userDto.getAdresse());
        if (userDto.getRole() != null) user.setRole(userDto.getRole());
        if (userDto.getPoste() != null) user.setPoste(userDto.getPoste());

        // Update departement
        if (userDto.getDepartementId() != null) {
            Departement departement = departementRepository.findById(userDto.getDepartementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Departement", "id", userDto.getDepartementId()));
            user.setDepartement(departement);
        }

        // Update manager
        if (userDto.getManagerId() != null) {
            if (userDto.getManagerId().equals(id)) {
                throw new InvalidDataException("Un utilisateur ne peut pas être son propre manager");
            }
            User manager = userRepository.findById(userDto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", "id", userDto.getManagerId()));
            user.setManager(manager);
        }

        if (userDto.getActive() != null) user.setActive(userDto.getActive());

        // Update password only if provided
        if (userDto.getPassword() != null && !userDto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with id: {}", updatedUser.getId());

        return mapToDto(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Check if user is a manager for other users
        List<User> subordinates = userRepository.findByManagerId(id);
        if (!subordinates.isEmpty()) {
            throw new InvalidDataException(
                    String.format("Impossible de supprimer cet utilisateur car il est manager de %d employé(s)",
                            subordinates.size())
            );
        }

        userRepository.deleteById(id);
        log.info("User deleted successfully with id: {}", id);
    }

    @Override
    public UserDto mapToDto(User user) {
        if (user == null) return null;

        Long departementId = null;
        String departementLibelle = null;
        if (user.getDepartement() != null) {
            try {
                // Try to safely get id and libelle; fetch from repository to avoid lazy init
                Long dId = user.getDepartement().getId();
                departementId = dId;
                departementLibelle = departementRepository.findById(dId)
                        .map(Departement::getLibelle)
                        .orElse(null);
            } catch (Exception ex) {
                log.warn("Could not read departement from user entity, fetching lazily: {}", ex.getMessage());
                departementId = user.getDepartement() != null ? user.getDepartement().getId() : null;
                departementLibelle = null;
            }
        }

        Long managerId = null;
        String managerFullName = null;
        if (user.getManager() != null) {
            try {
                managerId = user.getManager().getId();
                managerFullName = user.getManager().getFullName();
            } catch (Exception ex) {
                log.warn("Could not read manager from user entity: {}", ex.getMessage());
                managerId = null;
                managerFullName = null;
            }
        }

        return UserDto.builder()
                .id(user.getId())
                .prenom(user.getPrenom())
                .nom(user.getNom())
                .email(user.getEmail())
                .telephone(user.getTelephone())
                .adresse(user.getAdresse())
                .role(user.getRole())
                .poste(user.getPoste())
                .matricule(user.getMatricule())
                .departementId(departementId)
                .departementLibelle(departementLibelle)
                .managerId(managerId)
                .managerFullName(managerFullName)
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public boolean isCurrentUser(Long id, org.springframework.security.core.Authentication authentication) {
        if (authentication == null) return false;
        String email = authentication.getName();
        return userRepository.findById(id)
                .map(u -> u.getEmail().equals(email))
                .orElse(false);
    }

    @Override
    public ManagerStatsDto getManagerStats(Long managerId) {
        // Total employees in manager's departement
        // Get manager entity
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", managerId));

        Long departementId = manager.getDepartement() != null ? manager.getDepartement().getId() : null;

        long totalEmployees = 0;
        long activeEmployees = 0;
        if (departementId != null) {
            totalEmployees = userRepository.countByDepartementId(departementId);
            activeEmployees = userRepository.findByDepartementId(departementId).stream().filter(u -> Boolean.TRUE.equals(u.getActive())).count();
        } else {
            // Fallback: count users who have this manager assigned
            List<User> subordinates = userRepository.findByManagerId(managerId);
            totalEmployees = subordinates.size();
            activeEmployees = subordinates.stream().filter(u -> Boolean.TRUE.equals(u.getActive())).count();
        }

        // Total demandes en attente for manager's team (where their subordinates' requests have statutManager = EN_ATTENTE)
        long totalDemandesEnAttente = demandeCongeRepository.findByUserManagerId(managerId).stream()
                .filter(d -> d.getStatutManager().name().equals("EN_ATTENTE"))
                .count();

        long totalAbsents = totalEmployees - activeEmployees;

        return new ManagerStatsDto(totalEmployees, activeEmployees, totalDemandesEnAttente, totalAbsents);
    }
}

