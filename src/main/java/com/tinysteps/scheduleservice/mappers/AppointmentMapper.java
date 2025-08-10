package com.tinysteps.scheduleservice.mappers;

import com.tinysteps.scheduleservice.entity.Appointment;
import com.tinysteps.scheduleservice.model.AppointmentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentMapper {

    AppointmentDto toDto(Appointment entity);

    Appointment toEntity(AppointmentDto dto);
}
