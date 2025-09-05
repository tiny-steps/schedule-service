package com.tinysteps.scheduleservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for branch transfer responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchTransferResponseDto {

    private UUID transferId;
    private TransferStatus status;
    private String message;
    private LocalDateTime transferredAt;
    
    // Transfer summary
    private int totalItemsRequested;
    private int successfulTransfers;
    private int failedTransfers;
    
    // Details
    private List<TransferItemResult> transferResults;
    private List<String> warnings;
    private List<String> errors;
    
    // Rollback information
    private boolean rollbackAvailable;
    private UUID rollbackId;

    public enum TransferStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILED,
        PENDING,
        ROLLED_BACK
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferItemResult {
        private UUID itemId;
        private String itemType; // "APPOINTMENT" or "DOCTOR"
        private boolean success;
        private String errorMessage;
        private UUID newItemId; // For cases where new records are created
    }
}