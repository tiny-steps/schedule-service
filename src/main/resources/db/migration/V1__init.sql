CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


-- ENUM types
CREATE TYPE appointment_status AS ENUM ('PENDING_PAYMENT', 'SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW');
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
                              status appointment_status NOT NULL DEFAULT 'PENDING_PAYMENT',
                              consultation_type consultation_type NOT NULL DEFAULT 'IN_PERSON',
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
