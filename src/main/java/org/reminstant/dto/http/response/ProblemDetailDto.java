package org.reminstant.dto.http.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Описание ошибки в формате Problem Details RFC 7807",
    accessMode = Schema.AccessMode.READ_ONLY)
public record ProblemDetailDto(
    @Schema(description = "URI-ссылка на документацию о типе ошибки", example = "about:blank")
    String type,
    @Schema(description = "Краткое описание ошибки", example = "Bad Request")
    String title,
    @Schema(description = "HTTP статус код ошибки", example = "400")
    int status,
    @Schema(description = "Описание ошибки", example = "Some error description")
    String detail,
    @Schema(description = "URI-ссылка на ресурс, где произошла ошибка", example = "/api/service/reservation")
    String instance) {
}
