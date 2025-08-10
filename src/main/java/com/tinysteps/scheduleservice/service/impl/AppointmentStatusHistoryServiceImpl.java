package com.tinysteps.scheduleservice.service.impl;

import com.tinysteps.scheduleservice.exception.ResourceNotFoundException;
import com.tinysteps.scheduleservice.mappers.AppointmentStatusHistoryMapper;
import com.tinysteps.scheduleservice.model.AppointmentStatusHistoryDto;
import com.tinysteps.scheduleservice.repository.AppointmentRepository;
import com.tinysteps.scheduleservice.repository.AppointmentStatusHistoryRepository;
import com.tinysteps.scheduleservice.service.AppointmentStatusHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentStatusHistoryServiceImpl implements AppointmentStatusHistoryService {

    private final AppointmentStatusHistoryRepository repository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentStatusHistoryMapper mapper;

    @Override
    public List<AppointmentStatusHistoryDto> getHistoryByAppointmentId(UUID appointmentId) {
        // Validate appointment exists
        appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + appointmentId));

        return repository.findByAppointment_IdOrderByChangedAtDesc(appointmentId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }
}
