package com.tinysteps.scheduleservice.controller;

import com.tinysteps.scheduleservice.dto.BranchTransferRequestDto;
import com.tinysteps.scheduleservice.dto.BranchTransferResponseDto;
import com.tinysteps.scheduleservice.service.BranchTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for branch transfer operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/branch-transfer")
@RequiredArgsConstructor
@Tag(name = "Branch Transfer", description = "APIs for transferring appointments and doctors between branches")
public class BranchTransferController {

    private final BranchTransferService branchTransferService;

    @Operation(
            summary = "Transfer appointments between branches",
            description = "Transfer specific appointments or bulk transfer by date range from one branch to another"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfer completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid transfer request"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/appointments")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<BranchTransferResponseDto> transferAppointments(
            @Parameter(description = "Branch transfer request details")
            @Valid @RequestBody BranchTransferRequestDto request) {
        
        log.info("Received appointment transfer request from branch {} to branch {}", 
                request.getSourceBranchId(), request.getTargetBranchId());
        
        try {
            // Validate transfer type
            if (request.getTransferType() != BranchTransferRequestDto.TransferType.APPOINTMENT_TRANSFER &&
                request.getTransferType() != BranchTransferRequestDto.TransferType.BULK_APPOINTMENT_TRANSFER &&
                request.getTransferType() != BranchTransferRequestDto.TransferType.EMERGENCY_TRANSFER) {
                
                return ResponseEntity.badRequest()
                        .body(BranchTransferResponseDto.builder()
                                .transferId(UUID.randomUUID())
                                .status(BranchTransferResponseDto.TransferStatus.FAILED)
                                .message("Invalid transfer type for appointment transfer")
                                .errors(java.util.List.of("Transfer type must be APPOINTMENT_TRANSFER, BULK_APPOINTMENT_TRANSFER, or EMERGENCY_TRANSFER"))
                                .build());
            }
            
            BranchTransferResponseDto response = branchTransferService.transferAppointments(request);
            
            // Return appropriate HTTP status based on transfer result
            HttpStatus status = switch (response.getStatus()) {
                case SUCCESS -> HttpStatus.OK;
                case PARTIAL_SUCCESS -> HttpStatus.ACCEPTED;
                case FAILED -> HttpStatus.BAD_REQUEST;
                default -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
            
            return ResponseEntity.status(status).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid appointment transfer request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(BranchTransferResponseDto.builder()
                            .transferId(UUID.randomUUID())
                            .status(BranchTransferResponseDto.TransferStatus.FAILED)
                            .message("Invalid request: " + e.getMessage())
                            .errors(java.util.List.of(e.getMessage()))
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error during appointment transfer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BranchTransferResponseDto.builder()
                            .transferId(UUID.randomUUID())
                            .status(BranchTransferResponseDto.TransferStatus.FAILED)
                            .message("Internal server error occurred")
                            .errors(java.util.List.of("System error: " + e.getMessage()))
                            .build());
        }
    }

    @Operation(
            summary = "Transfer doctors between branches",
            description = "Transfer specific doctors from one branch to another"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfer completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid transfer request"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/doctors")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<BranchTransferResponseDto> transferDoctors(
            @Parameter(description = "Doctor transfer request details")
            @Valid @RequestBody BranchTransferRequestDto request) {
        
        log.info("Received doctor transfer request from branch {} to branch {}", 
                request.getSourceBranchId(), request.getTargetBranchId());
        
        try {
            // Validate transfer type
            if (request.getTransferType() != BranchTransferRequestDto.TransferType.DOCTOR_TRANSFER &&
                request.getTransferType() != BranchTransferRequestDto.TransferType.EMERGENCY_TRANSFER) {
                
                return ResponseEntity.badRequest()
                        .body(BranchTransferResponseDto.builder()
                                .transferId(UUID.randomUUID())
                                .status(BranchTransferResponseDto.TransferStatus.FAILED)
                                .message("Invalid transfer type for doctor transfer")
                                .errors(java.util.List.of("Transfer type must be DOCTOR_TRANSFER or EMERGENCY_TRANSFER"))
                                .build());
            }
            
            BranchTransferResponseDto response = branchTransferService.transferDoctors(request);
            
            // Return appropriate HTTP status based on transfer result
            HttpStatus status = switch (response.getStatus()) {
                case SUCCESS -> HttpStatus.OK;
                case PARTIAL_SUCCESS -> HttpStatus.ACCEPTED;
                case FAILED -> HttpStatus.BAD_REQUEST;
                default -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
            
            return ResponseEntity.status(status).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid doctor transfer request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(BranchTransferResponseDto.builder()
                            .transferId(UUID.randomUUID())
                            .status(BranchTransferResponseDto.TransferStatus.FAILED)
                            .message("Invalid request: " + e.getMessage())
                            .errors(java.util.List.of(e.getMessage()))
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error during doctor transfer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BranchTransferResponseDto.builder()
                            .transferId(UUID.randomUUID())
                            .status(BranchTransferResponseDto.TransferStatus.FAILED)
                            .message("Internal server error occurred")
                            .errors(java.util.List.of("System error: " + e.getMessage()))
                            .build());
        }
    }

    @Operation(
            summary = "Get transfer status",
            description = "Get the status and details of a previous transfer operation"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfer status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Transfer not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/status/{transferId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<String> getTransferStatus(
            @Parameter(description = "Transfer ID to check status for")
            @PathVariable UUID transferId) {
        
        log.info("Received transfer status request for transfer ID: {}", transferId);
        
        // For now, return a simple message indicating this feature is not yet implemented
        // In a full implementation, you would store transfer history and retrieve it here
        return ResponseEntity.ok("Transfer status tracking is not yet implemented. Transfer ID: " + transferId);
    }

    @Operation(
            summary = "Emergency transfer",
            description = "Perform emergency transfer of appointments and doctors (bypasses some validations)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Emergency transfer completed"),
            @ApiResponse(responseCode = "400", description = "Invalid emergency transfer request"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/emergency")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BranchTransferResponseDto> emergencyTransfer(
            @Parameter(description = "Emergency transfer request details")
            @Valid @RequestBody BranchTransferRequestDto request) {
        
        log.warn("Received EMERGENCY transfer request from branch {} to branch {}", 
                request.getSourceBranchId(), request.getTargetBranchId());
        
        try {
            // Force transfer type to emergency
            request.setTransferType(BranchTransferRequestDto.TransferType.EMERGENCY_TRANSFER);
            
            // Determine what to transfer based on request content
            BranchTransferResponseDto response;
            if (request.getDoctorIds() != null && !request.getDoctorIds().isEmpty()) {
                response = branchTransferService.transferDoctors(request);
            } else {
                response = branchTransferService.transferAppointments(request);
            }
            
            // Return appropriate HTTP status based on transfer result
            HttpStatus status = switch (response.getStatus()) {
                case SUCCESS -> HttpStatus.OK;
                case PARTIAL_SUCCESS -> HttpStatus.ACCEPTED;
                case FAILED -> HttpStatus.BAD_REQUEST;
                default -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
            
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            log.error("Critical error during emergency transfer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BranchTransferResponseDto.builder()
                            .transferId(UUID.randomUUID())
                            .status(BranchTransferResponseDto.TransferStatus.FAILED)
                            .message("Emergency transfer failed")
                            .errors(java.util.List.of("Critical system error: " + e.getMessage()))
                            .build());
        }
    }
}