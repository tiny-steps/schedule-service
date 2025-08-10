package com.tinysteps.scheduleservice.model;

import com.tinysteps.scheduleservice.constants.AppointmentStatus;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppointmentStatusHistoryDto {
    private UUID id;
    private UUID appointmentId;
    private AppointmentStatus oldStatus;
    private AppointmentStatus newStatus;
    private UUID changedById;
    private String changedByType;
    private String reason;
    private ZonedDateTime changedAt;
}
