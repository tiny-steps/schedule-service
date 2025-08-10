package com.tinysteps.scheduleservice.model;

import java.time.ZonedDateTime;
import java.util.List;

public record ErrorModel(
        String message,
        String details,
        ZonedDateTime timestamp,
        List<String> validationErrors // nullable, for input validation issues
) {}
