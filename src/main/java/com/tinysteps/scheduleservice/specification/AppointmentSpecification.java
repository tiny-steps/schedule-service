package com.tinysteps.scheduleservice.specification;

import com.tinysteps.scheduleservice.constants.AppointmentStatus;
import com.tinysteps.scheduleservice.constants.ConsultationType;
import com.tinysteps.scheduleservice.entity.Appointment;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public class AppointmentSpecification {

    public static Specification<Appointment> byDoctorId(UUID doctorId) {
        return (root, cq, cb) -> doctorId == null ? null : cb.equal(root.get("doctorId"), doctorId);
    }

    public static Specification<Appointment> byPatientId(UUID patientId) {
        return (root, cq, cb) -> patientId == null ? null : cb.equal(root.get("patientId"), patientId);
    }

    public static Specification<Appointment> byPracticeId(UUID practiceId) {
        return (root, cq, cb) -> practiceId == null ? null : cb.equal(root.get("practiceId"), practiceId);
    }

    public static Specification<Appointment> bySessionTypeId(UUID sessionTypeId) {
        return (root, cq, cb) -> sessionTypeId == null ? null : cb.equal(root.get("sessionTypeId"), sessionTypeId);
    }

    public static Specification<Appointment> byDate(LocalDate date) {
        return (root, cq, cb) -> date == null ? null : cb.equal(root.get("appointmentDate"), date);
    }

    public static Specification<Appointment> byStatus(AppointmentStatus status) {
        return (root, cq, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Appointment> byConsultationType(ConsultationType type) {
        return (root, cq, cb) -> type == null ? null : cb.equal(root.get("consultationType"), type);
    }

    public static Specification<Appointment> byBranchId(UUID branchId) {
        return (root, cq, cb) -> branchId == null ? null : cb.equal(root.get("branchId"), branchId);
    }
}