package sn.groupeisi.leaveworkflow.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AbsenceResponse {
    private Long id;
    private Long userId;
    private String userName;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Long nombreJours;
    private String motif;
    private LocalDateTime createdAt;
}

