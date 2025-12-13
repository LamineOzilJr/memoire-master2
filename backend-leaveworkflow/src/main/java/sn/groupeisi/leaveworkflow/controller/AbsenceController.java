package sn.groupeisi.leaveworkflow.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.groupeisi.leaveworkflow.dto.AbsenceResponse;
import sn.groupeisi.leaveworkflow.model.User;
import sn.groupeisi.leaveworkflow.service.AbsenceService;

import java.util.List;

@RestController
@RequestMapping("/absences")
@RequiredArgsConstructor
public class AbsenceController {

    private final AbsenceService absenceService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('SALARIE') or hasRole('MANAGER') or hasRole('SERVICE_RH')")
    public ResponseEntity<List<AbsenceResponse>> getMyAbsences(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(absenceService.getAbsencesForUser(user));
    }
}

