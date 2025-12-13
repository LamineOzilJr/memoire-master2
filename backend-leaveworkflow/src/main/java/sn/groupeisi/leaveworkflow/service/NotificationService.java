package sn.groupeisi.leaveworkflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.groupeisi.leaveworkflow.dto.NotificationResponse;
import sn.groupeisi.leaveworkflow.model.Notification;
import sn.groupeisi.leaveworkflow.model.User;
import sn.groupeisi.leaveworkflow.repository.NotificationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void createNotification(User user, String titre, String message) {
        createNotification(user, titre, message, null);
    }

    @Transactional
    public void createNotification(User user, String titre, String message, Long targetId) {
        System.out.println("🔔 Creating notification for userId=" + (user != null ? user.getId() : null) + ", titre=" + titre + ", targetId=" + targetId);
        Notification notification = Notification.builder()
                .user(user)
                .titre(titre)
                .message(message)
                .lu(false)
                .targetId(targetId)
                .build();
        notificationRepository.save(notification);
    }

    public List<NotificationResponse> getNotificationsByUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndLuFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        // Delete the notification from DB when user has seen it
        System.out.println("🔔 Deleting notification id=" + notificationId + " as it was seen by user");
        notificationRepository.deleteById(notificationId);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .titre(n.getTitre())
                .message(n.getMessage())
                .lu(n.getLu())
                .createdAt(n.getCreatedAt())
                .targetId(n.getTargetId())
                .build();
    }
}