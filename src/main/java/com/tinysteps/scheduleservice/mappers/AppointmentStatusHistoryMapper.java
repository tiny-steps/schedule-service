package com.tinysteps.scheduleservice.mappers;

import com.tinysteps.scheduleservice.entity.AppointmentStatusHistory;
import com.tinysteps.scheduleservice.model.AppointmentStatusHistoryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentStatusHistoryMapper {

    @Mapping(source = "appointment.id", target = "appointmentId")
    AppointmentStatusHistoryDto toDto(AppointmentStatusHistory entity);

    @Mapping(source = "appointmentId", target = "appointment.id")
    AppointmentStatusHistory toEntity(AppointmentStatusHistoryDto dto);
}
