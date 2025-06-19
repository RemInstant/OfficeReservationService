package org.reminstant.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.reminstant.dto.http.response.*;
import org.reminstant.dto.http.request.ReservationRequestDto;
import org.reminstant.model.Reservation;
import org.reminstant.model.RoomDayRangeAvailability;
import org.reminstant.model.RoomsDayAvailability;
import org.reminstant.service.RoomService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

@Slf4j
@Validated
@RestController
@Tag(name = "Бронирование", description = "Управление бронями пользователей")
@SecurityRequirement(name = "BearerAuth")
public class ReservationServiceController {

  private final RoomService roomService;

  public ReservationServiceController(RoomService roomService) {
    this.roomService = roomService;
  }

  @GetMapping("${api.service.get-rooms}")
  @Operation(summary = "Получение списка помещений")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK", content = @Content(
          schema = @Schema(implementation = RoomsListDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "401", description = "Невалидный/истёкший токен доступа", content = @Content),
      @ApiResponse(responseCode = "403", description = "Нет доступа (отсутствует авторизация)", content = @Content)
  })
  RoomsListDto getRooms() {
    List<String> roomList = roomService.getRoomTitles();
    return new RoomsListDto(roomList);
  }

  @GetMapping("${api.service.get-available-reservations-by-room}")
  @Operation(summary = "Получение времени, доступного для бронирования выбранного помещения, в диапазоне дат")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK", content = @Content(
          schema = @Schema(implementation = RoomDayRangeAvailabilityDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "400", description = "Помещение не найдено / Невалидные данные", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "401", description = "Невалидный/истёкший токен доступа", content = @Content),
      @ApiResponse(responseCode = "403", description = "Нет доступа (отсутствует авторизация)", content = @Content)
  })
  RoomDayRangeAvailabilityDto getAvailByRoom(
      @RequestParam
      @Parameter(description = "Идентификатор помещения")
      String roomTitle,
      @RequestParam(required = false)
      @Parameter(description = "Начало диапазона поиска, null == сегодняшний день (ISO 8601)", example = "2025-12-25")
      String startDate,
      @RequestParam(defaultValue = "7") @Min(1) @Max(30)
      @Parameter(description = "Ширина диапазона в днях")
      int dayCount
  ) {
    RoomDayRangeAvailability avail = roomService.getRoomAvailabilityPerDay(roomTitle, startDate, dayCount);
    return roomService.convertAvailabilityToDto(avail);
  }

  @GetMapping("${api.service.get-available-reservations-by-date}")
  @Operation(summary = "Получение времени, доступного для бронирования помещений, в выбранную дату")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK", content = @Content(
          schema = @Schema(implementation = RoomsDayAvailabilityDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "400", description = "Невалидные данные", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "401", description = "Невалидный/истёкший токен доступа", content = @Content),
      @ApiResponse(responseCode = "403", description = "Нет доступа (отсутствует авторизация)", content = @Content)
  })
  RoomsDayAvailabilityDto getAvailByDate(
      @RequestParam
      @Parameter(description = "Дата бронирования (ISO 8601)", example = "2025-12-25")
      String date) {
    RoomsDayAvailability avail = roomService.getRoomsAvailabilityByDay(date);
    return roomService.convertAvailabilityToDto(avail);
  }

  @GetMapping("${api.service.get-your-reservations}")
  @Operation(summary = "Получение своих бронирований")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK", content = @Content(
          schema = @Schema(implementation = ReservationsListDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "401", description = "Невалидный/истёкший токен доступа", content = @Content),
      @ApiResponse(responseCode = "403", description = "Нет доступа (отсутствует авторизация)", content = @Content)
  })
  ReservationsListDto getReservations(Principal principal) {
    Objects.requireNonNull(principal, "Principal must be non-null");
    String username = principal.getName();

    List<String> reservationIds = roomService.getActualReservationIds(username);
    return new ReservationsListDto(reservationIds);
  }

  @GetMapping("${api.service.get-your-reservation-info}")
  @Operation(summary = "Получение информации о своём бронировании")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK", content = @Content(
          schema = @Schema(implementation = ReservationParamsDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "401", description = "Невалидный/истёкший токен доступа", content = @Content),
      @ApiResponse(responseCode = "403", description = "Нет доступа (отсутствует авторизация)", content = @Content),
      @ApiResponse(responseCode = "404", description = "Бронь не найдена", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  ReservationParamsDto getReservation(
      @RequestParam
      @Parameter(description = "Идентификатор брони")
      String reservationId,
      Principal principal) {
    String username = principal == null ? null : principal.getName();

    Reservation reservation = roomService.getReservationById(username, reservationId);
    return roomService.convertReservationToDto(reservation);
  }

  @PostMapping("${api.service.reserve-room}")
  @Operation(summary = "Бронирование помещения")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK", content = @Content(
          schema = @Schema(implementation = RoomsDayAvailabilityDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "400", description = "Выбранное время уже забронировано / Невалидные данные", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "401", description = "Невалидный/истёкший токен доступа", content = @Content),
      @ApiResponse(responseCode = "403", description = "Нет доступа (отсутствует авторизация)", content = @Content),
      @ApiResponse(responseCode = "404", description = "Помещение не найдено", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  ReservationIdDto reserveRoom(@Valid @RequestBody ReservationRequestDto dto,
                               Principal principal) {
    Objects.requireNonNull(principal, "Principal must be non-null");
    String username = principal.getName();
    String id = roomService.reserveRoom(username, dto.roomTitle(), dto.date(), dto.startHour(), dto.endHour());

    return new ReservationIdDto(id);
  }

  @DeleteMapping("${api.service.cancel-reservation}")
  @Operation(summary = "Отмена брони помещения")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "OK", content = @Content),
      @ApiResponse(responseCode = "401", description = "Невалидный/истёкший токен доступа", content = @Content),
      @ApiResponse(responseCode = "403", description = "Нет доступа (отсутствует авторизация)", content = @Content),
      @ApiResponse(responseCode = "404", description = "Бронь не найдена", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  ResponseEntity<Object> reserveRoom(
      @RequestParam @Size(min = 24, max = 24)
      @Parameter(description = "Идентификатор брони")
      String reservationId,
      Principal principal) {
    Objects.requireNonNull(principal, "Principal must be non-null");
    String username = principal.getName();
    roomService.cancelReservation(username, reservationId);

    return ResponseEntity.noContent().build();
  }
}
