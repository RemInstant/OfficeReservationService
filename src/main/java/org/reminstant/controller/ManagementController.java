package org.reminstant.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
import org.reminstant.dto.http.RoomConfigurationDto;
import org.reminstant.dto.http.RoomsListDto;
import org.reminstant.model.Room;
import org.reminstant.service.RoomService;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@Validated
@RestController
public class ManagementController {

  private final RoomService roomService;

  public ManagementController(RoomService roomService) {
    this.roomService = roomService;
  }

  @GetMapping("api/management/rooms")
  RoomsListDto getRooms() {
    List<String> roomList = roomService.getRooms();
    return new RoomsListDto(roomList);
  }

  @GetMapping("api/management/room")
  ResponseEntity<Object> getRoom(@RequestParam @Length(min = 1, max = 32) String roomTitle,
                                 HttpServletRequest request) {
    Optional<Room> room = roomService.getRoom(roomTitle);
    if (room.isEmpty()) {
      String detail = "No room with title '%s'".formatted(roomTitle);
      return buildErrorResponse(HttpStatus.NOT_FOUND, request.getRequestURI(), detail);
    }

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(room.get().toDto());
  }

  @PostMapping("api/management/room")
  ResponseEntity<Object> addRoom(@Valid @RequestBody RoomConfigurationDto dto) {
    roomService.addRoom(Room.fromDto(dto));
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("api/management/room")
  ResponseEntity<Object> configureRoom(@Valid @RequestBody RoomConfigurationDto dto,
                                       HttpServletRequest request) {
    boolean updated = roomService.configureRoom(Room.fromDto(dto));
    if (!updated) {
      String detail = "No room with title '%s'".formatted(dto.roomTitle());
      return buildErrorResponse(HttpStatus.NOT_FOUND, request.getRequestURI(), detail);
    }

    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("api/management/room")
  ResponseEntity<Object> deleteRoom(@RequestParam @Length(min = 1, max = 32) String roomTitle,
                                    HttpServletRequest request) {
    boolean deleted = roomService.deleteRoom(roomTitle);
    if (!deleted) {
      String detail = "No room with title '%s'".formatted(roomTitle);
      return buildErrorResponse(HttpStatus.NOT_FOUND, request.getRequestURI(), detail);
    }

    return ResponseEntity.noContent().build();
  }



  private ResponseEntity<Object> buildErrorResponse(HttpStatus status, String path, String detail) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", new Date());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("detail", detail);
    body.put("path", path);

    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }
}
