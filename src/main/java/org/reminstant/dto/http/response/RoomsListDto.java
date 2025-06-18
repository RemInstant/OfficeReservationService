package org.reminstant.dto.http.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Список комнат", accessMode = Schema.AccessMode.READ_ONLY)
public record RoomsListDto(
    List<String> rooms) {
}