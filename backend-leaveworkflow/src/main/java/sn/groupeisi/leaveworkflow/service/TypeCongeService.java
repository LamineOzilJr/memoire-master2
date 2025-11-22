package sn.groupeisi.leaveworkflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.groupeisi.leaveworkflow.dto.TypeCongeDto;
import sn.groupeisi.leaveworkflow.model.TypeConge;
import sn.groupeisi.leaveworkflow.repository.TypeCongeRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TypeCongeService {

    private final TypeCongeRepository typeCongeRepository;

    @Transactional(readOnly = true)
    public List<TypeConge> getAllTypeConges() {
        return typeCongeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public TypeCongeDto getTypeCongeById(Long id) {
        TypeConge typeConge = typeCongeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Type de conge non trouve"));
        return mapToDto(typeConge);
    }

    @Transactional
    public TypeConge createTypeConge(TypeConge typeConge) {
        if (typeCongeRepository.findByLibelle(typeConge.getLibelle()).isPresent()) {
            throw new RuntimeException("Type de conge existe deja");
        }
        return typeCongeRepository.save(typeConge);
    }

    @Transactional
    public TypeCongeDto updateTypeConge(Long id, TypeCongeDto dto) {
        TypeConge typeConge = typeCongeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Type de conge non trouve"));
        
        if (!typeConge.getLibelle().equals(dto.getLibelle()) &&
            typeCongeRepository.findByLibelle(dto.getLibelle()).isPresent()) {
            throw new RuntimeException("Type de conge existe deja");
        }

        typeConge.setLibelle(dto.getLibelle());
        typeConge.setDescription(dto.getDescription());
        typeConge.setMaxJours(dto.getMaxJours());
        if (dto.getRequiresJustificatif() != null) {
            typeConge.setRequiresJustificatif(dto.getRequiresJustificatif());
        }
        if (dto.getActive() != null) {
            typeConge.setActive(dto.getActive());
        }

        return mapToDto(typeCongeRepository.save(typeConge));
    }

    @Transactional
    public void deleteTypeConge(Long id) {
        TypeConge typeConge = typeCongeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Type de conge non trouve"));
        typeCongeRepository.delete(typeConge);
    }

    @Transactional
    public TypeCongeDto toggleActive(Long id) {
        TypeConge typeConge = typeCongeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Type de conge non trouve"));
        typeConge.setActive(!typeConge.getActive());
        return mapToDto(typeCongeRepository.save(typeConge));
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
