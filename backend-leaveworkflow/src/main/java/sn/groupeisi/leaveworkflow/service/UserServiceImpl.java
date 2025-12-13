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
import sn.groupeisi.leaveworkflow.model.Entreprise;
import sn.groupeisi.leaveworkflow.repository.DepartementRepository;
import sn.groupeisi.leaveworkflow.repository.UserRepository;
import sn.groupeisi.leaveworkflow.repository.DemandeCongeRepository;
import sn.groupeisi.leaveworkflow.repository.EntrepriseRepository;
import sn.groupeisi.leaveworkflow.util.MatriculeGenerator;
import sn.groupeisi.leaveworkflow.enums.Role;
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
    private final EntrepriseRepository entrepriseRepository;

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

        // Fetch entreprise if provided
        Entreprise entreprise = null;
        if (userDto.getEntrepriseId() != null) {
            entreprise = entrepriseRepository.findById(userDto.getEntrepriseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Entreprise", "id", userDto.getEntrepriseId()));
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

        // Generate or validate matricule
        String matricule = userDto.getMatricule();
        if (matricule == null || matricule.trim().isEmpty()) {
            // Auto-generate matricule based on role and enterprise
            Role role = userDto.getRole() != null ? userDto.getRole() : Role.SALARIE;
            Long entrepriseId = entreprise != null ? entreprise.getId() : null;

            if (entrepriseId != null) {
                // Compute the next sequence number by scanning existing users for this role in this enterprise
                List<User> existing = userRepository.findByEntrepriseIdAndRole(entrepriseId, role);
                long maxSeq = existing.stream()
                        .map(User::getMatricule)
                        .mapToLong(MatriculeGenerator::extractSequenceFromMatricule)
                        .max()
                        .orElse(0L);
                long nextSequence = maxSeq + 1;
                matricule = MatriculeGenerator.generateMatricule(role, nextSequence);

                // Verify uniqueness (defensive check in case of race condition)
                if (userRepository.existsByMatricule(matricule)) {
                    throw new DuplicateResourceException("Le matricule généré " + matricule + " existe déjà. Veuillez réessayer.");
                }
            } else {
                // If no enterprise provided, still generate but with default numbering
                long nextSequence = 1;
                matricule = MatriculeGenerator.generateMatricule(role, nextSequence);

                // Verify uniqueness
                if (userRepository.existsByMatricule(matricule)) {
                    throw new DuplicateResourceException("Le matricule généré " + matricule + " existe déjà. Veuillez réessayer.");
                }
            }
            log.info("Generated matricule: {} for role: {}", matricule, userDto.getRole());
        } else {
            // Validate unique matricule if provided - EVERY MATRICULE MUST BE UNIQUE IN DATABASE
            matricule = matricule.trim();
            if (userRepository.existsByMatricule(matricule)) {
                throw new DuplicateResourceException("Un utilisateur avec le matricule '" + matricule + "' existe déjà. Chaque matricule doit être unique dans la base de données.");
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
                .matricule(matricule)
                .departement(departement)
                .entreprise(entreprise)
                .manager(manager)
                .active(userDto.getActive() != null ? userDto.getActive() : true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully with id: {} and matricule: {}", savedUser.getId(), matricule);

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

        // Handle matricule update or auto-generation
        if (userDto.getMatricule() != null && !userDto.getMatricule().trim().isEmpty()) {
            // If matricule is provided and different from current
            if (!user.getMatricule().equals(userDto.getMatricule())) {
                if (userRepository.existsByMatricule(userDto.getMatricule())) {
                    throw new DuplicateResourceException("Un utilisateur avec ce matricule existe déjà");
                }
                user.setMatricule(userDto.getMatricule());
            }
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

        // Update entreprise
        if (userDto.getEntrepriseId() != null) {
            Entreprise entreprise = entrepriseRepository.findById(userDto.getEntrepriseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Entreprise", "id", userDto.getEntrepriseId()));
            user.setEntreprise(entreprise);
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
                .entrepriseId(user.getEntreprise() != null ? user.getEntreprise().getId() : null)
                .entrepriseLibelle(user.getEntreprise() != null ? user.getEntreprise().getLibelle() : null)
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
        log.debug("Fetching manager stats for manager id: {}", managerId);

        // Get manager entity
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", managerId));

        // Get the manager's enterprise
        Long entrepriseId = manager.getEntreprise() != null ? manager.getEntreprise().getId() : null;

        long totalEmployees = 0;
        long activeEmployees = 0;

        if (entrepriseId != null) {
            // Count employees only from the same enterprise
            // First approach: if manager has a department, count from that department in the same enterprise
            Long departementId = manager.getDepartement() != null ? manager.getDepartement().getId() : null;

            if (departementId != null) {
                // Count employees in the manager's department AND the same enterprise
                List<User> deptEmployees = userRepository.findByDepartementId(departementId);
                totalEmployees = deptEmployees.stream()
                        .filter(u -> u.getEntreprise() != null && u.getEntreprise().getId().equals(entrepriseId))
                        .count();
                activeEmployees = deptEmployees.stream()
                        .filter(u -> u.getEntreprise() != null && u.getEntreprise().getId().equals(entrepriseId))
                        .filter(u -> Boolean.TRUE.equals(u.getActive()))
                        .count();
            } else {
                // Fallback: count users who have this manager assigned AND the same enterprise
                List<User> subordinates = userRepository.findByManagerId(managerId);
                totalEmployees = subordinates.stream()
                        .filter(u -> u.getEntreprise() != null && u.getEntreprise().getId().equals(entrepriseId))
                        .count();
                activeEmployees = subordinates.stream()
                        .filter(u -> u.getEntreprise() != null && u.getEntreprise().getId().equals(entrepriseId))
                        .filter(u -> Boolean.TRUE.equals(u.getActive()))
                        .count();
            }
        } else {
            // If manager has no enterprise, count by department only
            Long departementId = manager.getDepartement() != null ? manager.getDepartement().getId() : null;
            if (departementId != null) {
                totalEmployees = userRepository.countByDepartementId(departementId);
                activeEmployees = userRepository.findByDepartementId(departementId).stream()
                        .filter(u -> Boolean.TRUE.equals(u.getActive()))
                        .count();
            } else {
                // Fallback: count users who have this manager assigned
                List<User> subordinates = userRepository.findByManagerId(managerId);
                totalEmployees = subordinates.size();
                activeEmployees = subordinates.stream()
                        .filter(u -> Boolean.TRUE.equals(u.getActive()))
                        .count();
            }
        }

        // Total demandes en attente for manager's team (where their subordinates' requests have statutManager = EN_ATTENTE)
        long totalDemandesEnAttente = demandeCongeRepository.findByUserManagerId(managerId).stream()
                .filter(d -> d.getStatutManager().name().equals("EN_ATTENTE"))
                .count();

        long totalAbsents = totalEmployees - activeEmployees;

        log.debug("Manager stats - Total: {}, Active: {}, Pending: {}, Absent: {}",
                totalEmployees, activeEmployees, totalDemandesEnAttente, totalAbsents);

        return new ManagerStatsDto(totalEmployees, activeEmployees, totalDemandesEnAttente, totalAbsents);
    }

    @Override
    public Long getMaxMatriculeSequence(Long entrepriseId, Role role) {
        log.debug("Getting max matricule sequence for entrepriseId: {}, role: {}", entrepriseId, role);
        return userRepository.getMaxMatriculeSequence(entrepriseId, role);
    }
}
