package com.tinysteps.scheduleservice.entity;

import com.tinysteps.scheduleservice.constants.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointment_status_history", indexes = {
        @Index(name = "idx_ash_appointment_id", columnList = "appointment_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppointmentStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_history_appointment"))
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private AppointmentStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private AppointmentStatus newStatus;

    @Column(name = "changed_by_id")
    private UUID changedById;

    @Column(name = "changed_by_type", length = 20)
    private String changedByType;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private ZonedDateTime changedAt;
}
