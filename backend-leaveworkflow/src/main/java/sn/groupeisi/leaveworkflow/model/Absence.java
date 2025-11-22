package sn.groupeisi.leaveworkflow.model;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "absences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Absence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Long nombreJours;
    private String motif;

    private LocalDateTime createdAt = LocalDateTime.now();
}
