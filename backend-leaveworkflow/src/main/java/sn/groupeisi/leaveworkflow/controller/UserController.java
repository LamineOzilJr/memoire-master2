package sn.groupeisi.leaveworkflow.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sn.groupeisi.leaveworkflow.dto.UserDto;
import sn.groupeisi.leaveworkflow.dto.ManagerStatsDto;
import sn.groupeisi.leaveworkflow.model.User;
import sn.groupeisi.leaveworkflow.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SERVICE_RH', 'ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        log.info("GET /users - Fetching all users");
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SERVICE_RH', 'ADMIN', 'MANAGER') or @userService.isCurrentUser(#id, authentication)")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id, Authentication authentication) {
        log.info("GET /users/{} - Fetching user by id", id);
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SERVICE_RH', 'ADMIN')")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
        log.info("POST /users - Creating new user with email: {}", userDto.getEmail());
        UserDto createdUser = userService.createUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SERVICE_RH', 'ADMIN')")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto userDto) {
        log.info("PUT /users/{} - Updating user", id);
        UserDto updatedUser = userService.updateUser(id, userDto);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SERVICE_RH', 'ADMIN')")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        log.info("DELETE /users/{} - Deleting user", id);
        userService.deleteUser(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Utilisateur supprimé avec succès");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        log.info("GET /users/me - Fetching current user");
        User user = (User) authentication.getPrincipal();
        // Fetch fresh DTO via service to ensure associations are loaded within a transaction
        UserDto userDto = userService.getUserById(user.getId());
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/manager/stats")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ManagerStatsDto> getManagerStats(Authentication authentication) {
        log.info("GET /users/manager/stats - Fetching manager stats");
        User user = (User) authentication.getPrincipal();
        ManagerStatsDto stats = userService.getManagerStats(user.getId());
        return ResponseEntity.ok(stats);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SERVICE_RH', 'ADMIN')")
    public ResponseEntity<UserDto> activateUser(@PathVariable Long id) {
        log.info("PATCH /users/{}/activate - Activating user", id);
        UserDto user = userService.getUserById(id);
        user.setActive(true);
        UserDto updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SERVICE_RH', 'ADMIN')")
    public ResponseEntity<UserDto> deactivateUser(@PathVariable Long id) {
        log.info("PATCH /users/{}/deactivate - Deactivating user", id);
        UserDto user = userService.getUserById(id);
        user.setActive(false);
        UserDto updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(updatedUser);
    }
}