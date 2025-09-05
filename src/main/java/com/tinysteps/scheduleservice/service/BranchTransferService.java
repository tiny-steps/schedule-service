package com.tinysteps.scheduleservice.service;

import com.tinysteps.scheduleservice.dto.BranchTransferRequestDto;
import com.tinysteps.scheduleservice.dto.BranchTransferResponseDto;
import com.tinysteps.scheduleservice.entity.Appointment;
import com.tinysteps.scheduleservice.repository.AppointmentRepository;
import com.tinysteps.scheduleservice.service.external.DoctorServiceClient;
import com.tinysteps.scheduleservice.service.external.AddressServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling branch transfer operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BranchTransferService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorServiceClient doctorServiceClient;
    private final AddressServiceClient addressServiceClient;
    private final AppointmentService appointmentService;

    /**
     * Transfer appointments between branches
     */
    @Transactional
    public BranchTransferResponseDto transferAppointments(BranchTransferRequestDto request) {
        log.info("Starting appointment transfer from branch {} to branch {}", 
                request.getSourceBranchId(), request.getTargetBranchId());

        UUID transferId = UUID.randomUUID();
        List<BranchTransferResponseDto.TransferItemResult> results = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            // Validate branches exist
            validateBranches(request.getSourceBranchId(), request.getTargetBranchId());

            List<UUID> appointmentIds = getAppointmentIds(request);
            
            if (appointmentIds.isEmpty()) {
                return BranchTransferResponseDto.builder()
                        .transferId(transferId)
                        .status(BranchTransferResponseDto.TransferStatus.FAILED)
                        .message("No appointments found for transfer")
                        .transferredAt(LocalDateTime.now())
                        .totalItemsRequested(0)
                        .successfulTransfers(0)
                        .failedTransfers(0)
                        .errors(List.of("No appointments found matching the criteria"))
                        .build();
            }

            // Process each appointment
            for (UUID appointmentId : appointmentIds) {
                try {
                    BranchTransferResponseDto.TransferItemResult result = 
                            transferSingleAppointment(appointmentId, request);
                    results.add(result);
                    
                    if (!result.isSuccess()) {
                        errors.add(String.format("Failed to transfer appointment %s: %s", 
                                appointmentId, result.getErrorMessage()));
                    }
                } catch (Exception e) {
                    log.error("Error transferring appointment {}: {}", appointmentId, e.getMessage(), e);
                    results.add(BranchTransferResponseDto.TransferItemResult.builder()
                            .itemId(appointmentId)
                            .itemType("APPOINTMENT")
                            .success(false)
                            .errorMessage(e.getMessage())
                            .build());
                    errors.add(String.format("Exception transferring appointment %s: %s", 
                            appointmentId, e.getMessage()));
                }
            }

            // Calculate summary
            int successful = (int) results.stream().filter(BranchTransferResponseDto.TransferItemResult::isSuccess).count();
            int failed = results.size() - successful;

            BranchTransferResponseDto.TransferStatus status;
            if (successful == results.size()) {
                status = BranchTransferResponseDto.TransferStatus.SUCCESS;
            } else if (successful > 0) {
                status = BranchTransferResponseDto.TransferStatus.PARTIAL_SUCCESS;
            } else {
                status = BranchTransferResponseDto.TransferStatus.FAILED;
            }

            return BranchTransferResponseDto.builder()
                    .transferId(transferId)
                    .status(status)
                    .message(String.format("Transfer completed: %d successful, %d failed", successful, failed))
                    .transferredAt(LocalDateTime.now())
                    .totalItemsRequested(appointmentIds.size())
                    .successfulTransfers(successful)
                    .failedTransfers(failed)
                    .transferResults(results)
                    .warnings(warnings)
                    .errors(errors)
                    .rollbackAvailable(successful > 0)
                    .rollbackId(successful > 0 ? UUID.randomUUID() : null)
                    .build();

        } catch (Exception e) {
            log.error("Critical error during appointment transfer: {}", e.getMessage(), e);
            return BranchTransferResponseDto.builder()
                    .transferId(transferId)
                    .status(BranchTransferResponseDto.TransferStatus.FAILED)
                    .message("Transfer failed due to system error")
                    .transferredAt(LocalDateTime.now())
                    .totalItemsRequested(0)
                    .successfulTransfers(0)
                    .failedTransfers(0)
                    .errors(List.of("System error: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Transfer doctors between branches
     */
    @Transactional
    public BranchTransferResponseDto transferDoctors(BranchTransferRequestDto request) {
        log.info("Starting doctor transfer from branch {} to branch {}", 
                request.getSourceBranchId(), request.getTargetBranchId());

        UUID transferId = UUID.randomUUID();
        List<BranchTransferResponseDto.TransferItemResult> results = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            // Validate branches exist
            validateBranches(request.getSourceBranchId(), request.getTargetBranchId());

            if (request.getDoctorIds() == null || request.getDoctorIds().isEmpty()) {
                return BranchTransferResponseDto.builder()
                        .transferId(transferId)
                        .status(BranchTransferResponseDto.TransferStatus.FAILED)
                        .message("No doctors specified for transfer")
                        .transferredAt(LocalDateTime.now())
                        .errors(List.of("Doctor IDs list is empty"))
                        .build();
            }

            // Process each doctor
            for (UUID doctorId : request.getDoctorIds()) {
                try {
                    BranchTransferResponseDto.TransferItemResult result = 
                            transferSingleDoctor(doctorId, request);
                    results.add(result);
                    
                    if (!result.isSuccess()) {
                        errors.add(String.format("Failed to transfer doctor %s: %s", 
                                doctorId, result.getErrorMessage()));
                    }
                } catch (Exception e) {
                    log.error("Error transferring doctor {}: {}", doctorId, e.getMessage(), e);
                    results.add(BranchTransferResponseDto.TransferItemResult.builder()
                            .itemId(doctorId)
                            .itemType("DOCTOR")
                            .success(false)
                            .errorMessage(e.getMessage())
                            .build());
                    errors.add(String.format("Exception transferring doctor %s: %s", 
                            doctorId, e.getMessage()));
                }
            }

            // Calculate summary
            int successful = (int) results.stream().filter(BranchTransferResponseDto.TransferItemResult::isSuccess).count();
            int failed = results.size() - successful;

            BranchTransferResponseDto.TransferStatus status;
            if (successful == results.size()) {
                status = BranchTransferResponseDto.TransferStatus.SUCCESS;
            } else if (successful > 0) {
                status = BranchTransferResponseDto.TransferStatus.PARTIAL_SUCCESS;
            } else {
                status = BranchTransferResponseDto.TransferStatus.FAILED;
            }

            return BranchTransferResponseDto.builder()
                    .transferId(transferId)
                    .status(status)
                    .message(String.format("Doctor transfer completed: %d successful, %d failed", successful, failed))
                    .transferredAt(LocalDateTime.now())
                    .totalItemsRequested(request.getDoctorIds().size())
                    .successfulTransfers(successful)
                    .failedTransfers(failed)
                    .transferResults(results)
                    .warnings(warnings)
                    .errors(errors)
                    .rollbackAvailable(successful > 0)
                    .rollbackId(successful > 0 ? UUID.randomUUID() : null)
                    .build();

        } catch (Exception e) {
            log.error("Critical error during doctor transfer: {}", e.getMessage(), e);
            return BranchTransferResponseDto.builder()
                    .transferId(transferId)
                    .status(BranchTransferResponseDto.TransferStatus.FAILED)
                    .message("Doctor transfer failed due to system error")
                    .transferredAt(LocalDateTime.now())
                    .totalItemsRequested(0)
                    .successfulTransfers(0)
                    .failedTransfers(0)
                    .errors(List.of("System error: " + e.getMessage()))
                    .build();
        }
    }

    private List<UUID> getAppointmentIds(BranchTransferRequestDto request) {
        if (request.getAppointmentIds() != null && !request.getAppointmentIds().isEmpty()) {
            return request.getAppointmentIds();
        }

        // For bulk transfers by date range
        if (request.getStartDate() != null && request.getEndDate() != null) {
            return appointmentRepository.findByBranchIdAndAppointmentDateBetween(
                    request.getSourceBranchId(),
                    request.getStartDate(),
                    request.getEndDate()
            ).stream().map(Appointment::getId).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private BranchTransferResponseDto.TransferItemResult transferSingleAppointment(
            UUID appointmentId, BranchTransferRequestDto request) {
        
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            return BranchTransferResponseDto.TransferItemResult.builder()
                    .itemId(appointmentId)
                    .itemType("APPOINTMENT")
                    .success(false)
                    .errorMessage("Appointment not found")
                    .build();
        }

        Appointment appointment = appointmentOpt.get();
        
        // Validate appointment belongs to source branch
        if (!appointment.getBranchId().equals(request.getSourceBranchId())) {
            return BranchTransferResponseDto.TransferItemResult.builder()
                    .itemId(appointmentId)
                    .itemType("APPOINTMENT")
                    .success(false)
                    .errorMessage("Appointment does not belong to source branch")
                    .build();
        }

        try {
            // Update appointment branch
            appointment.setBranchId(request.getTargetBranchId());
            appointmentRepository.save(appointment);

            log.info("Successfully transferred appointment {} from branch {} to branch {}", 
                    appointmentId, request.getSourceBranchId(), request.getTargetBranchId());

            return BranchTransferResponseDto.TransferItemResult.builder()
                    .itemId(appointmentId)
                    .itemType("APPOINTMENT")
                    .success(true)
                    .newItemId(appointmentId)
                    .build();

        } catch (Exception e) {
            log.error("Failed to transfer appointment {}: {}", appointmentId, e.getMessage(), e);
            return BranchTransferResponseDto.TransferItemResult.builder()
                    .itemId(appointmentId)
                    .itemType("APPOINTMENT")
                    .success(false)
                    .errorMessage("Database error: " + e.getMessage())
                    .build();
        }
    }

    private BranchTransferResponseDto.TransferItemResult transferSingleDoctor(
            UUID doctorId, BranchTransferRequestDto request) {
        
        try {
            // Call doctor service to transfer doctor between branches
            boolean success = doctorServiceClient.transferDoctorBetweenBranches(
                    doctorId, 
                    request.getSourceBranchId(), 
                    request.getTargetBranchId()
            );

            if (success) {
                log.info("Successfully transferred doctor {} from branch {} to branch {}", 
                        doctorId, request.getSourceBranchId(), request.getTargetBranchId());

                return BranchTransferResponseDto.TransferItemResult.builder()
                        .itemId(doctorId)
                        .itemType("DOCTOR")
                        .success(true)
                        .newItemId(doctorId)
                        .build();
            } else {
                return BranchTransferResponseDto.TransferItemResult.builder()
                        .itemId(doctorId)
                        .itemType("DOCTOR")
                        .success(false)
                        .errorMessage("Doctor service transfer failed")
                        .build();
            }

        } catch (Exception e) {
            log.error("Failed to transfer doctor {}: {}", doctorId, e.getMessage(), e);
            return BranchTransferResponseDto.TransferItemResult.builder()
                    .itemId(doctorId)
                    .itemType("DOCTOR")
                    .success(false)
                    .errorMessage("Service error: " + e.getMessage())
                    .build();
        }
    }

    private void validateBranches(UUID sourceBranchId, UUID targetBranchId) {
        if (sourceBranchId.equals(targetBranchId)) {
            throw new IllegalArgumentException("Source and target branches cannot be the same");
        }

        // Validate branches exist through address service
        try {
            boolean sourceExists = addressServiceClient.branchExists(sourceBranchId);
            boolean targetExists = addressServiceClient.branchExists(targetBranchId);

            if (!sourceExists) {
                throw new IllegalArgumentException("Source branch does not exist: " + sourceBranchId);
            }
            if (!targetExists) {
                throw new IllegalArgumentException("Target branch does not exist: " + targetBranchId);
            }
        } catch (Exception e) {
            log.error("Error validating branches: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to validate branch existence", e);
        }
    }
}