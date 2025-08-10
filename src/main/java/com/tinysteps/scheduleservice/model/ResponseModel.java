package com.tinysteps.scheduleservice.model;

import com.tinysteps.scheduleservice.model.ErrorModel;

import java.time.ZonedDateTime;
import java.util.List;

public record ResponseModel<T>(
		int statusCode,
		String status,
		String message,
		ZonedDateTime timestamp,
		T data,
		List<ErrorModel> errors
) {}
