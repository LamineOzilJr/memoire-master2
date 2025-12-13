package sn.groupeisi.leaveworkflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sn.groupeisi.leaveworkflow.enums.StatutDg;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDemandeDgRequest {
    @NotNull
    private StatutDg statutDg;
    private String commentaire;
    private Long dgId;
}

