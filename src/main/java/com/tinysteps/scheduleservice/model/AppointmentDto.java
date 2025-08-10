package com.tinysteps.scheduleservice.model;

import com.tinysteps.scheduleservice.constants.AppointmentStatus;
import com.tinysteps.scheduleservice.constants.ConsultationType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppointmentDto {
    private UUID id;
    private String appointmentNumber;
    private UUID doctorId;
    private UUID patientId;
    private UUID sessionTypeId;
    private UUID practiceId;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private AppointmentStatus status;
    private ConsultationType consultationType;
    private String notes;
    private String cancellationReason;
}
