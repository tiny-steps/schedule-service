package com.tinysteps.scheduleservice.entity;

import com.tinysteps.scheduleservice.constants.AppointmentStatus;
import com.tinysteps.scheduleservice.constants.ConsultationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments", uniqueConstraints = {
        @UniqueConstraint(name = "uq_doctor_practice_slot",
                columnNames = {"doctor_id", "practice_id", "appointment_date", "start_time"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "appointment_number", length = 20, nullable = false, unique = true)
    private String appointmentNumber;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "session_type_id", nullable = false)
    private UUID sessionTypeId;

    @Column(name = "practice_id")
    private UUID practiceId;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_type", nullable = false)
    private ConsultationType consultationType = ConsultationType.IN_PERSON;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
