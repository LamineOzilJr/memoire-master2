package sn.groupeisi.leaveworkflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sn.groupeisi.leaveworkflow.dto.*;
import sn.groupeisi.leaveworkflow.enums.StatutManager;
import sn.groupeisi.leaveworkflow.enums.StatutRh;
import sn.groupeisi.leaveworkflow.enums.StatutChefService;
import sn.groupeisi.leaveworkflow.enums.StatutDg;
import sn.groupeisi.leaveworkflow.enums.Role;
import sn.groupeisi.leaveworkflow.model.DemandeConge;
import sn.groupeisi.leaveworkflow.model.TypeConge;
import sn.groupeisi.leaveworkflow.model.User;
import sn.groupeisi.leaveworkflow.repository.DemandeCongeRepository;
import sn.groupeisi.leaveworkflow.repository.TypeCongeRepository;
import sn.groupeisi.leaveworkflow.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class DemandeCongeService {

    private final DemandeCongeRepository demandeCongeRepository;
    private final UserRepository userRepository;
    private final TypeCongeRepository typeCongeRepository;
    private final FileStorageService fileStorageService;
    private final SoldeCongeService soldeCongeService;
    private final NotificationService notificationService;
    private final AbsenceService absenceService;
    private final EmailService emailService;

    @Transactional
    public DemandeResponse createDemande(Long userId, DemandeRequest request, MultipartFile justificatif) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        TypeConge typeConge = typeCongeRepository.findById(request.getTypeCongeId())
                .orElseThrow(() -> new RuntimeException("Type de congé non trouvé"));

        // Check for overlapping requests
        List<DemandeConge> overlapping = demandeCongeRepository.findOverlappingRequests(
                userId, request.getDateDebut(), request.getDateFin()
        );
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Vous avez déjà une demande validée pour cette période");
        }

        // Create demande
        DemandeConge demande = DemandeConge.builder()
                .user(user)
                .typeConge(typeConge)
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .motif(request.getMotif())
                .statutManager(StatutManager.EN_ATTENTE)
                .statutRh(StatutRh.EN_ATTENTE)
                .statutChefService(StatutChefService.EN_ATTENTE)
                .statutDg(StatutDg.EN_ATTENTE)
                .build();

        // Handle file upload
        if (justificatif != null && !justificatif.isEmpty()) {
            String fileName = fileStorageService.storeFile(justificatif);
            demande.setJustificatif(fileName);
        }

        DemandeConge savedDemande = demandeCongeRepository.save(demande);

        // Send notifications
        String emailStatus = "ℹ️ NO_MANAGER";
        String emailErrorMessage = "";

        if (user.getManager() != null) {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("📨 SENDING NOTIFICATIONS FOR NEW LEAVE REQUEST (ID: " + savedDemande.getId() + ")");
            System.out.println("=".repeat(80));

            // Send email to manager
            emailStatus = "❌ FAILED";
            try {
                emailService.sendNewDemandeToManagerEmail(
                        user.getManager().getEmail(),
                        user.getManager().getFullName(),
                        user.getFullName(),
                        typeConge.getLibelle(),
                        request.getDateDebut().toString(),
                        request.getDateFin().toString()
                );
                System.out.println("✅ EMAIL SUCCESSFULLY SENT to manager: " + user.getManager().getEmail());
                System.out.println("   Manager Name: " + user.getManager().getFullName());
                System.out.println("   Employee: " + user.getFullName());
                emailStatus = "✅ SENT";
            } catch (Exception e) {
                System.err.println("❌ EMAIL FAILED: " + e.getClass().getSimpleName());
                System.err.println("   Error: " + e.getMessage());
                emailErrorMessage = e.getMessage();
                e.printStackTrace();
            }

            // Print detailed notification for manager
            printDemandeForManager(savedDemande.getId());

            // Create in-app notification (always, regardless of email)
            System.out.println("🔔 Creating IN-APP NOTIFICATION for managerId=" + user.getManager().getId());
            try {
                String notificationMessage = String.format("Nouvelle demande de congé de %s du %s au %s",
                        user.getFullName(),
                        request.getDateDebut().format(DateTimeFormatter.ISO_DATE),
                        request.getDateFin().format(DateTimeFormatter.ISO_DATE));

                if (!emailErrorMessage.isEmpty()) {
                    notificationMessage += "\n⚠️ Email non envoyé: " + emailErrorMessage;
                }

                notificationService.createNotification(
                        user.getManager(),
                        "Nouvelle demande de congé",
                        notificationMessage,
                        savedDemande.getId()
                );
                System.out.println("✅ IN-APP NOTIFICATION SAVED to database");
            } catch (Exception e) {
                System.err.println("❌ IN-APP NOTIFICATION FAILED: " + e.getMessage());
            }

            System.out.println("=".repeat(80));
            System.out.println("Summary: Email=" + emailStatus + " | Notification=SAVED");
            System.out.println("=".repeat(80) + "\n");
        } else {
            System.out.println("⚠️  No manager assigned for user id=" + user.getId() + ", skipping notification");
        }

        DemandeResponse response = mapToDemandeResponse(savedDemande);
        // Attach email status info for frontend
        response.setEmailStatus(emailStatus);
        response.setEmailError(emailErrorMessage);

        return response;
    }

    @Transactional
    public DemandeResponse updateDemandeByManager(Long demandeId, UpdateDemandeManagerRequest request) {
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        // Check if manager is authorized
        if (!demande.getUser().getManager().getId().equals(request.getManagerId())) {
            throw new RuntimeException("Non autorisé à traiter cette demande");
        }

        demande.setStatutManager(request.getStatutManager());
        demande.setCommentaireManager(request.getCommentaire());
        demande.setDateTraitementManager(LocalDateTime.now());

        DemandeConge updatedDemande = demandeCongeRepository.save(demande);

        // Send notification to employee
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📨 MANAGER DECISION - SENDING NOTIFICATIONS (Request ID: " + demandeId + ")");
        System.out.println("=".repeat(80));
        System.out.println("Decision: " + request.getStatutManager());
        System.out.println("Employee: " + demande.getUser().getFullName() + " (" + demande.getUser().getEmail() + ")");

        String emailToEmployeeStatus = "❌ FAILED";
        String emailToEmployeeError = "";
        try {
            // Send email to employee about manager decision
            emailService.sendManagerDecisionEmail(
                    demande.getUser().getEmail(),
                    demande.getUser().getFullName(),
                    demande.getTypeConge().getLibelle(),
                    demande.getDateDebut().toString(),
                    demande.getDateFin().toString(),
                    request.getStatutManager().toString(),
                    request.getCommentaire()
            );
            System.out.println("✅ EMAIL SENT to employee: " + demande.getUser().getEmail());
            emailToEmployeeStatus = "✅ SENT";
        } catch (Exception e) {
            System.err.println("❌ EMAIL TO EMPLOYEE FAILED: " + e.getMessage());
            emailToEmployeeError = e.getMessage();
            e.printStackTrace();
        }

        try {
            String notificationMessage = String.format("Votre demande de congé du %s au %s a été %s",
                    demande.getDateDebut().format(DateTimeFormatter.ISO_DATE),
                    demande.getDateFin().format(DateTimeFormatter.ISO_DATE),
                    request.getStatutManager().toString());

            if (!emailToEmployeeError.isEmpty()) {
                notificationMessage += "\n⚠️ Email non envoyé: " + emailToEmployeeError;
            }

            notificationService.createNotification(
                    demande.getUser(),
                    "Décision du manager sur votre demande de congé",
                    notificationMessage,
                    demande.getId()
            );
            System.out.println("✅ IN-APP NOTIFICATION SAVED for employee");
        } catch (Exception e) {
            System.err.println("❌ IN-APP NOTIFICATION FAILED: " + e.getMessage());
        }

        // If approved by manager, notify RH
        if (request.getStatutManager() == StatutManager.APPROUVE) {
            System.out.println("\n📨 REQUEST APPROVED - NOTIFYING RH TEAM");
            List<User> rhUsers = userRepository.findByRole(Role.SERVICE_RH);
            System.out.println("Found " + rhUsers.size() + " RH users");

            for (User rhUser : rhUsers) {
                String emailToRhStatus = "❌ FAILED";
                String emailToRhError = "";
                try {
                    // Send email to RH about new request
                    emailService.sendNewDemandeToRhEmail(
                            rhUser.getEmail(),
                            rhUser.getFullName(),
                            demande.getUser().getFullName(),
                            demande.getTypeConge().getLibelle(),
                            demande.getDateDebut().toString(),
                            demande.getDateFin().toString()
                    );
                    System.out.println("✅ EMAIL SENT to RH: " + rhUser.getEmail());
                    emailToRhStatus = "✅ SENT";
                } catch (Exception e) {
                    System.err.println("❌ EMAIL TO RH FAILED for " + rhUser.getEmail() + ": " + e.getMessage());
                    emailToRhError = e.getMessage();
                }

                try {
                    String notificationMessage = String.format("Demande de congé de %s du %s au %s approuvée par le manager",
                            demande.getUser().getFullName(),
                            demande.getDateDebut().format(DateTimeFormatter.ISO_DATE),
                            demande.getDateFin().format(DateTimeFormatter.ISO_DATE));

                    if (!emailToRhError.isEmpty()) {
                        notificationMessage += "\n⚠️ Email non envoyé: " + emailToRhError;
                    }

                    notificationService.createNotification(
                            rhUser,
                            "Nouvelle demande de congé à traiter",
                            notificationMessage,
                            demande.getId()
                    );
                    System.out.println("✅ IN-APP NOTIFICATION SAVED for RH: " + rhUser.getFullName());
                } catch (Exception e) {
                    System.err.println("❌ IN-APP NOTIFICATION FAILED for RH: " + e.getMessage());
                }
            }
        }

        System.out.println("=".repeat(80));
        System.out.println("Summary: Email to Employee=" + emailToEmployeeStatus + " | Notifications=SAVED");
        System.out.println("=".repeat(80) + "\n");

        DemandeResponse resp = mapToDemandeResponse(updatedDemande);
        resp.setEmailStatus(emailToEmployeeStatus);
        resp.setEmailError(emailToEmployeeError);
        return resp;
    }

    @Transactional
    public DemandeResponse updateDemandeByRh(Long demandeId, UpdateDemandeRhRequest request) {
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        // Verify that the request was approved by manager
        if (demande.getStatutManager() != StatutManager.APPROUVE) {
            System.err.println("❌ ERROR: Request not approved by manager. Current status: " + demande.getStatutManager());
            throw new RuntimeException("La demande doit d'abord être approuvée par le manager");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔄 RH UPDATING DEMANDE #" + demandeId);
        System.out.println("   Current RH Status: " + demande.getStatutRh());
        System.out.println("   New RH Status: " + request.getStatutRh());
        System.out.println("   Comment: " + request.getCommentaire());
        System.out.println("=".repeat(80));

        // Capture previous RH status to avoid double-deductions
        StatutRh previousRhStatus = demande.getStatutRh();

        demande.setStatutRh(request.getStatutRh());
        demande.setCommentaireRh(request.getCommentaire());
        demande.setDateTraitementRh(LocalDateTime.now());

        DemandeConge updatedDemande = demandeCongeRepository.save(demande);
        demandeCongeRepository.flush();  // Force immediate persistence

        System.out.println("✅ Demande status updated: " + updatedDemande.getStatutRh());

        // RH validation: only update status and notify chef_service
        // Solde deduction and user deactivation will be done by DG after final validation
        if (request.getStatutRh() == StatutRh.VALIDER && previousRhStatus != StatutRh.VALIDER) {
            System.out.println("🔍 RH VALIDATION - Request approved, routing to CHEF_SERVICE");
            System.out.println("   ⏳ Solde deduction and user deactivation will be done by DG only");
        }

        // Send notification to employee
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📨 RH DECISION - SENDING NOTIFICATIONS (Request ID: " + demandeId + ")");
        System.out.println("=".repeat(80));
        System.out.println("Decision: " + request.getStatutRh());
        System.out.println("Employee: " + demande.getUser().getFullName() + " (" + demande.getUser().getEmail() + ")");

        String emailToEmployeeStatus = "❌ FAILED";
        String emailToEmployeeError = "";
        try {
            // Send email to employee about RH decision
            emailService.sendRhDecisionEmail(
                    demande.getUser().getEmail(),
                    demande.getUser().getFullName(),
                    demande.getTypeConge().getLibelle(),
                    demande.getDateDebut().toString(),
                    demande.getDateFin().toString(),
                    request.getStatutRh().toString(),
                    request.getCommentaire()
            );
            System.out.println("✅ EMAIL SENT to employee: " + demande.getUser().getEmail());
            emailToEmployeeStatus = "✅ SENT";
        } catch (Exception e) {
            System.err.println("❌ EMAIL TO EMPLOYEE FAILED: " + e.getMessage());
            emailToEmployeeError = e.getMessage();
        }

        try {
            String notificationMessage = String.format("Votre demande de congé du %s au %s a été %s par RH",
                    demande.getDateDebut().format(DateTimeFormatter.ISO_DATE),
                    demande.getDateFin().format(DateTimeFormatter.ISO_DATE),
                    request.getStatutRh().toString());

            if (!emailToEmployeeError.isEmpty()) {
                notificationMessage += "\n⚠️ Email non envoyé: " + emailToEmployeeError;
            }

            notificationService.createNotification(
                    demande.getUser(),
                    "Décision RH sur votre demande de congé",
                    notificationMessage,
                    demande.getId()
            );
            System.out.println("✅ IN-APP NOTIFICATION SAVED for employee");
        } catch (Exception e) {
            System.err.println("❌ IN-APP NOTIFICATION FAILED: " + e.getMessage());
        }

        System.out.println("=".repeat(80));
        System.out.println("Summary: Email=" + emailToEmployeeStatus + " | Notification=SAVED");
        System.out.println("=".repeat(80) + "\n");

        DemandeResponse resp = mapToDemandeResponse(updatedDemande);
        resp.setEmailStatus(emailToEmployeeStatus);
        resp.setEmailError(emailToEmployeeError);
        return resp;
    }

    public List<DemandeResponse> getDemandesByUser(Long userId) {
        return demandeCongeRepository.findByUserId(userId).stream()
                .map(this::mapToDemandeResponse)
                .collect(Collectors.toList());
    }

    public DemandeResponse getDemandeById(Long demandeId) {
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        return mapToDemandeResponse(demande);
    }

    public List<DemandeResponse> getDemandesForManager(Long managerId) {
        List<User> subordinates = userRepository.findByManagerId(managerId);
        return subordinates.stream()
                .flatMap(user -> demandeCongeRepository.findByUserId(user.getId()).stream())
                .map(this::mapToDemandeResponse)
                .collect(Collectors.toList());
    }

    public void printDemandeForManager(Long demandeId) {
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📋 DEMANDE DE CONGÉ - NOTIFICATION MANAGER");
        System.out.println("=".repeat(60));
        System.out.println("🆔 ID Demande: " + demande.getId());
        System.out.println("👤 Employé: " + demande.getUser().getFullName());
        System.out.println("📧 Email: " + demande.getUser().getEmail());
        System.out.println("🏢 Poste: " + demande.getUser().getPoste());
        System.out.println("🏷️  Type de congé: " + demande.getTypeConge().getLibelle());
        System.out.println("📅 Date début: " + demande.getDateDebut());
        System.out.println("📅 Date fin: " + demande.getDateFin());
        System.out.println("💬 Motif: " + (demande.getMotif() != null ? demande.getMotif() : "Aucun motif spécifié"));
        System.out.println("📎 Justificatif: " + (demande.getJustificatif() != null ? "Oui (" + demande.getJustificatif() + ")" : "Non"));
        System.out.println("⏰ Date de création: " + demande.getDateCreation());
        System.out.println("📊 Statut Manager: " + demande.getStatutManager());
        System.out.println("📊 Statut RH: " + demande.getStatutRh());
        
        if (demande.getUser().getManager() != null) {
            System.out.println("👨‍💼 Manager responsable: " + demande.getUser().getManager().getFullName());
            System.out.println("📧 Email manager: " + demande.getUser().getManager().getEmail());
        }
        
        System.out.println("=".repeat(60));
        System.out.println("⚠️  ACTION REQUISE: Cette demande nécessite votre approbation");
        System.out.println("=".repeat(60) + "\n");
    }

    private DemandeResponse mapToDemandeResponse(DemandeConge demande) {
        long nombreJours = demande.getDateDebut().until(demande.getDateFin().plusDays(1), java.time.temporal.ChronoUnit.DAYS);

        // Check for overlaps in same department
        boolean hasOverlap = false;
        if (demande.getUser().getDepartement() != null) {
            List<DemandeConge> overlappingDemands = demandeCongeRepository.findOverlappingDemandsInDepartment(
                    demande.getUser().getDepartement().getId(),
                    demande.getDateDebut(),
                    demande.getDateFin()
            );
            // Filter out current demand and check if any others exist
            hasOverlap = overlappingDemands.stream()
                    .anyMatch(d -> !d.getId().equals(demande.getId()));
        }

        return DemandeResponse.builder()
                .id(demande.getId())
                .userId(demande.getUser().getId())
                .userName(demande.getUser().getFullName())
                .typeCongeId(demande.getTypeConge().getId())
                .typeCongeLibelle(demande.getTypeConge().getLibelle())
                .dateDebut(demande.getDateDebut())
                .dateFin(demande.getDateFin())
                .nombreJours(nombreJours)
                .motif(demande.getMotif())
                .justificatif(demande.getJustificatif())
                .statutManager(demande.getStatutManager())
                .commentaireManager(demande.getCommentaireManager())
                .dateTraitementManager(demande.getDateTraitementManager())
                .statutRh(demande.getStatutRh())
                .commentaireRh(demande.getCommentaireRh())
                .dateTraitementRh(demande.getDateTraitementRh())
                .statutChefService(demande.getStatutChefService())
                .commentaireChefService(demande.getCommentaireChefService())
                .dateTraitementChefService(demande.getDateTraitementChefService())
                .statutDg(demande.getStatutDg())
                .commentaireDg(demande.getCommentaireDg())
                .dateTraitementDg(demande.getDateTraitementDg())
                .dateCreation(demande.getDateCreation())
                .hasOverlap(hasOverlap)
                .build();
    }

    @Transactional
    public DemandeResponse modifyPendingDemande(Long demandeId, DemandeRequest request, MultipartFile justificatif) {
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        if (demande.getStatutManager() != StatutManager.EN_ATTENTE || demande.getStatutRh() != StatutRh.EN_ATTENTE) {
            throw new RuntimeException("Seules les demandes en attente peuvent être modifiées");
        }

        demande.setDateDebut(request.getDateDebut());
        demande.setDateFin(request.getDateFin());
        demande.setMotif(request.getMotif());
        if (justificatif != null) {
            String newFile = fileStorageService.storeFile(justificatif);
            demande.setJustificatif(newFile);
        }

        DemandeConge updated = demandeCongeRepository.save(demande);

        // Re-notify manager
        if (demande.getUser().getManager() != null) {
            try {
                // Temporarily disabled email sending
                // emailService.sendNewDemandeToManagerEmail(...);
                System.out.println("📧 EMAIL NOTIFICATION (DISABLED): Modified request notification sent to manager " + 
                                 demande.getUser().getManager().getFullName());
            } catch (Exception e) {
                System.err.println("Email notification failed: " + e.getMessage());
            }
        }

        return mapToDemandeResponse(updated);
    }

    @Transactional
    public void cancelPendingDemande(Long demandeId) {
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        if (demande.getStatutManager() != StatutManager.EN_ATTENTE || demande.getStatutRh() != StatutRh.EN_ATTENTE) {
            throw new RuntimeException("Seules les demandes en attente peuvent être annulées");
        }

        demandeCongeRepository.delete(demande);

        // Notify manager/RH if needed
    }

    // Add these methods to your existing DemandeCongeService class

    /**
     * Check if user is authorized to view a demande
     */
    public boolean isAuthorizedToView(Long demandeId, Long userId, String role) {
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        // Owner can always view
        if (demande.getUser().getId().equals(userId)) {
            return true;
        }

        // Manager can view their subordinates' requests
        if ("MANAGER".equals(role) && demande.getUser().getManager() != null && demande.getUser().getManager().getId().equals(userId)) {
            return true;
        }

        // RH and ADMIN can view all
        if ("SERVICE_RH".equals(role) || "ADMIN".equals(role)) {
            return true;
        }

        // Also support ROLE_ prefixed versions
        if ("ROLE_MANAGER".equals(role) && demande.getUser().getManager() != null && demande.getUser().getManager().getId().equals(userId)) {
            return true;
        }

        return "ROLE_SERVICE_RH".equals(role) || "ROLE_ADMIN".equals(role);
    }

    /**
     * Check if user is the owner of a demande
     */
    public boolean isUserOwner(Long demandeId, Long userId) {
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        return demande.getUser().getId().equals(userId);
    }

    /**
     * Get all pending requests for RH (approved by manager) in the user's enterprise
     */
    public List<DemandeResponse> getDemandesForRh(Long entrepriseId) {
        return demandeCongeRepository.findByStatutManagerApprouveByEntreprise(entrepriseId).stream()
                .map(this::mapToDemandeResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all pending requests for ChefService (validated by RH, pending by chef_service) in the user's enterprise
     */
    public List<DemandeResponse> getDemandesForChefService(Long entrepriseId) {
        return demandeCongeRepository.findByStatutChefServicePendingByEntreprise(entrepriseId).stream()
                .map(this::mapToDemandeResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all pending requests for DG (validated by chef_service, pending by dg) in the user's enterprise
     */
    public List<DemandeResponse> getDemandesForDg(Long entrepriseId) {
        return demandeCongeRepository.findByStatutDgPendingByEntreprise(entrepriseId).stream()
                .map(this::mapToDemandeResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DemandeResponse updateDemandeByChefService(Long demandeId, UpdateDemandeChefServiceRequest request) {
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        demande.setStatutChefService(request.getStatutChefService());
        demande.setCommentaireChefService(request.getCommentaire());
        demande.setDateTraitementChefService(LocalDateTime.now());

        DemandeConge updatedDemande = demandeCongeRepository.save(demande);
        demandeCongeRepository.flush();

        // Send notification to employee
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📨 CHEF_SERVICE DECISION - SENDING NOTIFICATIONS (Request ID: " + demandeId + ")");
        System.out.println("=".repeat(80));
        System.out.println("Decision: " + request.getStatutChefService());
        System.out.println("Employee: " + demande.getUser().getFullName());

        String emailStatus = "❌ FAILED";
        String emailError = "";
        try {
            emailService.sendChefServiceDecisionEmail(
                    demande.getUser().getEmail(),
                    demande.getUser().getFullName(),
                    demande.getTypeConge().getLibelle(),
                    demande.getDateDebut().toString(),
                    demande.getDateFin().toString(),
                    request.getStatutChefService().toString(),
                    request.getCommentaire()
            );
            System.out.println("✅ EMAIL SENT");
            emailStatus = "✅ SENT";
        } catch (Exception e) {
            System.err.println("❌ EMAIL FAILED: " + e.getMessage());
            emailError = e.getMessage();
        }

        try {
            notificationService.createNotification(
                    demande.getUser(),
                    "Décision du chef de service",
                    "Votre demande a été " + request.getStatutChefService().toString(),
                    demande.getId()
            );
        } catch (Exception e) {
            System.err.println("❌ NOTIFICATION FAILED: " + e.getMessage());
        }

        System.out.println("=".repeat(80) + "\n");

        DemandeResponse resp = mapToDemandeResponse(updatedDemande);
        resp.setEmailStatus(emailStatus);
        resp.setEmailError(emailError);
        return resp;
    }

    @Transactional
    public DemandeResponse updateDemandByDg(Long demandeId, UpdateDemandeDgRequest request) {
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        StatutDg previousDgStatus = demande.getStatutDg();

        demande.setStatutDg(request.getStatutDg());
        demande.setCommentaireDg(request.getCommentaire());
        demande.setDateTraitementDg(LocalDateTime.now());

        DemandeConge updatedDemande = demandeCongeRepository.save(demande);
        demandeCongeRepository.flush();

        // Update leave balance ONLY when DG validates (final approval)
        if (request.getStatutDg() == StatutDg.VALIDER && previousDgStatus != StatutDg.VALIDER) {
            long days = demande.getDateFin().toEpochDay() - demande.getDateDebut().toEpochDay() + 1;
            System.out.println("🔍 DG VALIDATION - Processing leave request (Days: " + days + ")");

            try {
                if (days <= 2) {
                    System.out.println("   ℹ️  Days <= 2: Treating as ABSENCE");
                    absenceService.recordAbsence(
                            demande.getUser().getId(),
                            demande.getTypeConge().getId(),
                            demande.getDateDebut(),
                            demande.getDateFin(),
                            demande.getMotif()
                    );
                } else {
                    System.out.println("   ℹ️  Days > 2: Deducting from SOLDE");
                    soldeCongeService.deductLeaveBalance(
                            demande.getUser().getId(),
                            demande.getTypeConge().getId(),
                            demande.getDateDebut(),
                            demande.getDateFin()
                    );
                }

                // Check if leave starts today or in the past
                LocalDate today = LocalDate.now();
                if (demande.getDateDebut().isBefore(today) || demande.getDateDebut().isEqual(today)) {
                    // Leave has already started, deactivate user immediately
                    demande.getUser().setActive(false);
                    userRepository.save(demande.getUser());
                    System.out.println("   ✅ User set to inactive immediately (leave starts today or in the past)");
                } else {
                    // Leave is in the future - will be deactivated by scheduler when dateDebut is reached
                    System.out.println("   ℹ️  Leave starts in the future (" + demande.getDateDebut() + "). Scheduler will deactivate user at that time.");
                }

            } catch (Exception e) {
                System.err.println("   ❌ ERROR: " + e.getMessage());
                throw new RuntimeException("Erreur lors du traitement: " + e.getMessage());
            }
        }

        // Send notification to employee
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📨 DG DECISION - SENDING NOTIFICATIONS (Request ID: " + demandeId + ")");
        System.out.println("=".repeat(80));
        System.out.println("Decision: " + request.getStatutDg());

        String emailStatus = "❌ FAILED";
        String emailError = "";
        try {
            emailService.sendDgDecisionEmail(
                    demande.getUser().getEmail(),
                    demande.getUser().getFullName(),
                    demande.getTypeConge().getLibelle(),
                    demande.getDateDebut().toString(),
                    demande.getDateFin().toString(),
                    request.getStatutDg().toString(),
                    request.getCommentaire()
            );
            System.out.println("✅ EMAIL SENT");
            emailStatus = "✅ SENT";
        } catch (Exception e) {
            System.err.println("❌ EMAIL FAILED: " + e.getMessage());
            emailError = e.getMessage();
        }

        try {
            notificationService.createNotification(
                    demande.getUser(),
                    "Décision du directeur général",
                    "Votre demande a été " + request.getStatutDg().toString(),
                    demande.getId()
            );
        } catch (Exception e) {
            System.err.println("❌ NOTIFICATION FAILED: " + e.getMessage());
        }

        System.out.println("=".repeat(80) + "\n");

        DemandeResponse resp = mapToDemandeResponse(updatedDemande);
        resp.setEmailStatus(emailStatus);
        resp.setEmailError(emailError);
        return resp;
    }

    /**
     * Get only requests that can be modified (in pending state)
     */
    public List<DemandeResponse> getModifiableDemandes(Long userId) {
        return demandeCongeRepository.findByUserId(userId).stream()
                .filter(d -> d.getStatutManager() == StatutManager.EN_ATTENTE &&
                        d.getStatutRh() == StatutRh.EN_ATTENTE)
                .map(this::mapToDemandeResponse)
                .collect(Collectors.toList());
    }

    /**
     * Calculate number of days for a leave request
     */
    public long calculateNombreJours(LocalDate dateDebut, LocalDate dateFin) {
        return dateDebut.until(dateFin.plusDays(1), java.time.temporal.ChronoUnit.DAYS);
    }

    /**
     * Get all demandes for a specific manager (from their SALARIE subordinates)
     */
    public List<DemandeResponse> getDemandesForManagerTeam(Long managerId) {
        return demandeCongeRepository.findAllForManager(managerId).stream()
                .map(this::mapToDemandeResponse)
                .collect(Collectors.toList());
    }

    /**
     * SALARIE can update their own pending demande
     */
    @Transactional
    public DemandeResponse updateDemandeBySalarie(Long demandeId, Long userId, DemandeRequest request, MultipartFile justificatif) {
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        // Verify ownership
        if (!demande.getUser().getId().equals(userId)) {
            throw new RuntimeException("Non autorisé: vous ne pouvez modifier que vos propres demandes");
        }

        // Verify request is still pending or requesting more info
        if (demande.getStatutManager() != StatutManager.EN_ATTENTE &&
            demande.getStatutManager() != StatutManager.PLUS_D_INFOS) {
            throw new RuntimeException("Seules les demandes en attente peuvent être modifiées");
        }
        if (demande.getStatutRh() != StatutRh.EN_ATTENTE) {
            throw new RuntimeException("La demande ne peut plus être modifiée");
        }

        demande.setDateDebut(request.getDateDebut());
        demande.setDateFin(request.getDateFin());
        demande.setMotif(request.getMotif());
        demande.setTypeConge(typeCongeRepository.findById(request.getTypeCongeId())
                .orElseThrow(() -> new RuntimeException("Type de congé non trouvé")));

        // Handle file upload
        if (justificatif != null && !justificatif.isEmpty()) {
            String fileName = fileStorageService.storeFile(justificatif);
            demande.setJustificatif(fileName);
        }

        DemandeConge updated = demandeCongeRepository.save(demande);

        // Reset manager status if was PLUS_D_INFOS
        if (demande.getStatutManager() == StatutManager.PLUS_D_INFOS) {
            demande.setStatutManager(StatutManager.EN_ATTENTE);
            demandeCongeRepository.save(demande);
        }

        return mapToDemandeResponse(updated);
    }

    /**
     * SALARIE can delete their own pending demande
     */
    @Transactional
    public void deleteDemandeBySalarie(Long demandeId, Long userId) {
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        // Verify ownership
        if (!demande.getUser().getId().equals(userId)) {
            throw new RuntimeException("Non autorisé: vous ne pouvez supprimer que vos propres demandes");
        }

        // Verify request is still pending (can only delete EN_ATTENTE demandes)
        if (demande.getStatutManager() != StatutManager.EN_ATTENTE ||
            demande.getStatutRh() != StatutRh.EN_ATTENTE) {
            throw new RuntimeException("Seules les demandes en attente peuvent être supprimées");
        }

        demandeCongeRepository.delete(demande);
    }

    /**
     * Verify if manager is authorized to approve/reject a demande
     */
    public void validateManagerAuthorization(Long demandeId, Long managerId) {
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        if (demande.getUser().getManager() == null ||
            !demande.getUser().getManager().getId().equals(managerId)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à traiter cette demande");
        }
    }
}
