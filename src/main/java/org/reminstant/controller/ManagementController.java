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
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.reminstant.dto.http.common.CommonUnavailableDaysDto;
import org.reminstant.dto.http.common.RoomDto;
import org.reminstant.dto.http.response.ProblemDetailDto;
import org.reminstant.dto.http.response.RoomsListDto;
import org.reminstant.model.CommonUnavailableDays;
import org.reminstant.model.Room;
import org.reminstant.service.RoomService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@Validated
@RestController
@Tag(name = "Управление", description = "Управление комнатами (для администраторов)")
@SecurityRequirement(name = "BearerAuth")
public class ManagementController {

  private final RoomService roomService;

  public ManagementController(RoomService roomService) {
    this.roomService = roomService;
  }

  @GetMapping("${api.management.get-rooms}")
  @Operation(summary = "Получение списка помещений")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK", content = @Content(
          schema = @Schema(implementation = RoomsListDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "401", description = "Невалидный/истёкший токен доступа", content = @Content),
      @ApiResponse(responseCode = "403", description = "Нет доступа (отсутствует авторизация / нет прав)", content = @Content)
  })
  RoomsListDto getRooms() {
    List<String> roomList = roomService.getRoomTitles();
    return new RoomsListDto(roomList);
  }

  @GetMapping("${api.management.get-room}")
  @Operation(summary = "Получение данных помещения")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK", content = @Content(
          schema = @Schema(implementation = RoomDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "400", description = "Невалидные данные", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "401", description = "Невалидный/истёкший токен доступа", content = @Content),
      @ApiResponse(responseCode = "403", description = "Нет доступа (отсутствует авторизация / нет прав)", content = @Content),
      @ApiResponse(responseCode = "404", description = "Помещение не найдено", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  RoomDto getRoom(
      @RequestParam @Size(min = 1, max = 32)
      @Parameter(description = "Идентификатор помещения")
      String roomTitle) {
    Room room = roomService.getRoom(roomTitle);
    return roomService.convertRoomToDto(room);
  }

  @PostMapping("${api.management.add-room}")
  @Operation(summary = "Добавление помещения")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "OK", content = @Content),
      @ApiResponse(responseCode = "400", description = "Идентификатор занят / Невалидные данные", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "401", description = "Невалидный/истёкший токен доступа", content = @Content),
      @ApiResponse(responseCode = "403", description = "Нет доступа (отсутствует авторизация / нет прав)", content = @Content)
  })
  ResponseEntity<Object> addRoom(@Valid @RequestBody RoomDto dto) {
    Room room = roomService.getRoomFromDto(dto);
    roomService.addRoom(room);

    return ResponseEntity.noContent().build();
  }

  @PatchMapping("${api.management.configure-room}")
  @Operation(summary = "Изменение данных помещения")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "OK", content = @Content),
      @ApiResponse(responseCode = "400", description = "Невалидные данные", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "401", description = "Невалидный/истёкший токен доступа", content = @Content),
      @ApiResponse(responseCode = "403", description = "Нет доступа (отсутствует авторизация / нет прав)", content = @Content),
      @ApiResponse(responseCode = "404", description = "Помещение не найдено", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  ResponseEntity<Object> configureRoom(@Valid @RequestBody RoomDto dto) {
    Room room = roomService.getRoomFromDto(dto);
    roomService.configureRoom(room);

    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("${api.management.delete-room}")
  @Operation(summary = "Удаление помещения")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "OK", content = @Content),
      @ApiResponse(responseCode = "400", description = "Невалидные данные", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "401", description = "Невалидный/истёкший токен доступа", content = @Content),
      @ApiResponse(responseCode = "403", description = "Нет доступа (отсутствует авторизация / нет прав)", content = @Content),
      @ApiResponse(responseCode = "404", description = "Помещение не найдено", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  ResponseEntity<Object> deleteRoom(
      @RequestParam @Size(min = 1, max = 32)
      @Parameter(description = "Идентификатор помещения")
      String roomTitle) {
    roomService.deleteRoom(roomTitle);
    return ResponseEntity.noContent().build();
  }



  @GetMapping("${api.management.get-common-unavailable}")
  @Operation(summary = "Получение общего времени, не доступного для бронирования")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK", content = @Content(
          schema = @Schema(implementation = CommonUnavailableDaysDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "401", description = "Невалидный/истёкший токен доступа", content = @Content),
      @ApiResponse(responseCode = "403", description = "Нет доступа (отсутствует авторизация / нет прав)", content = @Content)
  })
  CommonUnavailableDaysDto getCommonUnavailableDays() {
    CommonUnavailableDays unavailable = roomService.getCommonUnavailableDays();
    return roomService.convertCommonUnavailableDaysToDto(unavailable);
  }
  
  @PutMapping("${api.management.update-common-unavailable}")
  @Operation(summary = "Изменение общего времени, не доступного для бронирования")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "OK", content = @Content),
      @ApiResponse(responseCode = "400", description = "Невалидные данные", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "401", description = "Невалидный/истёкший токен доступа", content = @Content),
      @ApiResponse(responseCode = "403", description = "Нет доступа (отсутствует авторизация / нет прав)", content = @Content)
  })
  ResponseEntity<Object> updateCommonUnavailableDays(@Valid @RequestBody CommonUnavailableDaysDto dto) {
    CommonUnavailableDays unavailable = roomService.getCommonUnavailableDaysFromDto(dto);
    roomService.setCommonUnavailableDays(unavailable);

    return ResponseEntity.noContent().build();
  }
}
