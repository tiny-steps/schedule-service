package com.tinysteps.scheduleservice.controller;

import com.tinysteps.scheduleservice.model.AppointmentStatusHistoryDto;
import com.tinysteps.scheduleservice.model.ResponseModel;
import com.tinysteps.scheduleservice.service.AppointmentStatusHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments/{appointmentId}/history")
@RequiredArgsConstructor
public class AppointmentStatusHistoryController {

    private final AppointmentStatusHistoryService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @scheduleSecurity.isAppointmentParticipant(authentication, #appointmentId)")
    public ResponseModel<List<AppointmentStatusHistoryDto>> getHistory(@PathVariable UUID appointmentId) {
        List<AppointmentStatusHistoryDto> history = service.getHistoryByAppointmentId(appointmentId);
        return new ResponseModel<>(
                200,
                "OK",
                "Status history retrieved successfully",
                ZonedDateTime.now(),
                history,
                null
        );
    }
}
