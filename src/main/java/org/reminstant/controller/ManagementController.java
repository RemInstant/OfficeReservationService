package org.reminstant.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
import org.reminstant.dto.http.common.RoomDto;
import org.reminstant.dto.http.response.RoomsListDto;
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
  ResponseEntity<Object> getRoom(@RequestParam @Length(min = 1, max = 32) String roomTitle) {
    Room room = roomService.getRoom(roomTitle);
    RoomDto dto = roomService.convertRoomToDto(room);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(dto);
  }

  @PostMapping("api/management/room")
  ResponseEntity<Object> addRoom(@Valid @RequestBody RoomDto dto) {
    Room room = roomService.getRoomFromDto(dto);
    roomService.addRoom(room);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("api/management/room")
  ResponseEntity<Object> configureRoom(@Valid @RequestBody RoomDto dto) {
    Room room = roomService.getRoomFromDto(dto);
    roomService.configureRoom(room);

    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("api/management/room")
  ResponseEntity<Object> deleteRoom(@RequestParam @Length(min = 1, max = 32) String roomTitle) {
    roomService.deleteRoom(roomTitle);

    return ResponseEntity.noContent().build();
  }
}
