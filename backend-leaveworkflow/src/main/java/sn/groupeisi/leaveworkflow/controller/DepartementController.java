package sn.groupeisi.leaveworkflow.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.groupeisi.leaveworkflow.dto.DepartementDto;
import sn.groupeisi.leaveworkflow.model.Departement;
import sn.groupeisi.leaveworkflow.service.DepartementService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/departements")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class DepartementController {

    private final DepartementService departementService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SERVICE_RH', 'ADMIN')")
    public ResponseEntity<List<DepartementDto>> getAllDepartements() {
        log.info("GET /api/departements - Fetching all departments");
        return ResponseEntity.ok(departementService.getAllDepartements());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SERVICE_RH', 'ADMIN')")
    public ResponseEntity<DepartementDto> createDepartement(@Valid @RequestBody DepartementDto dto) {
        log.info("POST /api/departements - Creating new department: {}", dto.getLibelle());
        Departement departement = Departement.builder()
                .libelle(dto.getLibelle())
                .description(dto.getDescription())
                .active(true)
                .build();
        DepartementDto created = departementService.createDepartement(departement);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SERVICE_RH', 'ADMIN')")
    public ResponseEntity<DepartementDto> updateDepartement(@PathVariable Long id, @Valid @RequestBody DepartementDto dto) {
        log.info("PUT /api/departements/{} - Updating department", id);
        DepartementDto updated = departementService.updateDepartement(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SERVICE_RH', 'ADMIN')")
    public ResponseEntity<Map<String, String>> deleteDepartement(@PathVariable Long id) {
        log.info("DELETE /api/departements/{} - Deleting department", id);
        departementService.deleteDepartement(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Département supprimé avec succès");
        return ResponseEntity.ok(response);
    }

    
}
