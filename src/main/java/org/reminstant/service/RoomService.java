package org.reminstant.service;

import com.mongodb.DuplicateKeyException;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.reminstant.dto.http.ReservationRequestDto;
import org.reminstant.dto.http.RoomDto;
import org.reminstant.exception.RoomNotFoundException;
import org.reminstant.exception.UnavailableReservationException;
import org.reminstant.model.Reservation;
import org.reminstant.model.Room;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
public class RoomService {

  private static final String ROOMS_COLLECTION = "rooms";
  private static final String RESERVATIONS_COLLECTION = "reservations";

  private final MongoTemplate mongoTemplate;

  public RoomService(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }


  public List<String> getRooms() {
    Query query = new Query();
    query.fields().include("roomTitle").exclude("_id");
    List<Room> rooms = mongoTemplate.find(query, Room.class, ROOMS_COLLECTION);

    return rooms.stream().map(Room::getRoomTitle).toList();
  }

  public Room getRoomById(String roomId) throws RoomNotFoundException {
    Query query = new Query(Criteria.where("_id").is(roomId));
    List<Room> rooms = mongoTemplate.find(query, Room.class, ROOMS_COLLECTION);
    Optional<Room> room = rooms.stream().findFirst();

    return room.orElseThrow(() -> new RoomNotFoundException("id", roomId));
  }

  public Room getRoom(String roomTitle) throws RoomNotFoundException {
    Query query = new Query(Criteria.where("roomTitle").is(roomTitle));
    List<Room> rooms = mongoTemplate.find(query, Room.class, ROOMS_COLLECTION);
    Optional<Room> room = rooms.stream().findFirst();

    return room.orElseThrow(() -> new RoomNotFoundException("title", roomTitle));
  }

  public void addRoom(Room room) throws DuplicateKeyException {
    mongoTemplate.save(room, ROOMS_COLLECTION);
  }

  public void configureRoom(Room room) throws RoomNotFoundException {
    Query query = new Query(Criteria.where("roomTitle").is(room.getRoomTitle()));
    UpdateDefinition update = new Update().set("unavailabilityMasks", room.getUnavailabilityMasks());

    UpdateResult result = mongoTemplate.updateFirst(query, update, ROOMS_COLLECTION);
    if (result.getMatchedCount() == 0) {
      throw new RoomNotFoundException("title", room.getRoomTitle());
    }
  }

  public void deleteRoom(String roomTitle) throws RoomNotFoundException {
    Query query = new Query(Criteria.where("roomTitle").is(roomTitle));

    DeleteResult result = mongoTemplate.remove(query, ROOMS_COLLECTION);
    if (result.getDeletedCount() == 0) {
      throw new RoomNotFoundException("title", roomTitle);
    }
  }



  public String reserveRoom(String roomTitle, String dateString, int startHour, int endHour)
      throws DateTimeParseException, RoomNotFoundException, UnavailableReservationException {
    OffsetDateTime date = OffsetDateTime.parse(dateString + "T00:00:00Z");
    int reservationMask = convertHourRangeToMask(startHour, endHour);
    Room room = getRoom(roomTitle);

    Integer roomMask = switch (date.getDayOfWeek()) {
      case MONDAY -> room.getUnavailabilityMasks().getMonday();
      case TUESDAY -> room.getUnavailabilityMasks().getTuesday();
      case WEDNESDAY -> room.getUnavailabilityMasks().getWednesday();
      case THURSDAY -> room.getUnavailabilityMasks().getThursday();
      case FRIDAY -> room.getUnavailabilityMasks().getFriday();
      case SATURDAY -> room.getUnavailabilityMasks().getSaturday();
      case SUNDAY -> room.getUnavailabilityMasks().getSunday();
    };

    if (roomMask != 0 && (roomMask & reservationMask) != 0) {
      throw new UnavailableReservationException("Unavailable time");
    }

    Query query = new Query(Criteria
        .where("roomId").is(room.getId())
        .and("date").is(date));
    query.fields().include("reservationMask").exclude("_id");
    List<Reservation> reservations = mongoTemplate.find(query, Reservation.class, RESERVATIONS_COLLECTION);

    int mask = reservations.stream()
        .mapToInt(Reservation::getReservationMask)
        .reduce(0, (a, b) -> a | b);

    if ((mask & reservationMask) != 0) {
      throw new UnavailableReservationException("Already reserved");
    }

    Reservation reservation = new Reservation(room.getId(), date, reservationMask);
    reservation = mongoTemplate.save(reservation, RESERVATIONS_COLLECTION);

    return reservation.getId();
  }



  public Room getRoomFromDto(RoomDto dto) {
    Room room = new Room(dto.roomTitle().trim());
    room.getUnavailabilityMasks().setMonday(convertHourListToMask(dto.mondayUnavailable()));
    room.getUnavailabilityMasks().setTuesday(convertHourListToMask(dto.tuesdayUnavailable()));
    room.getUnavailabilityMasks().setThursday(convertHourListToMask(dto.thursdayUnavailable()));
    room.getUnavailabilityMasks().setWednesday(convertHourListToMask(dto.wednesdayUnavailable()));
    room.getUnavailabilityMasks().setFriday(convertHourListToMask(dto.fridayUnavailable()));
    room.getUnavailabilityMasks().setSaturday(convertHourListToMask(dto.saturdayUnavailable()));
    room.getUnavailabilityMasks().setSunday(convertHourListToMask(dto.sundayUnavailable()));

    return room;
  }

  public RoomDto convertRoomToDto(Room room) {
    return new RoomDto(
        room.getRoomTitle(),
        convertHourMaskToList(room.getUnavailabilityMasks().getMonday()),
        convertHourMaskToList(room.getUnavailabilityMasks().getTuesday()),
        convertHourMaskToList(room.getUnavailabilityMasks().getWednesday()),
        convertHourMaskToList(room.getUnavailabilityMasks().getThursday()),
        convertHourMaskToList(room.getUnavailabilityMasks().getFriday()),
        convertHourMaskToList(room.getUnavailabilityMasks().getSaturday()),
        convertHourMaskToList(room.getUnavailabilityMasks().getSunday()));
  }



  private Integer convertHourListToMask(List<Integer> hours) {
    if (hours == null) {
      return null;
    }
    int mask = 0;
    for (int hour : hours) {
      mask |= 1 << hour;
    }
    return mask;
  }

  private List<Integer> convertHourMaskToList(Integer mask) {
    if (mask == null) {
      return null; // NOSONAR
    }
    List<Integer> list = new ArrayList<>();
    for (int i = 0; i < 24; ++i) {
      if ((mask & (1 << i)) != 0) {
        list.add(i);
      }
    }
    return list;
  }

  private int convertHourRangeToMask(int startHour, int endHour) {
    int mask = 0;
    for (int hour = startHour; hour <= endHour; ++hour) {
      mask |= 1 << hour;
    }
    return mask;
  }
}
