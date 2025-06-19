package org.reminstant.dto.http.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

@Schema(
    description = "Часы, в которые бронь не доступна, на каждый день недели",
    accessMode = Schema.AccessMode.READ_WRITE)
public record CommonUnavailableDaysDto(
    List<@Min(0) @Max(23) Integer> mondayUnavailable,
    List<@Min(0) @Max(23) Integer> tuesdayUnavailable,
    List<@Min(0) @Max(23) Integer> wednesdayUnavailable,
    List<@Min(0) @Max(23) Integer> thursdayUnavailable,
    List<@Min(0) @Max(23) Integer> fridayUnavailable,
    List<@Min(0) @Max(23) Integer> saturdayUnavailable,
    List<@Min(0) @Max(23) Integer> sundayUnavailable) {
}