package sn.groupeisi.leaveworkflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sn.groupeisi.leaveworkflow.enums.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Prénom requis")
    private String prenom;

    @NotBlank(message = "Nom requis")
    private String nom;

    @Email(message = "Email invalide")
    @NotBlank(message = "Email requis")
    private String email;

    @NotBlank(message = "Mot de passe requis")
    private String password;

    private String telephone;
    private String adresse;
    private Role role = Role.SALARIE;
    private String poste;
    private String matricule;
    private Long departementId;
    private Long managerId;
}