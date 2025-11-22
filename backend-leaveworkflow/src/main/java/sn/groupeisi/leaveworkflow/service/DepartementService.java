package sn.groupeisi.leaveworkflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.groupeisi.leaveworkflow.dto.DepartementDto;
import sn.groupeisi.leaveworkflow.model.Departement;
import sn.groupeisi.leaveworkflow.repository.DepartementRepository;
import sn.groupeisi.leaveworkflow.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartementService {

    private final DepartementRepository departementRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DepartementDto> getAllDepartements() {
        return departementRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartementDto getDepartementById(Long id) {
        Departement departement = departementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Departement non trouve"));
        return mapToDto(departement);
    }

    @Transactional
    public DepartementDto createDepartement(Departement departement) {
        if (departementRepository.findByLibelle(departement.getLibelle()).isPresent()) {
            throw new RuntimeException("Departement existe deja");
        }
        Departement saved = departementRepository.save(departement);
        return mapToDto(saved);
    }

    @Transactional
    public DepartementDto updateDepartement(Long id, DepartementDto dto) {
        Departement departement = departementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Departement non trouve"));
        
        if (!departement.getLibelle().equals(dto.getLibelle()) &&
            departementRepository.findByLibelle(dto.getLibelle()).isPresent()) {
            throw new RuntimeException("Departement existe deja");
        }

        departement.setLibelle(dto.getLibelle());
        departement.setDescription(dto.getDescription());
        if (dto.getActive() != null) {
            departement.setActive(dto.getActive());
        }

        return mapToDto(departementRepository.save(departement));
    }

    @Transactional
    public void deleteDepartement(Long id) {
        Departement departement = departementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Departement non trouve"));
        
        if (userRepository.countByDepartementId(id) > 0) {
            throw new RuntimeException("Impossible de supprimer un departement contenant des utilisateurs");
        }
        
        departementRepository.delete(departement);
    }

    @Transactional
    public DepartementDto toggleActive(Long id) {
        Departement departement = departementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Departement non trouve"));
        departement.setActive(!departement.getActive());
        return mapToDto(departementRepository.save(departement));
    }

    private DepartementDto mapToDto(Departement departement) {
        return DepartementDto.builder()
                .id(departement.getId())
                .libelle(departement.getLibelle())
                .description(departement.getDescription())
                .active(departement.getActive())
                .createdAt(departement.getCreatedAt())
                .updatedAt(departement.getUpdatedAt())
                .userCount(departement.getId() != null ? Math.toIntExact(userRepository.countByDepartementId(departement.getId())) : 0)
                .build();
    }
}
