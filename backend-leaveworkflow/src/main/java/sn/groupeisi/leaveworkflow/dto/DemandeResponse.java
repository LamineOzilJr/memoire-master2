package sn.groupeisi.leaveworkflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sn.groupeisi.leaveworkflow.enums.StatutManager;
import sn.groupeisi.leaveworkflow.enums.StatutRh;
import sn.groupeisi.leaveworkflow.enums.StatutChefService;
import sn.groupeisi.leaveworkflow.enums.StatutDg;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long typeCongeId;
    private String typeCongeLibelle;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Long nombreJours;
    private String motif;
    private String justificatif;
    private StatutManager statutManager;
    private StatutRh statutRh;
    private StatutChefService statutChefService;
    private StatutDg statutDg;
    private String commentaireManager;
    private String commentaireRh;
    private String commentaireChefService;
    private String commentaireDg;
    private LocalDateTime dateCreation;
    private LocalDateTime dateTraitementManager;
    private LocalDateTime dateTraitementRh;
    private LocalDateTime dateTraitementChefService;
    private LocalDateTime dateTraitementDg;
    private String emailStatus;
    private String emailError;
    private Boolean hasOverlap;  // New field to indicate if demand overlaps with others in same department
}