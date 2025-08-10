package com.tinysteps.scheduleservice.config;

import com.tinysteps.scheduleservice.entity.Appointment;
import com.tinysteps.scheduleservice.repository.AppointmentRepository;
import com.tinysteps.scheduleservice.integration.service.DoctorIntegrationService;
import com.tinysteps.scheduleservice.integration.service.UserIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component("scheduleSecurity")
@RequiredArgsConstructor
@Slf4j
public class ApplicationSecurityConfig {

    private final AppointmentRepository appointmentRepository;
    private final DoctorIntegrationService doctorIntegrationService;
    private final UserIntegrationService userIntegrationService;

    // ----- Role Checks -----
    public boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, "ADMIN");
    }

    public boolean isDoctor(Authentication authentication) {
        return hasRole(authentication, "DOCTOR");
    }

    public boolean isPatient(Authentication authentication) {
        return hasRole(authentication, "PATIENT");
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth != null && auth.getAuthorities() != null &&
                auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(a -> a.equals("ROLE_" + role) || a.equals(role));
    }

    private String getAuthenticatedUserId(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }

    // ----- Ownership / Participant Checks -----

    /**
     * Check if the authenticated doctor owns the appointment (via doctorId).
     */
    public boolean isAppointmentOwner(Authentication authentication, UUID appointmentId) {
        if (isAdmin(authentication)) return true;

        Optional<Appointment> opt = appointmentRepository.findById(appointmentId);
        return opt.filter(appt ->
                isDoctorOwner(authentication, appt.getDoctorId())
        ).isPresent();
    }

    /**
     * Check if the appointment is linked to the current doctor or patient.
     */
    public boolean isAppointmentParticipant(Authentication authentication, UUID appointmentId) {
        if (isAdmin(authentication)) return true;

        Optional<Appointment> opt = appointmentRepository.findById(appointmentId);
        if (opt.isEmpty()) return false;

        Appointment appt = opt.get();
        if (isDoctor(authentication) && isDoctorOwner(authentication, appt.getDoctorId())) {
            return true;
        }
        if (isPatient(authentication) && isPatientOwner(authentication, appt.getPatientId())) {
            return true;
        }
        return false;
    }

    /**
     * Overload for appointmentNumber (string) for lookups by public ref.
     */
    public boolean isAppointmentParticipant(Authentication authentication, String appointmentNumber) {
        if (isAdmin(authentication)) return true;

        Optional<Appointment> opt = appointmentRepository.findByAppointmentNumber(appointmentNumber);
        if (opt.isEmpty()) return false;

        Appointment appt = opt.get();
        if (isDoctor(authentication) && isDoctorOwner(authentication, appt.getDoctorId())) {
            return true;
        }
        if (isPatient(authentication) && isPatientOwner(authentication, appt.getPatientId())) {
            return true;
        }
        return false;
    }

    /**
     * Check doctor ownership via Doctor Service integration.
     */
    public boolean isDoctorOwner(Authentication authentication, UUID doctorId) {
        if (isAdmin(authentication)) return true;
        if (!isDoctor(authentication)) return false;
        try {
            return doctorIntegrationService.validateDoctorOwnership(doctorId, getAuthenticatedUserId(authentication));
        } catch (Exception e) {
            log.warn("Doctor ownership check failed for {}", doctorId, e);
            return false;
        }
    }

    /**
     * Check patient ownership via User Service integration.
     */
    public boolean isPatientOwner(Authentication authentication, UUID patientId) {
        if (isAdmin(authentication)) return true;
        if (!isPatient(authentication)) return false;
        try {
            return userIntegrationService.validateUserOwnership(patientId, getAuthenticatedUserId(authentication));
        } catch (Exception e) {
            log.warn("Patient ownership check failed for {}", patientId, e);
            return false;
        }
    }
}
