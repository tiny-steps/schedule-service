CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


-- ENUM types
CREATE TYPE appointment_status AS ENUM ('SCHEDULED', 'CHECKED_IN', 'COMPLETED', 'CANCELLED');
CREATE TYPE consultation_type AS ENUM ('IN_PERSON', 'TELEMEDICINE');

CREATE TABLE appointments (
                              id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                              appointment_number VARCHAR(20) UNIQUE NOT NULL,
                              doctor_id UUID NOT NULL,
                              patient_id UUID NOT NULL,
                              session_type_id UUID NOT NULL,
                              practice_id UUID,
                              appointment_date DATE NOT NULL,
                              start_time TIME NOT NULL,
                              end_time TIME NOT NULL,
                              status appointment_status NOT NULL DEFAULT 'SCHEDULED',
                              consultation_type consultation_type NOT NULL DEFAULT 'IN_PERSON',
                              checked_in_at TIMESTAMP WITH TIME ZONE,
                              notes TEXT,
                              cancellation_reason TEXT,
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              UNIQUE (doctor_id, practice_id, appointment_date, start_time)
);

CREATE TABLE appointment_status_history (
                                            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                            appointment_id UUID NOT NULL REFERENCES appointments(id),
                                            old_status appointment_status,
                                            new_status appointment_status NOT NULL,
                                            changed_by_id UUID,
                                            changed_by_type VARCHAR(20),
                                            reason TEXT,
                                            changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_appointment_status_history_appointment_id ON appointment_status_history(appointment_id);
CREATE INDEX idx_appointments_checked_in_at ON appointments(checked_in_at);

COMMENT ON COLUMN appointments.checked_in_at IS 'Timestamp when patient checked in for the appointment';
COMMENT ON INDEX idx_appointments_checked_in_at IS 'Index for efficient check-in time queries';
COMMENT ON TYPE appointment_status IS 'Appointment status: SCHEDULED, CHECKED_IN, COMPLETED, CANCELLED';
