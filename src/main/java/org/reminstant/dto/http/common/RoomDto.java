package org.reminstant.dto.http.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.util.List;

public record RoomDto(
    @NotBlank @Length(min = 1, max = 32)
    String roomTitle,
    List<@Min(0) @Max(23) Integer> mondayUnavailable,
    List<@Min(0) @Max(23) Integer> tuesdayUnavailable,
    List<@Min(0) @Max(23) Integer> wednesdayUnavailable,
    List<@Min(0) @Max(23) Integer> thursdayUnavailable,
    List<@Min(0) @Max(23) Integer> fridayUnavailable,
    List<@Min(0) @Max(23) Integer> saturdayUnavailable,
    List<@Min(0) @Max(23) Integer> sundayUnavailable) {
}
