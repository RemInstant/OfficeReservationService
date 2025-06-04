package org.reminstant.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.reminstant.dto.http.ReservationIdDto;
import org.reminstant.dto.http.ReservationRequestDto;
import org.reminstant.service.RoomService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Slf4j
@Validated
@RestController
public class ReservationController {

  private final RoomService roomService;

  public ReservationController(RoomService roomService) {
    this.roomService = roomService;
  }

  @PostMapping("api/service/reservation")
  ResponseEntity<Object> reserveRoom(@Valid @RequestBody ReservationRequestDto dto,
                                     Principal principal) {
    log.info("{}", (principal == null ? null : principal.getName()));
    String id = roomService.reserveRoom(dto.roomTitle(), dto.date(), dto.startHour(), dto.endHour());

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ReservationIdDto(id));
  }

}
