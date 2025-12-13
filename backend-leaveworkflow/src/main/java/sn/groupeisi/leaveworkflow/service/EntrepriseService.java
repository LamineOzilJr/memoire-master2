package sn.groupeisi.leaveworkflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.groupeisi.leaveworkflow.dto.EntrepriseDto;
import sn.groupeisi.leaveworkflow.model.Entreprise;
import sn.groupeisi.leaveworkflow.repository.EntrepriseRepository;
import sn.groupeisi.leaveworkflow.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<EntrepriseDto> getAllEntreprises() {
        return entrepriseRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EntrepriseDto getEntrepriseById(Long id) {
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise non trouvée"));
        return mapToDto(entreprise);
    }

    @Transactional
    public EntrepriseDto createEntreprise(Entreprise entreprise) {
        if (entrepriseRepository.findByLibelle(entreprise.getLibelle()).isPresent()) {
            throw new RuntimeException("Entreprise existe déjà");
        }
        Entreprise saved = entrepriseRepository.save(entreprise);
        return mapToDto(saved);
    }

    @Transactional
    public EntrepriseDto updateEntreprise(Long id, EntrepriseDto dto) {
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise non trouvée"));

        if (!entreprise.getLibelle().equals(dto.getLibelle()) &&
            entrepriseRepository.findByLibelle(dto.getLibelle()).isPresent()) {
            throw new RuntimeException("Entreprise existe déjà");
        }

        entreprise.setLibelle(dto.getLibelle());
        entreprise.setDescription(dto.getDescription());
        if (dto.getActive() != null) {
            entreprise.setActive(dto.getActive());
        }

        return mapToDto(entrepriseRepository.save(entreprise));
    }

    @Transactional
    public void deleteEntreprise(Long id) {
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise non trouvée"));

        if (userRepository.countByEntrepriseId(id) > 0) {
            throw new RuntimeException("Impossible de supprimer une entreprise contenant des utilisateurs");
        }

        entrepriseRepository.delete(entreprise);
    }

    @Transactional
    public EntrepriseDto toggleActive(Long id) {
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise non trouvée"));
        entreprise.setActive(!entreprise.getActive());
        return mapToDto(entrepriseRepository.save(entreprise));
    }

    private EntrepriseDto mapToDto(Entreprise entreprise) {
        return EntrepriseDto.builder()
                .id(entreprise.getId())
                .libelle(entreprise.getLibelle())
                .description(entreprise.getDescription())
                .active(entreprise.getActive())
                .createdAt(entreprise.getCreatedAt())
                .updatedAt(entreprise.getUpdatedAt())
                .userCount(entreprise.getId() != null ? Math.toIntExact(userRepository.countByEntrepriseId(entreprise.getId())) : 0)
                .build();
    }
}

