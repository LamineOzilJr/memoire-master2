package sn.groupeisi.leaveworkflow.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.groupeisi.leaveworkflow.dto.SoldeResponse;
import sn.groupeisi.leaveworkflow.model.User;
import sn.groupeisi.leaveworkflow.service.SoldeCongeService;

import java.util.List;

@RestController
@RequestMapping("/soldes")
@RequiredArgsConstructor
public class SoldeCongeController {

    private final SoldeCongeService soldeCongeService;

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('SALARIE','MANAGER','SERVICE_RH','ADMIN')")
    public ResponseEntity<List<SoldeResponse>> getMySoldes(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(soldeCongeService.getSoldesByUser(user.getId()));
    }

    @GetMapping("/my/total")
    @PreAuthorize("hasAnyRole('SALARIE','MANAGER','SERVICE_RH','ADMIN')")
    public ResponseEntity<Double> getMyTotalSolde(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Double total = soldeCongeService.getTotalSoldeConsideringSeniority(user.getId());
        System.out.println("📤 SOLDE API RESPONSE: User " + user.getId() + " | Total: " + total);
        return ResponseEntity.ok(total);
    }
}
