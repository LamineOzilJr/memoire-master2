package sn.groupeisi.leaveworkflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoldeResponse {
    private Long id;
    private Long userId;
    private Long typeCongeId;
    private String typeCongeLibelle;
    private Integer annee;
    private Double joursAcquis;
    private Double joursPris;
    private Double joursRestants;
}