package sn.groupeisi.leaveworkflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sn.groupeisi.leaveworkflow.enums.Role;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;

    @NotBlank(message = "Le prénom est requis")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    private String prenom;

    @NotBlank(message = "Le nom est requis")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String nom;

    @NotBlank(message = "L'email est requis")
    @Email(message = "L'email doit être valide")
    private String email;

    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;

    @Pattern(regexp = "^[+]?[0-9]{9,15}$", message = "Le numéro de téléphone doit être valide")
    private String telephone;

    private String adresse;

    @NotNull(message = "Le rôle est requis")
    private Role role;

    @NotBlank(message = "Le poste est requis")
    private String poste;

    @NotBlank(message = "Le matricule est requis")
    @Pattern(regexp = "^[A-Z0-9]{3,10}$", message = "Le matricule doit contenir entre 3 et 10 caractères alphanumériques majuscules")
    private String matricule;

    private Long departementId;
    private String departementLibelle;
    private Long entrepriseId;
    private String entrepriseLibelle;
    private Long managerId;
    private String managerFullName;
    private Boolean active;
    private LocalDateTime createdAt;
}