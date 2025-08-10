package com.tinysteps.scheduleservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "integration")
@Data
public class IntegrationProperties {

    private DoctorService doctorService = new DoctorService();
    private AddressService addressService = new AddressService();
    private UserService userService = new UserService();
    private TimingService timingService = new TimingService();
    private AppointmentService appointmentService = new AppointmentService();

    @Data
    public static class DoctorService {
        private String baseUrl = "http://ts-doctor-service/api/v1/doctors";
        private int timeoutSeconds = 10;
        private int maxRetries = 3;
        private boolean enableCircuitBreaker = true;
    }

    @Data
    public static class AddressService {
        private String baseUrl = "http://ts-address-service/api/v1/addresses";
        private int timeoutSeconds = 10;
        private int maxRetries = 3;
        private boolean enableCircuitBreaker = true;
    }

    @Data
    public static class UserService {
        private String baseUrl = "http://ts-user-service/api/v1/users";
        private int timeoutSeconds = 10;
        private int maxRetries = 3;
        private boolean enableCircuitBreaker = true;
    }

    @Data
    public static class TimingService {
        private String baseUrl = "http://ts-timing-service/api/v1/timings";
        private int timeoutSeconds = 10;
        private int maxRetries = 3;
        private boolean enableCircuitBreaker = true;
    }

    @Data
    public static class AppointmentService {
        private String baseUrl = "http://ts-appointment-service/api/v1/appointments";
        private int timeoutSeconds = 10;
        private int maxRetries = 3;
        private boolean enableCircuitBreaker = true;
    }
}
