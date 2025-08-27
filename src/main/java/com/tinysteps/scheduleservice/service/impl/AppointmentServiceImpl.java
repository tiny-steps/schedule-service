package com.tinysteps.scheduleservice.service.impl;

import com.tinysteps.scheduleservice.constants.ConsultationType;
import com.tinysteps.scheduleservice.entity.Appointment;
import com.tinysteps.scheduleservice.constants.AppointmentStatus;
import com.tinysteps.scheduleservice.entity.AppointmentStatusHistory;
import com.tinysteps.scheduleservice.exception.BadRequestException;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tinysteps.scheduleservice.constants.AppointmentStatus.valueOf;

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
                .and(parseStatusSpecification(status))
                .and(AppointmentSpecification.byConsultationType(
                        consultationType != null ? ConsultationType.valueOf(consultationType) : null));

        return appointmentRepository.findAll(spec, pageable)
                .map(appointmentMapper::toDto);
    }

    private Specification<Appointment> parseStatusSpecification(String status) {
        if (status == null || status.trim().isEmpty()) {
            return AppointmentSpecification.byStatus(null);
        }

        try {
            // Handle comma-separated statuses
            if (status.contains(",")) {
                List<AppointmentStatus> statuses = Arrays.stream(status.split(","))
                        .map(String::trim)
                        .map(AppointmentStatus::valueOf)
                        .collect(Collectors.toList());
                return AppointmentSpecification.byStatuses(statuses);
            }

            // Handle single status
            return AppointmentSpecification.byStatus(AppointmentStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid appointment status: " + status + ". Valid statuses are: " +
                    Arrays.toString(AppointmentStatus.values()));
        }
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
    public AppointmentDto changeStatus(UUID id, String newStatus, UUID changedById, String changedByType,
            String reason) {
        Appointment existing = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + id));

        AppointmentStatus oldStatus = existing.getStatus();
        AppointmentStatus newStatusEnum = AppointmentStatus.valueOf(newStatus);

        existing.setStatus(newStatusEnum);

        // Save the appointment
        appointmentRepository.save(existing);

        // Create a history record
        AppointmentStatusHistory history = new AppointmentStatusHistory();
        history.setAppointment(existing);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatusEnum);
        history.setChangedById(changedById);
        history.setChangedByType(changedByType);
        history.setReason(reason);

        historyRepository.save(history);

        return appointmentMapper.toDto(existing);
    }

    private String generateAppointmentNumber() {
        return "APT-" + System.currentTimeMillis();
    }
}
