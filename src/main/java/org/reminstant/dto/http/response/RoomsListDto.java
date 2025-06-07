package org.reminstant.dto.http.response;

import java.util.List;

public record RoomsListDto(
    List<String> rooms) {
}