package com.tinysteps.scheduleservice.controller;

import com.tinysteps.scheduleservice.model.AppointmentDto;
import com.tinysteps.scheduleservice.model.ResponseModel;
import com.tinysteps.scheduleservice.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

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
                null
        );
    }

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
                null
        );
    }

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
                null
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('PATIENT')")
    public ResponseModel<Page<AppointmentDto>> search(
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID practiceId,
            @RequestParam(required = false) UUID sessionTypeId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String consultationType,
            Pageable pageable
    ) {
        Page<AppointmentDto> page = appointmentService.search(
                doctorId, patientId, practiceId, sessionTypeId, date, status, consultationType, pageable
        );
        return new ResponseModel<>(
                200,
                "OK",
                "Appointments retrieved successfully",
                ZonedDateTime.now(),
                page,
                null
        );
    }

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
                null
        );
    }

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
                null
        );
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or @scheduleSecurity.isAppointmentOwner(authentication, #id)")
    public ResponseModel<AppointmentDto> changeStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestParam UUID changedById,
            @RequestParam String changedByType,
            @RequestParam(required = false) String reason
    ) {
        AppointmentDto updated = appointmentService.changeStatus(id, status, changedById, changedByType, reason);
        return new ResponseModel<>(
                200,
                "OK",
                "Appointment status updated successfully",
                ZonedDateTime.now(),
                updated,
                null
        );
    }
}
