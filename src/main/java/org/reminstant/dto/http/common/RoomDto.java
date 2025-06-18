package org.reminstant.dto.http.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(
    description = "Данные помещения: идентификатор и часы, в которые бронь не доступна, на каждый день недели",
    accessMode = Schema.AccessMode.READ_WRITE)
public record RoomDto(
    @NotBlank @Size(min = 1, max = 32)
    @Schema(description = "Идентификатор помещения", example = "A-423")
    String roomTitle,
    List<@Min(0) @Max(23) Integer> mondayUnavailable,
    List<@Min(0) @Max(23) Integer> tuesdayUnavailable,
    List<@Min(0) @Max(23) Integer> wednesdayUnavailable,
    List<@Min(0) @Max(23) Integer> thursdayUnavailable,
    List<@Min(0) @Max(23) Integer> fridayUnavailable,
    List<@Min(0) @Max(23) Integer> saturdayUnavailable,
    List<@Min(0) @Max(23) Integer> sundayUnavailable) {
}
