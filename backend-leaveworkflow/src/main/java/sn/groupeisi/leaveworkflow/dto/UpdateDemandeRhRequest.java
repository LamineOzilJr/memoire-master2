package sn.groupeisi.leaveworkflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sn.groupeisi.leaveworkflow.enums.StatutRh;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDemandeRhRequest {
    @NotNull
    private StatutRh statutRh;
    private String commentaire;
}