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

    public static Specification<Appointment> byDateOrDateRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return (root, cq, cb) -> {
            if (date != null) {
                // Single date filter
                return cb.equal(root.get("appointmentDate"), date);
            } else if (startDate != null && endDate != null) {
                // Date range filter
                return cb.between(root.get("appointmentDate"), startDate, endDate);
            } else if (startDate != null) {
                // Start date only
                return cb.greaterThanOrEqualTo(root.get("appointmentDate"), startDate);
            } else if (endDate != null) {
                // End date only
                return cb.lessThanOrEqualTo(root.get("appointmentDate"), endDate);
            }
            return null; // No date filtering
        };
    }

    public static Specification<Appointment> byStatus(AppointmentStatus status) {
        return (root, cq, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Appointment> byConsultationType(ConsultationType type) {
        return (root, cq, cb) -> type == null ? null : cb.equal(root.get("consultationType"), type);
    }

    public static Specification<Appointment> byBranchId(UUID branchId) {
        return (root, cq, cb) -> branchId == null ? null
                : cb.or(
                        cb.equal(root.get("branchId"), branchId),
                        cb.isNull(root.get("branchId")));
    }

    public static Specification<Appointment> byStatuses(java.util.List<AppointmentStatus> statuses) {
        return (root, cq, cb) -> {
            if (statuses == null || statuses.isEmpty()) return null;
            if (statuses.size() == 1) return cb.equal(root.get("status"), statuses.get(0));
            return root.get("status").in(statuses);
        };
    }
}
