package com.tinysteps.scheduleservice.service.impl;

import com.tinysteps.scheduleservice.constants.ConsultationType;
import com.tinysteps.scheduleservice.entity.Appointment;
import com.tinysteps.scheduleservice.constants.AppointmentStatus;
import com.tinysteps.scheduleservice.entity.AppointmentStatusHistory;
import com.tinysteps.scheduleservice.exception.ResourceConflictException;
import com.tinysteps.scheduleservice.exception.ResourceNotFoundException;
import com.tinysteps.scheduleservice.mappers.AppointmentMapper;
import com.tinysteps.scheduleservice.mappers.AppointmentStatusHistoryMapper;
import com.tinysteps.scheduleservice.model.AppointmentDto;
import com.tinysteps.scheduleservice.repository.AppointmentRepository;
import com.tinysteps.scheduleservice.repository.AppointmentStatusHistoryRepository;
import com.tinysteps.scheduleservice.service.AppointmentService;
import com.tinysteps.scheduleservice.specification.AppointmentSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.tinysteps.scheduleservice.constants.AppointmentStatus.valueOf;
import com.tinysteps.scheduleservice.constants.CancellationType;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentStatusHistoryRepository historyRepository;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentStatusHistoryMapper historyMapper;

    @Override
    public AppointmentDto create(AppointmentDto dto) {
        // Prevent double booking
        if (appointmentRepository.existsByDoctorIdAndPracticeIdAndAppointmentDateAndStartTime(
                dto.getDoctorId(), dto.getPracticeId(), dto.getAppointmentDate(), dto.getStartTime())) {
            throw new ResourceConflictException("Time slot already booked for this doctor/practice");
        }

        Appointment entity = appointmentMapper.toEntity(dto);
        // Generate appointment number based on your logic (could be timestamp or
        // sequence)
        entity.setAppointmentNumber(generateAppointmentNumber());

        // Calculate end time based on session type duration
        LocalTime startTime = entity.getStartTime();
        int durationMinutes = dto.getSessionDurationMinutes() != null ? dto.getSessionDurationMinutes() : 30; // Use
                                                                                                              // provided
                                                                                                              // duration
                                                                                                              // or
                                                                                                              // default
                                                                                                              // to 30
                                                                                                              // minutes
        LocalTime endTime = startTime.plusMinutes(durationMinutes);
        entity.setEndTime(endTime);

        return appointmentMapper.toDto(appointmentRepository.save(entity));
    }

    @Override
    public AppointmentDto getById(UUID id) {
        return appointmentRepository.findById(id)
                .map(appointmentMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + id));
    }

    @Override
    public AppointmentDto getByNumber(String appointmentNumber) {
        return appointmentRepository.findByAppointmentNumber(appointmentNumber)
                .map(appointmentMapper::toDto)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Appointment not found with number " + appointmentNumber));
    }

    @Override
    public Page<AppointmentDto> search(UUID doctorId,
            UUID patientId,
            UUID practiceId,
            UUID sessionTypeId,
            LocalDate date,
            String status,
            String consultationType,
            Pageable pageable) {
        Specification<Appointment> spec = Specification
                .where(AppointmentSpecification.byDoctorId(doctorId))
                .and(AppointmentSpecification.byPatientId(patientId))
                .and(AppointmentSpecification.byPracticeId(practiceId))
                .and(AppointmentSpecification.bySessionTypeId(sessionTypeId))
                .and(AppointmentSpecification.byDate(date))
                .and(AppointmentSpecification.byStatus(status != null ? valueOf(status) : null))
                .and(AppointmentSpecification.byConsultationType(
                        consultationType != null ? ConsultationType.valueOf(consultationType) : null));

        return appointmentRepository.findAll(spec, pageable)
                .map(appointmentMapper::toDto);
    }

    @Override
    public AppointmentDto update(UUID id, AppointmentDto dto) {
        Appointment existing = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + id));

        // Example: Only notes or consultationType allowed to update before appointment
        existing.setNotes(dto.getNotes());
        existing.setConsultationType(dto.getConsultationType());

        return appointmentMapper.toDto(appointmentRepository.save(existing));
    }

    @Override
    public void delete(UUID id) {
        Appointment existing = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + id));

        appointmentRepository.delete(existing);
    }

    @Override
    public AppointmentDto changeStatus(UUID id, String newStatus, UUID changedById,
            String reason, String cancellationType, UUID rescheduledToAppointmentId) {
        Appointment existing = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + id));

        AppointmentStatus oldStatus = existing.getStatus();
        AppointmentStatus newStatusEnum = AppointmentStatus.valueOf(newStatus);

        existing.setStatus(newStatusEnum);

        // Handle check-in status
        if (newStatusEnum == AppointmentStatus.CHECKED_IN) {
            if (existing.getCheckedInAt() != null) {
                throw new ResourceConflictException("Patient has already checked in for this appointment");
            }
            existing.setCheckedInAt(ZonedDateTime.now());
        }

        // Save the appointment
        appointmentRepository.save(existing);

        // Create a history record
        AppointmentStatusHistory history = AppointmentStatusHistory.builder()
                .appointment(existing)
                .oldStatus(oldStatus)
                .newStatus(newStatusEnum)
                .changedById(changedById)
                .reason(reason)
                .rescheduledToAppointmentId(rescheduledToAppointmentId)
                .build();

        // Set cancellation type for all status changes
        if (cancellationType != null) {
            history.setCancellationType(CancellationType.valueOf(cancellationType));
        }

        historyRepository.save(history);

        return appointmentMapper.toDto(existing);
    }

    private String generateAppointmentNumber() {
        return "APT-" + System.currentTimeMillis();
    }

    @Override
    public boolean hasTimeSlotConflict(UUID doctorId, LocalDate date, String startTime, String endTime) {
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);

        // Find appointments for the doctor on the given date with SCHEDULED or
        // CONFIRMED status
        Specification<Appointment> spec = Specification
                .where(AppointmentSpecification.byDoctorId(doctorId))
                .and(AppointmentSpecification.byDate(date))
                .and(AppointmentSpecification.byStatus(AppointmentStatus.SCHEDULED)
                        .or(AppointmentSpecification.byStatus(AppointmentStatus.CHECKED_IN)));

        List<Appointment> existingAppointments = appointmentRepository.findAll(spec);

        // Check for time conflicts
        return existingAppointments.stream()
                .anyMatch(appointment -> {
                    LocalTime appointmentStart = appointment.getStartTime();
                    LocalTime appointmentEnd = appointment.getEndTime();

                    // Check if time slots overlap: (start1 < end2) && (start2 < end1)
                    return start.isBefore(appointmentEnd) && appointmentStart.isBefore(end);
                });
    }

    @Override
    public List<AppointmentDto> getExistingAppointments(UUID doctorId, LocalDate date, String status) {
        Specification<Appointment> spec = Specification
                .where(AppointmentSpecification.byDoctorId(doctorId))
                .and(AppointmentSpecification.byDate(date));

        // Parse status parameter (comma-separated list)
        if (status != null && !status.trim().isEmpty()) {
            String[] statusList = status.split(",");
            Specification<Appointment> statusSpec = null;

            for (String s : statusList) {
                AppointmentStatus appointmentStatus = AppointmentStatus.valueOf(s.trim());
                if (statusSpec == null) {
                    statusSpec = AppointmentSpecification.byStatus(appointmentStatus);
                } else {
                    statusSpec = statusSpec.or(AppointmentSpecification.byStatus(appointmentStatus));
                }
            }

            spec = spec.and(statusSpec);
        }

        return appointmentRepository.findAll(spec)
                .stream()
                .map(appointmentMapper::toDto)
                .toList();
    }
}
