package org.reminstant.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.reminstant.dto.http.response.*;
import org.reminstant.dto.http.common.ReservationRequestDto;
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

@Slf4j
@Validated
@RestController
public class ReservationController {

  private final RoomService roomService;

  public ReservationController(RoomService roomService) {
    this.roomService = roomService;
  }


  @GetMapping("api/service/reservation/available-by-room")
  ResponseEntity<Object> getAvailByRoom(@RequestParam String roomTitle,
                                        @RequestParam(required = false) String startDate,
                                        @RequestParam(defaultValue = "7") @Min(1) @Max(30) int dayCount) {
    RoomDayRangeAvailability avail = roomService.getRoomAvailabilityPerDay(roomTitle, startDate, dayCount);
    RoomDayRangeAvailabilityDto dto = roomService.convertAvailabilityToDto(avail);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(dto);
  }

  @GetMapping("api/service/reservation/available-by-date")
  ResponseEntity<Object> getAvailByDate(@RequestParam(name = "date") String stringDate) {
    RoomsDayAvailability avail = roomService.getRoomsAvailabilityByDay(stringDate);
    RoomsDayAvailabilityDto dto = roomService.convertAvailabilityToDto(avail);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(dto);
  }

  @GetMapping("api/service/reservations")
  ResponseEntity<Object> getReservations(Principal principal) {
    String username = principal == null ? null : principal.getName();

    List<String> reservationIds = roomService.getActualReservationIds(username);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ReservationsListDto(reservationIds));
  }

  @GetMapping("api/service/reservation")
  ResponseEntity<Object> getReservations(@RequestParam String reservationId,
                                      Principal principal) {
    String username = principal == null ? null : principal.getName();

    Reservation reservation = roomService.getReservationById(username, reservationId);
    ReservationParamsDto dto = roomService.convertReservationToDto(reservation);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(dto);
  }

  @PostMapping("api/service/reservation")
  ResponseEntity<Object> reserveRoom(@Valid @RequestBody ReservationRequestDto dto,
                                     Principal principal) {
    String username = principal == null ? null : principal.getName();
    String id = roomService.reserveRoom(username, dto.roomTitle(), dto.date(), dto.startHour(), dto.endHour());

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ReservationIdDto(id));
  }

}
