package sn.groupeisi.leaveworkflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sn.groupeisi.leaveworkflow.enums.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String prenom;
    private String nom;
    private Role role;
    private String matricule;
    private Long managerId;
    private String managerName;
    private Long departementId;
    private String departementName;
}