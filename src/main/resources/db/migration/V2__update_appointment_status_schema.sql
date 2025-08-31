-- Migration V2: Update appointment status schema
-- Simplifies appointment statuses and adds cancellation type tracking

-- Step 1: Create new cancellation_type enum
CREATE TYPE cancellation_type AS ENUM ('NO_SHOW', 'CANCELLED_BY_DOCTOR', 'CANCELLED_BY_PATIENT', 'RESCHEDULED');

-- Step 2: Update appointment_status enum
-- First rename the old enum
ALTER TYPE appointment_status RENAME TO appointment_status_old;

-- Create new simplified appointment_status enum
CREATE TYPE appointment_status AS ENUM ('SCHEDULED', 'COMPLETED', 'CANCELLED');

-- Step 3: Update appointments table
-- Convert existing statuses to new format
UPDATE appointments 
SET status = CASE 
    WHEN status::text = 'PENDING_PAYMENT' THEN 'SCHEDULED'::appointment_status
    WHEN status::text = 'CONFIRMED' THEN 'SCHEDULED'::appointment_status
    WHEN status::text = 'NO_SHOW' THEN 'CANCELLED'::appointment_status
    ELSE status::text::appointment_status
END;

-- Alter the status column to use new enum
ALTER TABLE appointments 
    ALTER COLUMN status TYPE appointment_status 
    USING status::text::appointment_status;

-- Step 4: Update appointment_status_history table
-- Add new columns
ALTER TABLE appointment_status_history 
    ADD COLUMN cancellation_type cancellation_type,
    ADD COLUMN rescheduled_to_appointment_id UUID;

-- Remove the old changed_by_type column (no longer needed)
ALTER TABLE appointment_status_history 
    DROP COLUMN IF EXISTS changed_by_type;

-- Convert existing status values in history table
UPDATE appointment_status_history 
SET old_status = CASE 
    WHEN old_status::text = 'PENDING_PAYMENT' THEN 'SCHEDULED'::appointment_status
    WHEN old_status::text = 'CONFIRMED' THEN 'SCHEDULED'::appointment_status
    WHEN old_status::text = 'NO_SHOW' THEN 'CANCELLED'::appointment_status
    ELSE old_status::text::appointment_status
END
WHERE old_status IS NOT NULL;

UPDATE appointment_status_history 
SET new_status = CASE 
    WHEN new_status::text = 'PENDING_PAYMENT' THEN 'SCHEDULED'::appointment_status
    WHEN new_status::text = 'CONFIRMED' THEN 'SCHEDULED'::appointment_status
    WHEN new_status::text = 'NO_SHOW' THEN 'CANCELLED'::appointment_status
    ELSE new_status::text::appointment_status
END;

-- Set cancellation_type for existing CANCELLED records
UPDATE appointment_status_history 
SET cancellation_type = 'NO_SHOW'::cancellation_type
WHERE new_status::text = 'CANCELLED' 
    AND old_status::text = 'NO_SHOW';

-- For other cancelled records, set a default
UPDATE appointment_status_history 
SET cancellation_type = 'CANCELLED_BY_PATIENT'::cancellation_type
WHERE new_status::text = 'CANCELLED' 
    AND cancellation_type IS NULL;

-- Alter the status columns in history table to use new enum
ALTER TABLE appointment_status_history 
    ALTER COLUMN old_status TYPE appointment_status 
    USING old_status::text::appointment_status;

ALTER TABLE appointment_status_history 
    ALTER COLUMN new_status TYPE appointment_status 
    USING new_status::text::appointment_status;

-- Step 5: Drop the old enum type
DROP TYPE appointment_status_old;

-- Step 6: Add comments for documentation
COMMENT ON TYPE appointment_status IS 'Simplified appointment status: SCHEDULED, COMPLETED, CANCELLED';
COMMENT ON TYPE cancellation_type IS 'Cancellation reasons: NO_SHOW, CANCELLED_BY_DOCTOR, CANCELLED_BY_PATIENT, RESCHEDULED';
COMMENT ON COLUMN appointment_status_history.cancellation_type IS 'Only populated when status changes to CANCELLED';
COMMENT ON COLUMN appointment_status_history.rescheduled_to_appointment_id IS 'Links to new appointment if this was rescheduled';
