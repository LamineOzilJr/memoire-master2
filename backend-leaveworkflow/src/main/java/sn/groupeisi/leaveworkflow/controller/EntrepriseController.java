package sn.groupeisi.leaveworkflow.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.groupeisi.leaveworkflow.dto.EntrepriseDto;
import sn.groupeisi.leaveworkflow.model.Entreprise;
import sn.groupeisi.leaveworkflow.service.EntrepriseService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/entreprises")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<List<EntrepriseDto>> getAllEntreprises() {
        log.info("GET /api/entreprises - Fetching all entreprises");
        return ResponseEntity.ok(entrepriseService.getAllEntreprises());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntrepriseDto> createEntreprise(@Valid @RequestBody EntrepriseDto dto) {
        log.info("POST /api/entreprises - Creating new entreprise: {}", dto.getLibelle());
        Entreprise entreprise = Entreprise.builder()
                .libelle(dto.getLibelle())
                .description(dto.getDescription())
                .active(true)
                .build();
        EntrepriseDto created = entrepriseService.createEntreprise(entreprise);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntrepriseDto> updateEntreprise(@PathVariable Long id, @Valid @RequestBody EntrepriseDto dto) {
        log.info("PUT /api/entreprises/{} - Updating entreprise", id);
        EntrepriseDto updated = entrepriseService.updateEntreprise(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteEntreprise(@PathVariable Long id) {
        log.info("DELETE /api/entreprises/{} - Deleting entreprise", id);
        entrepriseService.deleteEntreprise(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Entreprise supprimée avec succès");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntrepriseDto> toggleActive(@PathVariable Long id) {
        log.info("PUT /api/entreprises/{}/toggle-active - Toggling active status", id);
        EntrepriseDto updated = entrepriseService.toggleActive(id);
        return ResponseEntity.ok(updated);
    }
}

