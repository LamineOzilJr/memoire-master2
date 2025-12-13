package sn.groupeisi.leaveworkflow.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.groupeisi.leaveworkflow.dto.TypeCongeDto;
import sn.groupeisi.leaveworkflow.model.TypeConge;
import sn.groupeisi.leaveworkflow.service.TypeCongeService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/type-conges")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class TypeCongeController {

    private final TypeCongeService typeCongeService;

    @GetMapping
    public ResponseEntity<List<TypeCongeDto>> getAllTypeConges() {
        log.info("GET /api/type-conges - Fetching all leave types");
        List<TypeConge> typeConges = typeCongeService.getAllTypeConges();
        List<TypeCongeDto> dtos = typeConges.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SERVICE_RH', 'ADMIN')")
    public ResponseEntity<TypeCongeDto> createTypeConge(@Valid @RequestBody TypeCongeDto dto) {
        log.info("POST /api/type-conges - Creating new leave type: {}", dto.getLibelle());
        TypeConge typeConge = TypeConge.builder()
                .libelle(dto.getLibelle())
                .description(dto.getDescription())
                .maxJours(dto.getMaxJours())
                .requiresJustificatif(dto.getRequiresJustificatif() != null ? dto.getRequiresJustificatif() : false)
                .active(true)
                .build();
        TypeConge created = typeCongeService.createTypeConge(typeConge);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SERVICE_RH', 'ADMIN')")
    public ResponseEntity<TypeCongeDto> updateTypeConge(@PathVariable Long id, @Valid @RequestBody TypeCongeDto dto) {
        log.info("PUT /api/type-conges/{} - Updating leave type", id);
        TypeCongeDto updated = typeCongeService.updateTypeConge(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SERVICE_RH', 'ADMIN')")
    public ResponseEntity<Map<String, String>> deleteTypeConge(@PathVariable Long id) {
        log.info("DELETE /api/type-conges/{} - Deleting leave type", id);
        typeCongeService.deleteTypeConge(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Type de congé supprimé avec succès");
        return ResponseEntity.ok(response);
    }

    private TypeCongeDto mapToDto(TypeConge typeConge) {
        return TypeCongeDto.builder()
                .id(typeConge.getId())
                .libelle(typeConge.getLibelle())
                .description(typeConge.getDescription())
                .maxJours(typeConge.getMaxJours())
                .requiresJustificatif(typeConge.getRequiresJustificatif())
                .active(typeConge.getActive())
                .createdAt(typeConge.getCreatedAt())
                .updatedAt(typeConge.getUpdatedAt())
                .build();
    }
}
