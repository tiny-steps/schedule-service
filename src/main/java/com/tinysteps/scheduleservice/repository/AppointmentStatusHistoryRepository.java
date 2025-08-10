package com.tinysteps.scheduleservice.repository;

import com.tinysteps.scheduleservice.entity.AppointmentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppointmentStatusHistoryRepository extends JpaRepository<AppointmentStatusHistory, UUID> {
    List<AppointmentStatusHistory> findByAppointment_IdOrderByChangedAtDesc(UUID appointmentId);
}
