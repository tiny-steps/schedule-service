package com.tinysteps.scheduleservice.repository;

import com.tinysteps.scheduleservice.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID>, JpaSpecificationExecutor<Appointment> {

    Optional<Appointment> findByAppointmentNumber(String appointmentNumber);

    boolean existsByDoctorIdAndPracticeIdAndAppointmentDateAndStartTime(UUID doctorId, UUID practiceId, LocalDate date, LocalTime startTime);
    
    /**
     * Find appointments by branch ID and date range for transfer operations
     */
    List<Appointment> findByBranchIdAndCreatedAtBetween(UUID branchId, LocalDateTime startDateTime, LocalDateTime endDateTime);
    
    /**
     * Find appointments by branch ID and appointment date range
     */
    List<Appointment> findByBranchIdAndAppointmentDateBetween(UUID branchId, LocalDate startDate, LocalDate endDate);
}
