package com.tinysteps.scheduleservice.repository;

import com.tinysteps.scheduleservice.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID>, JpaSpecificationExecutor<Appointment> {

    Optional<Appointment> findByAppointmentNumber(String appointmentNumber);

    boolean existsByDoctorIdAndPracticeIdAndAppointmentDateAndStartTime(UUID doctorId, UUID practiceId, LocalDate date, LocalTime startTime);
}
