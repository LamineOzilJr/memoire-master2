package sn.groupeisi.leaveworkflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.groupeisi.leaveworkflow.dto.SoldeResponse;
import sn.groupeisi.leaveworkflow.model.SoldeConge;
import sn.groupeisi.leaveworkflow.model.TypeConge;
import sn.groupeisi.leaveworkflow.model.User;
import sn.groupeisi.leaveworkflow.repository.SoldeCongeRepository;
import sn.groupeisi.leaveworkflow.repository.TypeCongeRepository;
import sn.groupeisi.leaveworkflow.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SoldeCongeService {

    private final SoldeCongeRepository soldeCongeRepository;
    private final UserRepository userRepository;
    private final TypeCongeRepository typeCongeRepository;

    @Value("${leave.monthly-accrual:2}")
    private double monthlyAccrual;

    @Value("${leave.max-carryover:10}")
    private int maxCarryover;

    @Value("${leave.annual-days:24}")
    private double annualDays;

    @Transactional
    public void initializeAnnualBalance(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        int currentYear = Year.now().getValue();
        List<TypeConge> types = typeCongeRepository.findAll();

        for (TypeConge type : types) {
            SoldeConge solde = soldeCongeRepository.findByUserAndTypeCongeAndAnnee(user, type, currentYear)
                    .orElseGet(() -> {
                        // Initialize solde based on leave type's max_jours
                        double initialDays = 0.0;

                        // If the type has a max_jours defined (e.g., Annuel=24), use that
                        // Otherwise, leave types with unlimited days (max_jours=null) start at 0
                        if (type.getMaxJours() != null && type.getMaxJours() > 0) {
                            initialDays = type.getMaxJours().doubleValue();
                        }

                        return SoldeConge.builder()
                                .user(user)
                                .typeConge(type)
                                .annee(currentYear)
                                .joursAcquis(initialDays)
                                .joursPris(0.0)
                                .joursRestants(initialDays)
                                .build();
                    });

            // Carryover from previous year: add to joursAcquis and joursRestants
            soldeCongeRepository.findByUserAndTypeCongeAndAnnee(user, type, currentYear - 1)
                    .ifPresent(prev -> {
                        double carry = Math.min(type.getMaxJours() != null ? type.getMaxJours() : 0,
                                                prev.getJoursRestants() == null ? 0.0 : prev.getJoursRestants());
                        solde.setJoursAcquis(solde.getJoursAcquis() + carry);
                        // If this is a newly created solde, joursRestants was set to initialDays; increase it by carry
                        if (solde.getJoursRestants() == null) {
                            solde.setJoursRestants(solde.getJoursAcquis());
                        } else {
                            solde.setJoursRestants(solde.getJoursRestants() + carry);
                        }
                    });

            // Ensure consistency: joursRestants = joursAcquis - joursPris
            if (solde.getJoursPris() == null) {
                solde.setJoursPris(0.0);
            }
            solde.setJoursRestants(solde.getJoursAcquis() - solde.getJoursPris());

            System.out.println("📝 SOLDE INITIALIZATION: User " + userId + " | Type: " + type.getLibelle() +
                              " | Max Jours: " + type.getMaxJours() + " | Jours Acquis: " + solde.getJoursAcquis() +
                              " | Jours Restants: " + solde.getJoursRestants());

            soldeCongeRepository.save(solde);
        }
    }

    @Transactional
    public void deductLeaveBalance(Long userId, Long typeCongeId, LocalDate dateDebut, LocalDate dateFin) {
        User user = userRepository.findById(userId).orElseThrow();
        TypeConge type = typeCongeRepository.findById(typeCongeId).orElseThrow();
        int year = dateDebut.getYear();

        // Try to find existing solde; if missing, attempt to initialize balances and retry.
        var opt = soldeCongeRepository.findByUserAndTypeCongeAndAnnee(user, type, year);
        if (opt.isEmpty()) {
            // Initialize balances for current year and try again
            initializeAnnualBalance(userId);
            opt = soldeCongeRepository.findByUserAndTypeCongeAndAnnee(user, type, year);
            if (opt.isEmpty()) {
                throw new RuntimeException("Solde non trouvé pour l'utilisateur/année après initialisation");
            }
        }

        SoldeConge solde = opt.get();

        // Validate dates
        if (dateFin.isBefore(dateDebut)) {
            throw new RuntimeException("Dates invalides: dateFin est antérieure à dateDebut");
        }

        // Compute working days (excluding weekends: Saturday=6, Sunday=7)
        long workingDays = 0;
        LocalDate current = dateDebut;
        while (!current.isAfter(dateFin)) {
            int dayOfWeek = current.getDayOfWeek().getValue(); // Monday=1, ..., Saturday=6, Sunday=7
            if (dayOfWeek < 6) { // Exclude Saturday (6) and Sunday (7)
                workingDays++;
            }
            current = current.plusDays(1);
        }

        if (workingDays <= 0) {
            throw new RuntimeException("Pas de jours ouvrables dans la période: dateDebut=" + dateDebut + ", dateFin=" + dateFin);
        }
        double days = (double) workingDays;

        // Log dates and computed days
        System.out.println("📅 DateDebut: " + dateDebut + " | DateFin: " + dateFin + " | Working days (excluding weekends): " + workingDays);
        System.out.println("📝 Leave Type: " + type.getLibelle() + " | Max Jours: " + type.getMaxJours());

        // Log BEFORE deduction including persisted identifiers
        System.out.println("📊 SOLDE DEDUCTION - BEFORE (id=" + solde.getId() + ", userId=" + solde.getUser().getId() + ", typeId=" + solde.getTypeConge().getId() + ", annee=" + solde.getAnnee() + ")");
        System.out.println("   Jours acquis: " + solde.getJoursAcquis());
        System.out.println("   Jours pris (avant): " + solde.getJoursPris());
        System.out.println("   Jours restants (avant): " + solde.getJoursRestants());

        // IMPORTANT: The shared pool (jours_acquis) is stored in the ANNUEL type (type_conge_id=1)
        // All leave types share this same 24-day pool
        TypeConge annuelType = typeCongeRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Annuel type (ID 1) not found"));
        SoldeConge annuelSolde = soldeCongeRepository.findByUserAndTypeCongeAndAnnee(user, annuelType, year)
                .orElseThrow(() -> new RuntimeException("Annuel solde not found for user"));

        double sharedPool = annuelSolde.getJoursAcquis();
        System.out.println("📊 SHARED POOL (from Annuel type): " + sharedPool + " days");

        // Calculate total joursPris across ALL leave types for this user
        List<SoldeConge> allUserSoldes = soldeCongeRepository.findAllByUser(user);
        double totalJoursPrisAcrossAllTypes = 0.0;

        for (SoldeConge s : allUserSoldes) {
            // Only count if same year
            if (s.getAnnee().equals(year)) {
                totalJoursPrisAcrossAllTypes += (s.getJoursPris() != null ? s.getJoursPris() : 0.0);
            }
        }

        System.out.println("📊 SHARED POOL ANALYSIS:");
        System.out.println("   Total joursPris across ALL leave types (before): " + totalJoursPrisAcrossAllTypes);
        System.out.println("   New days to add: " + days);

        // Calculate new total after this deduction
        double newTotalJoursPris = totalJoursPrisAcrossAllTypes + days;
        System.out.println("   Total joursPris after deduction: " + newTotalJoursPris);
        System.out.println("   Shared pool (jours_acquis): " + sharedPool);

        // Check if total pool is sufficient
        if (newTotalJoursPris > sharedPool) {
            throw new RuntimeException("Solde insuffisant: " + newTotalJoursPris + " > " + sharedPool);
        }

        // Update THIS solde row: add days to its joursPris
        double newJoursPrisThisType = solde.getJoursPris() + days;

        // Calculate new jours_restants for THIS type
        // jours_restants = shared_pool - total_joursPris_across_all_types
        double newJoursRestants = sharedPool - newTotalJoursPris;

        System.out.println("📊 SOLDE CALCULATION FOR THIS TYPE:");
        System.out.println("   This type joursPris: " + solde.getJoursPris() + " + " + days + " = " + newJoursPrisThisType);
        System.out.println("   FORMULA: jours_restants = shared_pool - total_joursPris_all_types");
        System.out.println("   CALCULATION: " + sharedPool + " - " + newTotalJoursPris + " = " + newJoursRestants);

        solde.setJoursPris(newJoursPrisThisType);
        solde.setJoursRestants(newJoursRestants);

        // Update ALL solde rows for this user to reflect the new shared pool status
        System.out.println("\n📊 UPDATING ALL SOLDE ROWS FOR USER " + userId + ":");
        for (SoldeConge s : allUserSoldes) {
            if (s.getAnnee().equals(year)) {
                // Each type maintains its own joursPris, but all share the same jours_restants calculation
                double updatedJoursRestants = sharedPool - newTotalJoursPris;

                if (!s.getId().equals(solde.getId())) {
                    // Update other types
                    System.out.println("   Type: " + s.getTypeConge().getLibelle() + " | joursPris: " + s.getJoursPris() + " | NEW jours_restants: " + updatedJoursRestants);
                    s.setJoursRestants(updatedJoursRestants);
                    soldeCongeRepository.save(s);
                } else {
                    // This is the one we're updating
                    System.out.println("   Type: " + s.getTypeConge().getLibelle() + " (UPDATED) | joursPris: " + newJoursPrisThisType + " | NEW jours_restants: " + newJoursRestants);
                }
            }
        }

        // Save the current solde
        SoldeConge savedSolde = soldeCongeRepository.save(solde);
        soldeCongeRepository.flush();

        // Re-fetch to be absolutely sure we read persisted state
        SoldeConge persisted = soldeCongeRepository.findById(savedSolde.getId())
                .orElseThrow(() -> new RuntimeException("Solde introuvable après sauvegarde"));

        // Log AFTER deduction with verification including identifiers and formula
        System.out.println("\n📊 SOLDE DEDUCTION - AFTER (id=" + persisted.getId() + ", userId=" + persisted.getUser().getId() + ", typeId=" + persisted.getTypeConge().getId() + ", annee=" + persisted.getAnnee() + ")");
        System.out.println("   Jours acquis: " + persisted.getJoursAcquis());
        System.out.println("   Jours pris (après): " + persisted.getJoursPris());
        System.out.println("   Jours restants (après): " + persisted.getJoursRestants());
        System.out.println("   ✅ Solde successfully updated and persisted!");
    }

    public List<SoldeResponse> getSoldesByUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return soldeCongeRepository.findAllByUser(user).stream()
                .map(this::mapToSoldeResponse)
                .collect(Collectors.toList());
    }

    private SoldeResponse mapToSoldeResponse(SoldeConge solde) {
        return SoldeResponse.builder()
                .id(solde.getId())
                .userId(solde.getUser().getId())
                .typeCongeId(solde.getTypeConge().getId())
                .typeCongeLibelle(solde.getTypeConge().getLibelle())
                .annee(solde.getAnnee())
                .joursAcquis(solde.getJoursAcquis())
                .joursPris(solde.getJoursPris())
                .joursRestants(solde.getJoursRestants())
                .build();
    }

    /**
     * Returns the total leave balance (jours restants) for the main annual leave type.
     * Applies the rule: before 1 year of service => 0 days; after >= 1 year => use actual solde
     */
    public Double getTotalSoldeConsideringSeniority(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        LocalDateTime createdAt = user.getCreatedAt();
        if (createdAt == null) {
            System.out.println("👤 User " + userId + " has no creation date. Returning 0 days.");
            return 0.0;
        }

        long years = ChronoUnit.YEARS.between(createdAt.toLocalDate(), LocalDate.now());
        if (years < 1) {
            System.out.println("👤 User " + userId + " has less than 1 year of service (" + years + " years). Returning 0 days.");
            return 0.0;
        }

        int currentYear = Year.now().getValue();

        // Get the main annual leave type (ID 1)
        // This is the primary leave balance users see
        TypeConge mainLeaveType = typeCongeRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Main leave type (ID 1) not found"));

        var soldeOpt = soldeCongeRepository.findByUserAndTypeCongeAndAnnee(user, mainLeaveType, currentYear);

        System.out.println("\n📊 TOTAL SOLDE CALCULATION FOR USER ID: " + userId);
        System.out.println("   Current Year: " + currentYear);
        System.out.println("   Years of Service: " + years + " years");
        System.out.println("   Leave Type: " + mainLeaveType.getLibelle() + " (ID: " + mainLeaveType.getId() + ")");

        if (soldeOpt.isEmpty()) {
            // No solde yet created for this year; by business rule, grant annual default
            System.out.println("   No solde found for this year. Returning annual default: " + annualDays + " days");
            return annualDays;
        }

        Double total = soldeOpt.get().getJoursRestants();
        System.out.println("   Jours acquis: " + soldeOpt.get().getJoursAcquis());
        System.out.println("   Jours pris: " + soldeOpt.get().getJoursPris());
        System.out.println("   Jours restants: " + total);
        System.out.println("   ✅ Final total returned: " + total + " days");

        return total;
    }
}