package sn.groupeisi.leaveworkflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private String titre;
    private String message;
    private Boolean lu;
    private LocalDateTime createdAt;
    private Long targetId; // optional related resource id (e.g., demande id)
}