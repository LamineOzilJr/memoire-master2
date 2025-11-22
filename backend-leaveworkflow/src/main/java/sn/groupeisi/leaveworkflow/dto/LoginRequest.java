
package sn.groupeisi.leaveworkflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email requis")
    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "Mot de passe requis")
    private String password;
}