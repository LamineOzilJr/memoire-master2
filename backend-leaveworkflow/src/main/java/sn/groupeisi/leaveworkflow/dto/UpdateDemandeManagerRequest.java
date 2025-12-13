package sn.groupeisi.leaveworkflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sn.groupeisi.leaveworkflow.enums.StatutManager;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDemandeManagerRequest {
    @NotNull
    private Long managerId;
    @NotNull
    private StatutManager statutManager;
    private String commentaire;
}