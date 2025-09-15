package com.tinysteps.scheduleservice.controller;

import com.tinysteps.scheduleservice.model.AppointmentDto;
import com.tinysteps.scheduleservice.model.ResponseModel;
import com.tinysteps.scheduleservice.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment Management", description = "APIs for managing appointments")
@SecurityRequirement(name = "Bearer Authentication")
public class AppointmentController {

        private final AppointmentService appointmentService;

        @Operation(summary = "Create a new appointment", description = "Creates a new appointment with the provided information")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Appointment created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data"),
                        @ApiResponse(responseCode = "403", description = "Access denied")
        })
        @PostMapping
        @PreAuthorize("hasRole('ADMIN') or @scheduleSecurity.isDoctorOwner(authentication, #dto.doctorId)")
        public ResponseModel<AppointmentDto> create(@RequestBody AppointmentDto dto) {
                AppointmentDto created = appointmentService.create(dto);
                return new ResponseModel<>(
                                201,
                                "CREATED",
                                "Appointment created successfully",
                                ZonedDateTime.now(),
                                created,
                                null);
        }

        @Operation(summary = "Get appointment by ID", description = "Retrieves an appointment by its unique identifier")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Appointment found"),
                        @ApiResponse(responseCode = "404", description = "Appointment not found"),
                        @ApiResponse(responseCode = "403", description = "Access denied")
        })
        @GetMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN') or @scheduleSecurity.isAppointmentParticipant(authentication, #id)")
        public ResponseModel<AppointmentDto> getById(@PathVariable UUID id) {
                AppointmentDto dto = appointmentService.getById(id);
                return new ResponseModel<>(
                                200,
                                "OK",
                                "Appointment retrieved successfully",
                                ZonedDateTime.now(),
                                dto,
                                null);
        }

        @Operation(summary = "Get appointment by number", description = "Retrieves an appointment by its appointment number")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Appointment found"),
                        @ApiResponse(responseCode = "404", description = "Appointment not found"),
                        @ApiResponse(responseCode = "403", description = "Access denied")
        })
        @GetMapping("/number/{appointmentNumber}")
        @PreAuthorize("hasRole('ADMIN') or @scheduleSecurity.isAppointmentParticipant(authentication, #appointmentNumber)")
        public ResponseModel<AppointmentDto> getByNumber(@PathVariable String appointmentNumber) {
                AppointmentDto dto = appointmentService.getByNumber(appointmentNumber);
                return new ResponseModel<>(
                                200,
                                "OK",
                                "Appointment retrieved successfully",
                                ZonedDateTime.now(),
                                dto,
                                null);
        }

        @Operation(summary = "Search appointments", description = "Search for appointments with multiple criteria")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Appointments retrieved successfully"),
                        @ApiResponse(responseCode = "403", description = "Access denied")
        })
        @GetMapping
        @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('PATIENT') or @securityService.isInternalServiceCall()")
        public ResponseModel<Page<AppointmentDto>> search(
                        @Parameter(description = "Doctor ID") @RequestParam(required = false) UUID doctorId,
                        @Parameter(description = "Patient ID") @RequestParam(required = false) UUID patientId,
                        @Parameter(description = "Practice ID") @RequestParam(required = false) UUID practiceId,
                        @Parameter(description = "Session type ID") @RequestParam(required = false) UUID sessionTypeId,
                        @Parameter(description = "Branch ID or 'all'") @RequestParam(required = false, name = "branchId") String branchIdStr,
                        @Parameter(description = "Single date (YYYY-MM-DD)") @RequestParam(required = false) LocalDate date,
                        @Parameter(description = "Start date (YYYY-MM-DD)") @RequestParam(required = false) LocalDate startDate,
                        @Parameter(description = "End date (YYYY-MM-DD)") @RequestParam(required = false) LocalDate endDate,
                        @Parameter(description = "Status (comma separated)") @RequestParam(required = false) String status,
                        @Parameter(description = "Consultation type") @RequestParam(required = false) String consultationType,
                        @Parameter(description = "Pagination information") Pageable pageable) {

                UUID branchId = null;
                if (branchIdStr != null && !branchIdStr.isBlank() && !branchIdStr.equalsIgnoreCase("all")) {
                        branchId = UUID.fromString(branchIdStr);
                }
                Page<AppointmentDto> page = appointmentService.search(
                                doctorId, patientId, practiceId, sessionTypeId, branchId,
                                date, startDate, endDate,
                                status, consultationType,
                                pageable);
                return new ResponseModel<>(
                                200,
                                "OK",
                                "Appointments retrieved successfully",
                                ZonedDateTime.now(),
                                page,
                                null);
        }

        @Operation(summary = "Update appointment", description = "Updates an appointment with new information")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Appointment updated successfully"),
                        @ApiResponse(responseCode = "404", description = "Appointment not found"),
                        @ApiResponse(responseCode = "403", description = "Access denied")
        })
        @PutMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN') or @scheduleSecurity.isAppointmentOwner(authentication, #id)")
        public ResponseModel<AppointmentDto> update(@PathVariable UUID id, @RequestBody AppointmentDto dto) {
                AppointmentDto updated = appointmentService.update(id, dto);
                return new ResponseModel<>(
                                200,
                                "OK",
                                "Appointment updated successfully",
                                ZonedDateTime.now(),
                                updated,
                                null);
        }

        @Operation(summary = "Delete appointment", description = "Deletes an appointment")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Appointment deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Appointment not found"),
                        @ApiResponse(responseCode = "403", description = "Access denied")
        })
        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN') or @scheduleSecurity.isAppointmentOwner(authentication, #id)")
        public ResponseModel<Void> delete(@PathVariable UUID id) {
                appointmentService.delete(id);
                return new ResponseModel<>(
                                200,
                                "OK",
                                "Appointment deleted successfully",
                                ZonedDateTime.now(),
                                null,
                                null);
        }

        @Operation(summary = "Change appointment status", description = "Changes the status of an appointment")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Appointment status updated successfully"),
                        @ApiResponse(responseCode = "404", description = "Appointment not found"),
                        @ApiResponse(responseCode = "403", description = "Access denied")
        })
        @PostMapping("/{id}/status")
        @PreAuthorize("hasRole('ADMIN') or @scheduleSecurity.isAppointmentOwner(authentication, #id)")
        public ResponseModel<AppointmentDto> changeStatus(
                        @Parameter(description = "Appointment ID", required = true) @PathVariable UUID id,
                        @Parameter(description = "New status") @RequestParam String status,
                        @Parameter(description = "User ID who changed the status") @RequestParam UUID changedById,
                        @Parameter(description = "Reason for status change") @RequestParam(required = false) String reason,
                        @Parameter(description = "Cancellation type") @RequestParam(required = false) String cancellationType,
                        @Parameter(description = "Rescheduled appointment ID") @RequestParam(required = false) UUID rescheduledToAppointmentId) {
                AppointmentDto updated = appointmentService.changeStatus(id, status, changedById, reason,
                                cancellationType, rescheduledToAppointmentId);
                return new ResponseModel<>(
                                200,
                                "OK",
                                "Appointment status updated successfully",
                                ZonedDateTime.now(),
                                updated,
                                null);
        }

        @Operation(summary = "Check time slot conflict", description = "Checks if a time slot has a conflict with existing appointments")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Time slot conflict check completed")
        })
        @GetMapping("/conflicts")
        @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('PATIENT')")
        public ResponseModel<Boolean> checkTimeSlotConflict(
                        @Parameter(description = "Doctor ID") @RequestParam UUID doctorId,
                        @Parameter(description = "Date") @RequestParam LocalDate date,
                        @Parameter(description = "Start time") @RequestParam String startTime,
                        @Parameter(description = "End time") @RequestParam String endTime) {
                boolean hasConflict = appointmentService.hasTimeSlotConflict(doctorId, date, startTime, endTime);
                return new ResponseModel<>(
                                200,
                                "OK",
                                "Time slot conflict check completed",
                                ZonedDateTime.now(),
                                hasConflict,
                                null);
        }

        @Operation(summary = "Get existing appointments", description = "Retrieves existing appointments for a doctor on a specific date")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Existing appointments retrieved successfully")
        })
        @GetMapping("/existing")
        @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('PATIENT')")
        public ResponseModel<List<AppointmentDto>> getExistingAppointments(
                        @Parameter(description = "Doctor ID") @RequestParam UUID doctorId,
                        @Parameter(description = "Date") @RequestParam LocalDate date,
                        @Parameter(description = "Status filter (comma-separated)") @RequestParam(required = false, defaultValue = "SCHEDULED,CONFIRMED") String status) {
                List<AppointmentDto> appointments = appointmentService.getExistingAppointments(doctorId, date, status);
                return new ResponseModel<>(
                                200,
                                "OK",
                                "Existing appointments retrieved successfully",
                                ZonedDateTime.now(),
                                appointments,
                                null);
        }

        // ==================== NEW BRANCH-BASED ENDPOINTS ====================

        @Operation(summary = "Get all appointments across all branches (Admin only via /branch/all)", description = "Retrieves a paginated list of all appointments across all branches. Only accessible by ADMIN users.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "All appointments retrieved successfully"),
                        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
        })
        @GetMapping("/branch/all")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseModel<Page<AppointmentDto>> getAppointmentsAllBranchesAlias(
                        @Parameter(description = "Doctor ID") @RequestParam(required = false) UUID doctorId,
                        @Parameter(description = "Patient ID") @RequestParam(required = false) UUID patientId,
                        @Parameter(description = "Practice ID") @RequestParam(required = false) UUID practiceId,
                        @Parameter(description = "Session type ID") @RequestParam(required = false) UUID sessionTypeId,
                        @Parameter(description = "Single date (YYYY-MM-DD)") @RequestParam(required = false) LocalDate date,
                        @Parameter(description = "Start date (YYYY-MM-DD)") @RequestParam(required = false) LocalDate startDate,
                        @Parameter(description = "End date (YYYY-MM-DD)") @RequestParam(required = false) LocalDate endDate,
                        @Parameter(description = "Status (comma separated)") @RequestParam(required = false) String status,
                        @Parameter(description = "Consultation type") @RequestParam(required = false) String consultationType,
                        @Parameter(description = "Pagination information") Pageable pageable) {
                Page<AppointmentDto> page = appointmentService.search(
                                doctorId, patientId, practiceId, sessionTypeId, null,
                                date, startDate, endDate, status, consultationType, pageable);
                return new ResponseModel<>(
                                200,
                                "OK",
                                "All appointments across all branches retrieved successfully",
                                ZonedDateTime.now(),
                                page,
                                null);
        }

        @Operation(summary = "Get appointments for a specific branch", description = "Retrieves a paginated list of appointments for a specific branch. Users can only access branches they have permission to view.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Appointments for branch retrieved successfully"),
                        @ApiResponse(responseCode = "403", description = "Access denied - No permission to view this branch")
        })
        @GetMapping("/branch/{branchId}")
        @PreAuthorize("@securityService.hasBranchAccess(#branchId) or hasRole('ADMIN')")
        public ResponseModel<Page<AppointmentDto>> getAppointmentsForBranch(
                        @Parameter(description = "Branch ID", required = true) @PathVariable UUID branchId,
                        @Parameter(description = "Doctor ID") @RequestParam(required = false) UUID doctorId,
                        @Parameter(description = "Patient ID") @RequestParam(required = false) UUID patientId,
                        @Parameter(description = "Practice ID") @RequestParam(required = false) UUID practiceId,
                        @Parameter(description = "Session type ID") @RequestParam(required = false) UUID sessionTypeId,
                        @Parameter(description = "Date") @RequestParam(required = false) LocalDate date,
                        @Parameter(description = "Status") @RequestParam(required = false) String status,
                        @Parameter(description = "Consultation type") @RequestParam(required = false) String consultationType,
                        @Parameter(description = "Pagination information") Pageable pageable) {
                Page<AppointmentDto> page = appointmentService.searchByBranch(
                                branchId, doctorId, patientId, practiceId, sessionTypeId, date, status,
                                consultationType,
                                pageable);
                return new ResponseModel<>(
                                200,
                                "OK",
                                "Appointments for branch retrieved successfully",
                                ZonedDateTime.now(),
                                page,
                                null);
        }

        @Operation(summary = "Get appointments for current user's branch", description = "Retrieves a paginated list of appointments for the current user's primary branch.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Appointments for current user's branch retrieved successfully")
        })
        @GetMapping("/my-branch")
        @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
        public ResponseModel<Page<AppointmentDto>> getAppointmentsForCurrentUserBranch(
                        @Parameter(description = "Doctor ID") @RequestParam(required = false) UUID doctorId,
                        @Parameter(description = "Patient ID") @RequestParam(required = false) UUID patientId,
                        @Parameter(description = "Practice ID") @RequestParam(required = false) UUID practiceId,
                        @Parameter(description = "Session type ID") @RequestParam(required = false) UUID sessionTypeId,
                        @Parameter(description = "Date") @RequestParam(required = false) LocalDate date,
                        @Parameter(description = "Status") @RequestParam(required = false) String status,
                        @Parameter(description = "Consultation type") @RequestParam(required = false) String consultationType,
                        @Parameter(description = "Pagination information") Pageable pageable) {
                Page<AppointmentDto> page = appointmentService.searchByCurrentUserBranch(
                                doctorId, patientId, practiceId, sessionTypeId, date, status, consultationType,
                                pageable);
                return new ResponseModel<>(
                                200,
                                "OK",
                                "Appointments for current user's branch retrieved successfully",
                                ZonedDateTime.now(),
                                page,
                                null);
        }

        @Operation(summary = "Search appointments across all branches (Admin only)", description = "Advanced search for appointments across all branches. Only accessible by ADMIN users.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Search across all branches completed successfully"),
                        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
        })
        @GetMapping("/search/all-branches")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseModel<Page<AppointmentDto>> searchAppointmentsAcrossAllBranches(
                        @Parameter(description = "Doctor ID") @RequestParam(required = false) UUID doctorId,
                        @Parameter(description = "Patient ID") @RequestParam(required = false) UUID patientId,
                        @Parameter(description = "Practice ID") @RequestParam(required = false) UUID practiceId,
                        @Parameter(description = "Session type ID") @RequestParam(required = false) UUID sessionTypeId,
                        @Parameter(description = "Single date (YYYY-MM-DD)") @RequestParam(required = false) LocalDate date,
                        @Parameter(description = "Start date (YYYY-MM-DD)") @RequestParam(required = false) LocalDate startDate,
                        @Parameter(description = "End date (YYYY-MM-DD)") @RequestParam(required = false) LocalDate endDate,
                        @Parameter(description = "Status (comma separated)") @RequestParam(required = false) String status,
                        @Parameter(description = "Consultation type") @RequestParam(required = false) String consultationType,
                        @Parameter(description = "Pagination information") Pageable pageable) {
                Page<AppointmentDto> page = appointmentService.search(
                                doctorId, patientId, practiceId, sessionTypeId, null,
                                date, startDate, endDate, status, consultationType, pageable);
                return new ResponseModel<>(
                                200,
                                "OK",
                                "Search across all branches completed successfully",
                                ZonedDateTime.now(),
                                page,
                                null);
        }

        @Operation(summary = "Search appointments in a specific branch", description = "Advanced search for appointments within a specific branch. Users can only search in branches they have permission to view.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Search in branch completed successfully"),
                        @ApiResponse(responseCode = "403", description = "Access denied - No permission to view this branch")
        })
        @GetMapping("/search/branch/{branchId}")
        @PreAuthorize("@securityService.hasBranchAccess(#branchId) or hasRole('ADMIN')")
        public ResponseModel<Page<AppointmentDto>> searchAppointmentsInBranch(
                        @Parameter(description = "Branch ID", required = true) @PathVariable UUID branchId,
                        @Parameter(description = "Doctor ID") @RequestParam(required = false) UUID doctorId,
                        @Parameter(description = "Patient ID") @RequestParam(required = false) UUID patientId,
                        @Parameter(description = "Practice ID") @RequestParam(required = false) UUID practiceId,
                        @Parameter(description = "Session type ID") @RequestParam(required = false) UUID sessionTypeId,
                        @Parameter(description = "Date") @RequestParam(required = false) LocalDate date,
                        @Parameter(description = "Status") @RequestParam(required = false) String status,
                        @Parameter(description = "Consultation type") @RequestParam(required = false) String consultationType,
                        @Parameter(description = "Pagination information") Pageable pageable) {
                Page<AppointmentDto> page = appointmentService.searchByBranch(
                                branchId, doctorId, patientId, practiceId, sessionTypeId, date, status,
                                consultationType,
                                pageable);
                return new ResponseModel<>(
                                200,
                                "OK",
                                "Search in branch completed successfully",
                                ZonedDateTime.now(),
                                page,
                                null);
        }

        @Operation(summary = "Get appointment statistics for a specific branch", description = "Retrieves various statistics about appointments in a specific branch.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Branch statistics retrieved successfully"),
                        @ApiResponse(responseCode = "403", description = "Access denied - No permission to view this branch")
        })
        @GetMapping("/statistics/branch/{branchId}")
        @PreAuthorize("@securityService.hasBranchAccess(#branchId) or hasRole('ADMIN')")
        public ResponseModel<Map<String, Object>> getAppointmentStatisticsForBranch(
                        @Parameter(description = "Branch ID", required = true) @PathVariable UUID branchId) {
                Map<String, Object> statistics = appointmentService.getBranchStatistics(branchId);
                return new ResponseModel<>(
                                200,
                                "OK",
                                "Branch statistics retrieved successfully",
                                ZonedDateTime.now(),
                                statistics,
                                null);
        }

        @Operation(summary = "Get appointment statistics for current user's branch", description = "Retrieves various statistics about appointments in the current user's primary branch.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Current branch statistics retrieved successfully")
        })
        @GetMapping("/statistics/my-branch")
        @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
        public ResponseModel<Map<String, Object>> getAppointmentStatisticsForCurrentUserBranch() {
                Map<String, Object> statistics = appointmentService.getCurrentUserBranchStatistics();
                return new ResponseModel<>(
                                200,
                                "OK",
                                "Current branch statistics retrieved successfully",
                                ZonedDateTime.now(),
                                statistics,
                                null);
        }

        @Operation(summary = "Get appointment statistics across all branches (Admin only)", description = "Retrieves various statistics about appointments across all branches. Only accessible by ADMIN users.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "All branches statistics retrieved successfully"),
                        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
        })
        @GetMapping("/statistics/all-branches")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseModel<Map<String, Object>> getAppointmentStatisticsAcrossAllBranches() {
                Map<String, Object> statistics = appointmentService.getAllBranchesStatistics();
                return new ResponseModel<>(
                                200,
                                "OK",
                                "All branches statistics retrieved successfully",
                                ZonedDateTime.now(),
                                statistics,
                                null);
        }
}
