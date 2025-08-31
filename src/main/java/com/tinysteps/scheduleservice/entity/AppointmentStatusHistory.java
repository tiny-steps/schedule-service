package com.tinysteps.scheduleservice.entity;

import com.tinysteps.scheduleservice.constants.AppointmentStatus;
import com.tinysteps.scheduleservice.constants.CancellationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointment_status_history", indexes = {
        @Index(name = "idx_ash_appointment_id", columnList = "appointment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_type")
    private CancellationType cancellationType; // Handles both WHO and WHY

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "rescheduled_to_appointment_id")
    private UUID rescheduledToAppointmentId; // If this was rescheduled, link to new appointment

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private ZonedDateTime changedAt;
}
