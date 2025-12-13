package sn.groupeisi.leaveworkflow.model;

import jakarta.persistence.*;
import lombok.*;
import sn.groupeisi.leaveworkflow.enums.StatutManager;
import sn.groupeisi.leaveworkflow.enums.StatutRh;
import sn.groupeisi.leaveworkflow.enums.StatutChefService;
import sn.groupeisi.leaveworkflow.enums.StatutDg;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_conges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_conge_id", nullable = false)
    private TypeConge typeConge;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    private String motif;

    private String justificatif;  // File path

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutManager statutManager = StatutManager.EN_ATTENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutRh statutRh = StatutRh.EN_ATTENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutChefService statutChefService = StatutChefService.EN_ATTENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDg statutDg = StatutDg.EN_ATTENTE;

    private String commentaireManager;

    private String commentaireRh;

    private String commentaireChefService;

    private String commentaireDg;

    private LocalDateTime dateTraitementManager;

    private LocalDateTime dateTraitementRh;

    private LocalDateTime dateTraitementChefService;

    private LocalDateTime dateTraitementDg;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper for days calculation
    public long getNombreJours() {
        return dateDebut.until(dateFin.plusDays(1), java.time.temporal.ChronoUnit.DAYS);
    }
}