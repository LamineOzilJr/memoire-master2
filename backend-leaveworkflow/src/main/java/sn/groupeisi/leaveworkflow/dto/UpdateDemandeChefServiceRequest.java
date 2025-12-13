package sn.groupeisi.leaveworkflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sn.groupeisi.leaveworkflow.enums.StatutChefService;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDemandeChefServiceRequest {
    @NotNull
    private StatutChefService statutChefService;
    private String commentaire;
    private Long chefServiceId;
}

