package sn.groupeisi.leaveworkflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntrepriseDto {

    private Long id;

    @NotBlank(message = "Le libellé est requis")
    private String libelle;

    private String description;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer userCount; // Number of users in this entreprise
}

