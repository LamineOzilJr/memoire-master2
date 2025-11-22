package sn.groupeisi.leaveworkflow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import sn.groupeisi.leaveworkflow.model.TypeConge;
import sn.groupeisi.leaveworkflow.repository.TypeCongeRepository;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final TypeCongeRepository typeCongeRepository;

    @Override
    public void run(String... args) throws Exception {
        loadLeaveTypes();
    }

    private void loadLeaveTypes() {
        if (typeCongeRepository.count() == 0) {
            log.info("Loading default leave types...");
            
            List<TypeConge> leaveTypes = Arrays.asList(
                TypeConge.builder()
                    .libelle("Mariage")
                    .description("Congé pour mariage")
                    .maxJours(3)
                    .requiresJustificatif(true)
                    .active(true)
                    .build(),
                    
                TypeConge.builder()
                    .libelle("Maladie")
                    .description("Congé maladie")
                    .maxJours(null)
                    .requiresJustificatif(true)
                    .active(true)
                    .build(),
                    
                TypeConge.builder()
                    .libelle("Accident de travail")
                    .description("Congé pour accident de travail")
                    .maxJours(null)
                    .requiresJustificatif(true)
                    .active(true)
                    .build(),
                    
                TypeConge.builder()
                    .libelle("Paternité")
                    .description("Congé de paternité")
                    .maxJours(14)
                    .requiresJustificatif(true)
                    .active(true)
                    .build(),
                    
                TypeConge.builder()
                    .libelle("Absence")
                    .description("Absence justifiée")
                    .maxJours(null)
                    .requiresJustificatif(false)
                    .active(true)
                    .build(),
                    
                TypeConge.builder()
                    .libelle("Maternité")
                    .description("Congé de maternité")
                    .maxJours(98)
                    .requiresJustificatif(true)
                    .active(true)
                    .build(),
                    
                TypeConge.builder()
                    .libelle("Naissance/décès")
                    .description("Congé pour naissance ou décès")
                    .maxJours(3)
                    .requiresJustificatif(true)
                    .active(true)
                    .build(),
                    
                TypeConge.builder()
                    .libelle("Repos")
                    .description("Congé de repos")
                    .maxJours(null)
                    .requiresJustificatif(false)
                    .active(true)
                    .build(),
                    
                TypeConge.builder()
                    .libelle("Annuel")
                    .description("Congé annuel")
                    .maxJours(24)
                    .requiresJustificatif(false)
                    .active(true)
                    .build()
            );
            
            typeCongeRepository.saveAll(leaveTypes);
            log.info("Loaded {} leave types successfully", leaveTypes.size());
        } else {
            log.info("Leave types already exist in database, skipping initialization");
        }
    }
}
