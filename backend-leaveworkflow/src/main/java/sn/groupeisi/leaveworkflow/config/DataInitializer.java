package sn.groupeisi.leaveworkflow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import sn.groupeisi.leaveworkflow.enums.Role;
import sn.groupeisi.leaveworkflow.model.User;
import sn.groupeisi.leaveworkflow.repository.UserRepository;

/**
 * DataInitializer is responsible for initializing default data when the application starts.
 * It creates an admin user if one doesn't already exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeAdminUser();
    }

    /**
     * Creates a default admin user if no admin exists in the database.
     */
    private void initializeAdminUser() {
        String adminEmail = "lamine@admin.com";
        
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = User.builder()
                    .prenom("Lamine")
                    .nom("Administrator")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Passerpasser"))
                    .telephone("221770000000")
                    .adresse("Dakar, Senegal")
                    .role(Role.ADMIN)
                    .poste("Administrateur Système")
                    .matricule("ADM001")
                    .active(true)
                    .build();

            userRepository.save(admin);
            log.info("✅ Admin user created successfully!");
            log.info("📧 Email: {}", adminEmail);
            log.info("🔑 Password: Passerpasser");
            log.info("👤 Username: lamine");
        } else {
            log.info("ℹ️ Admin user already exists. Skipping initialization.");
        }
    }
}
