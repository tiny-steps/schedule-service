# Appointment Status Schema Migration Guide

## Overview

This migration updates the appointment status system to use a simplified approach with better cancellation tracking. The changes include:

1. **Simplified Appointment Statuses**: Reduced from 6 statuses to 3 core statuses
2. **Enhanced Cancellation Tracking**: Added detailed cancellation type tracking
3. **Rescheduling Support**: Added ability to link rescheduled appointments

## Migration Files

### V2\_\_update_appointment_status_schema.sql

Main migration that updates the schema to the new design.

### V2_1\_\_rollback_appointment_status_schema.sql

Rollback migration to revert changes if needed.

## Status Mapping

### Before Migration

- `PENDING_PAYMENT` → `SCHEDULED`
- `CONFIRMED` → `SCHEDULED`
- `NO_SHOW` → `CANCELLED`
- `SCHEDULED` → `SCHEDULED`
- `COMPLETED` → `COMPLETED`
- `CANCELLED` → `CANCELLED`

### After Migration

- `SCHEDULED` - Appointment is confirmed and scheduled
- `COMPLETED` - Appointment has been completed
- `CANCELLED` - Appointment has been cancelled (with cancellation type)

## New Cancellation Types

- `NO_SHOW` - Patient didn't show up
- `CANCELLED_BY_DOCTOR` - Doctor cancelled the appointment
- `CANCELLED_BY_PATIENT` - Patient cancelled the appointment
- `RESCHEDULED` - Appointment was rescheduled to a different time

## Database Changes

### New Columns Added

- `appointment_status_history.cancellation_type` - Type of cancellation
- `appointment_status_history.rescheduled_to_appointment_id` - Link to new appointment if rescheduled

### Enum Changes

- `appointment_status` enum updated to: `SCHEDULED`, `COMPLETED`, `CANCELLED`
- New `cancellation_type` enum: `NO_SHOW`, `CANCELLED_BY_DOCTOR`, `CANCELLED_BY_PATIENT`, `RESCHEDULED`

## API Changes

### Updated Endpoint

```
POST /api/v1/appointments/{id}/status
```

### New Parameters

- `cancellationType` (optional) - Required when status is CANCELLED
- `rescheduledToAppointmentId` (optional) - Required when cancellationType is RESCHEDULED

### Example Usage

#### Doctor Cancels Appointment

```bash
POST /api/v1/appointments/{id}/status
{
  "status": "CANCELLED",
  "changedById": "doctor-uuid",
  "changedByType": "DOCTOR",
  "reason": "Emergency surgery scheduled",
  "cancellationType": "CANCELLED_BY_DOCTOR"
}
```

#### Patient Reschedules

```bash
POST /api/v1/appointments/{id}/status
{
  "status": "CANCELLED",
  "changedById": "patient-uuid",
  "changedByType": "PATIENT",
  "reason": "Need different time",
  "cancellationType": "RESCHEDULED",
  "rescheduledToAppointmentId": "new-appointment-uuid"
}
```

#### No Show

```bash
POST /api/v1/appointments/{id}/status
{
  "status": "CANCELLED",
  "changedById": "system-uuid",
  "changedByType": "SYSTEM",
  "reason": "Patient did not show up",
  "cancellationType": "NO_SHOW"
}
```

## Migration Steps

1. **Backup Database**: Always backup before running migrations
2. **Run Migration**: Execute `V2__update_appointment_status_schema.sql`
3. **Verify Data**: Check that existing appointments have correct statuses
4. **Update Application**: Deploy updated application code
5. **Test**: Verify all appointment operations work correctly

## Rollback

If issues occur, use the rollback migration:

```sql
-- Execute V2_1__rollback_appointment_status_schema.sql
```

## Data Validation Queries

### Check Status Distribution

```sql
SELECT status, COUNT(*)
FROM appointments
GROUP BY status;
```

### Check Cancellation Types

```sql
SELECT cancellation_type, COUNT(*)
FROM appointment_status_history
WHERE new_status = 'CANCELLED'
GROUP BY cancellation_type;
```

### Verify Rescheduled Links

```sql
SELECT
    ash.appointment_id,
    ash.rescheduled_to_appointment_id,
    a.appointment_number
FROM appointment_status_history ash
JOIN appointments a ON ash.appointment_id = a.id
WHERE ash.cancellation_type = 'RESCHEDULED';
```

## Benefits

1. **Simplified Status Management**: Only 3 core statuses to manage
2. **Better Analytics**: Detailed cancellation tracking for business intelligence
3. **Rescheduling Support**: Proper linking between old and new appointments
4. **Audit Trail**: Complete history of who cancelled what and why
5. **Payment Service Separation**: No payment-related statuses in schedule service
