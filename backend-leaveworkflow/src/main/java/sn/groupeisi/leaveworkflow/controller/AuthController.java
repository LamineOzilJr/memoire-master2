package sn.groupeisi.leaveworkflow.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.groupeisi.leaveworkflow.dto.LoginRequest;
import sn.groupeisi.leaveworkflow.dto.LoginResponse;
import sn.groupeisi.leaveworkflow.dto.RegisterRequest;
import sn.groupeisi.leaveworkflow.service.AuthService;  // Assume this exists or create it

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }
}