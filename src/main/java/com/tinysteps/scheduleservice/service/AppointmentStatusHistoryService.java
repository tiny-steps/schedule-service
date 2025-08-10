package com.tinysteps.scheduleservice.service;

import com.tinysteps.scheduleservice.model.AppointmentStatusHistoryDto;

import java.util.List;
import java.util.UUID;

public interface AppointmentStatusHistoryService {
    List<AppointmentStatusHistoryDto> getHistoryByAppointmentId(UUID appointmentId);
}
