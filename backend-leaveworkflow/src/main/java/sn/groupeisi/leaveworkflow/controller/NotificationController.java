package sn.groupeisi.leaveworkflow.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sn.groupeisi.leaveworkflow.dto.NotificationResponse;
import sn.groupeisi.leaveworkflow.model.User;
import sn.groupeisi.leaveworkflow.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(Authentication authentication, HttpServletRequest request) {
        // Temporarily allow access without method-level security to assist debugging.
        if (authentication == null || authentication.getPrincipal() == null) {
            System.out.println("🔔 [NotificationController] GET " + request.getRequestURI() + " - No authentication found");
            return ResponseEntity.ok(List.of());
        }
        User user = (User) authentication.getPrincipal();
        System.out.println("🔔 [NotificationController] GET " + request.getRequestURI() + " called by userId=" + (user != null ? user.getId() : null) + " (" + (user != null ? user.getEmail() : "anonymous") + ")");
        return ResponseEntity.ok(notificationService.getNotificationsByUser(user.getId()));
    }

    @GetMapping("/unread")
    public ResponseEntity<Long> getUnreadCount(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || authentication.getPrincipal() == null) {
            System.out.println("🔔 [NotificationController] GET " + request.getRequestURI() + " - No authentication found");
            return ResponseEntity.ok(0L);
        }
        User user = (User) authentication.getPrincipal();
        System.out.println("🔔 [NotificationController] GET " + request.getRequestURI() + " called by userId=" + (user != null ? user.getId() : null) + " (" + (user != null ? user.getEmail() : "anonymous") + ")");
        long count = notificationService.getUnreadCount(user.getId());
        System.out.println("🔔 [NotificationController] Unread count for userId=" + user.getId() + " => " + count);
        return ResponseEntity.ok(count);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        if (authentication == null || authentication.getPrincipal() == null) {
            System.out.println("🔔 [NotificationController] PUT " + request.getRequestURI() + " - No authentication found");
            return ResponseEntity.ok().build();
        }
        User user = (User) authentication.getPrincipal();
        System.out.println("🔔 [NotificationController] PUT " + request.getRequestURI() + " called by userId=" + (user != null ? user.getId() : null) + " mark notificationId=" + id);
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsForUserId(@PathVariable Long id, HttpServletRequest request, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            System.out.println("🔔 [NotificationController] GET " + request.getRequestURI() + " - No authentication found");
            return ResponseEntity.ok(List.of());
        }
        User caller = (User) authentication.getPrincipal();
        System.out.println("🔔 [NotificationController] GET " + request.getRequestURI() + " called by adminId=" + (caller != null ? caller.getId() : null) + " targetUserId=" + id);
        return ResponseEntity.ok(notificationService.getNotificationsByUser(id));
    }
}
