package sn.groupeisi.leaveworkflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.groupeisi.leaveworkflow.dto.LoginRequest;
import sn.groupeisi.leaveworkflow.dto.LoginResponse;
import sn.groupeisi.leaveworkflow.dto.RegisterRequest;
import sn.groupeisi.leaveworkflow.model.Departement;
import sn.groupeisi.leaveworkflow.model.User;
import sn.groupeisi.leaveworkflow.repository.DepartementRepository;
import sn.groupeisi.leaveworkflow.repository.UserRepository;
import sn.groupeisi.leaveworkflow.security.JwtTokenProvider;  // Assume exists

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DepartementRepository departementRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String token = jwtTokenProvider.generateToken(authentication);
        return LoginResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .prenom(user.getPrenom())
                .nom(user.getNom())
                .role(user.getRole())
                .matricule(user.getMatricule())
                .managerId(user.getManager() != null ? user.getManager().getId() : null)
                .managerName(user.getManager() != null ? user.getManager().getFullName() : null)
                .departementId(user.getDepartement() != null ? user.getDepartement().getId() : null)
                .departementName(user.getDepartement() != null ? user.getDepartement().getLibelle() : null)
                .build();
    }

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        Departement departement = null;
        if (request.getDepartementId() != null) {
            departement = departementRepository.findById(request.getDepartementId()).orElse(null);
        }

        User manager = null;
        if (request.getManagerId() != null) {
            manager = userRepository.findById(request.getManagerId()).orElse(null);
        }

        User user = User.builder()
                .prenom(request.getPrenom())
                .nom(request.getNom())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .telephone(request.getTelephone())
                .adresse(request.getAdresse())
                .role(request.getRole())
                .poste(request.getPoste())
                .matricule(request.getMatricule())
                .departement(departement)
                .manager(manager)
                .build();
        User saved = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(saved.getEmail());
        return LoginResponse.builder()
                .token(token)
                .id(saved.getId())
                .email(saved.getEmail())
                .prenom(saved.getPrenom())
                .nom(saved.getNom())
                .role(saved.getRole())
                .matricule(saved.getMatricule())
                .managerId(saved.getManager() != null ? saved.getManager().getId() : null)
                .managerName(saved.getManager() != null ? saved.getManager().getFullName() : null)
                .departementId(saved.getDepartement() != null ? saved.getDepartement().getId() : null)
                .departementName(saved.getDepartement() != null ? saved.getDepartement().getLibelle() : null)
                .build();
    }
}