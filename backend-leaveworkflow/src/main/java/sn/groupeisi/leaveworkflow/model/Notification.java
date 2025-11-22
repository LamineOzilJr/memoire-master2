package sn.groupeisi.leaveworkflow.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private String message;

    private Boolean lu = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Optional target id (e.g., demande id) to allow UI to open related resource
    @Column(name = "target_id")
    private Long targetId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}