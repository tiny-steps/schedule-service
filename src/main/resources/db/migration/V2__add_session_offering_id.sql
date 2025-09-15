-- Add session_offering_id column to appointments table
-- This will store the specific SessionOffering ID that the appointment is booked for

ALTER TABLE appointments 
ADD COLUMN session_offering_id UUID;

-- Add index for better query performance
CREATE INDEX idx_appointments_session_offering_id ON appointments(session_offering_id);

-- Add comment to document the purpose
COMMENT ON COLUMN appointments.session_offering_id IS 'References the specific session offering (SessionOffering entity) that this appointment is booked for';