-- Migration V3: Add check-in tracking to appointments
-- Adds CHECKED_IN status and checked_in_at column to track when patients check in

-- Step 1: Add CHECKED_IN to appointment_status enum
ALTER TYPE appointment_status ADD VALUE 'CHECKED_IN';

-- Step 2: Add checked_in_at column to appointments table
ALTER TABLE appointments
    ADD COLUMN checked_in_at TIMESTAMP WITH TIME ZONE;

-- Step 3: Set default value for status column (after enum is updated)
ALTER TABLE appointments 
    ALTER COLUMN status SET DEFAULT 'SCHEDULED'::appointment_status;

-- Step 4: Add index for better query performance on check-in lookups
CREATE INDEX idx_appointments_checked_in_at ON appointments(checked_in_at);

-- Step 5: Add comments for documentation
COMMENT ON COLUMN appointments.checked_in_at IS 'Timestamp when patient checked in for the appointment';
COMMENT ON INDEX idx_appointments_checked_in_at IS 'Index for efficient check-in time queries';
COMMENT ON TYPE appointment_status IS 'Appointment status: SCHEDULED, CHECKED_IN, COMPLETED, CANCELLED';
