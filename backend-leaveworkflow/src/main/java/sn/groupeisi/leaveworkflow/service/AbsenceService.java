package sn.groupeisi.leaveworkflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.groupeisi.leaveworkflow.dto.AbsenceResponse;
import sn.groupeisi.leaveworkflow.model.Absence;
import sn.groupeisi.leaveworkflow.model.User;
import sn.groupeisi.leaveworkflow.repository.AbsenceRepository;
import sn.groupeisi.leaveworkflow.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AbsenceService {
    private final AbsenceRepository absenceRepository;
    private final UserRepository userRepository;

    @Transactional
    public Absence createAbsence(User user, LocalDate dateDebut, LocalDate dateFin, String motif, long nombreJours) {
        Absence a = Absence.builder()
                .user(user)
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .nombreJours(nombreJours)
                .motif(motif)
                .build();
        return absenceRepository.save(a);
    }

    @Transactional
    public Absence recordAbsence(Long userId, Long typeCongeId, LocalDate dateDebut, LocalDate dateFin, String motif) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long nombreJours = dateFin.toEpochDay() - dateDebut.toEpochDay() + 1;

        System.out.println("📝 RECORDING ABSENCE:");
        System.out.println("   User: " + user.getFullName());
        System.out.println("   Date: " + dateDebut + " to " + dateFin);
        System.out.println("   Days: " + nombreJours);
        System.out.println("   Motif: " + motif);

        Absence a = Absence.builder()
                .user(user)
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .nombreJours(nombreJours)
                .motif(motif)
                .build();

        Absence saved = absenceRepository.save(a);
        System.out.println("   ✅ Absence saved with ID: " + saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AbsenceResponse> getAbsencesForUser(User user) {
        return absenceRepository.findByUser(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AbsenceResponse mapToResponse(Absence a) {
        return AbsenceResponse.builder()
                .id(a.getId())
                .userId(a.getUser().getId())
                .userName(a.getUser().getFullName())
                .dateDebut(a.getDateDebut())
                .dateFin(a.getDateFin())
                .nombreJours(a.getNombreJours())
                .motif(a.getMotif())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
