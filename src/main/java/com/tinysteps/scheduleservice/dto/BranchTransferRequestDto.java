package com.tinysteps.scheduleservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for branch transfer requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchTransferRequestDto {

    @NotNull(message = "Source branch ID is required")
    private UUID sourceBranchId;

    @NotNull(message = "Target branch ID is required")
    private UUID targetBranchId;

    @NotNull(message = "Transfer type is required")
    private TransferType transferType;

    // For appointment transfers
    private List<UUID> appointmentIds;
    
    // For doctor transfers
    private List<UUID> doctorIds;
    
    // For bulk transfers by date range
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Transfer options
    private boolean preserveOriginalSchedule = true;
    private boolean notifyPatients = true;
    private boolean validateDoctorAvailability = true;
    
    private String reason;
    private String notes;

    public enum TransferType {
        APPOINTMENT_TRANSFER,
        DOCTOR_TRANSFER,
        BULK_APPOINTMENT_TRANSFER,
        EMERGENCY_TRANSFER
    }
}