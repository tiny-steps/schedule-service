package com.tinysteps.scheduleservice.service;

import com.tinysteps.scheduleservice.model.AppointmentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AppointmentService {

        AppointmentDto create(AppointmentDto dto);

        AppointmentDto getById(UUID id);

        AppointmentDto getByNumber(String appointmentNumber);

        Page<AppointmentDto> search(UUID doctorId,
                        UUID patientId,
                        UUID practiceId,
                        UUID sessionTypeId,
                        LocalDate date,
                        String status,
                        String consultationType,
                        Pageable pageable);

        AppointmentDto update(UUID id, AppointmentDto dto);

        void delete(UUID id);

        AppointmentDto changeStatus(UUID id, String newStatus, UUID changedById, String reason,
                        String cancellationType, UUID rescheduledToAppointmentId);

        boolean hasTimeSlotConflict(UUID doctorId, LocalDate date, String startTime, String endTime);

        List<AppointmentDto> getExistingAppointments(UUID doctorId, LocalDate date, String status);
}
