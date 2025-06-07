package org.reminstant.service;

import com.mongodb.DuplicateKeyException;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.reminstant.dto.http.common.RoomDto;
import org.reminstant.dto.http.response.RoomDayRangeAvailabilityDto;
import org.reminstant.dto.mongo.DayHourMasksDto;
import org.reminstant.exception.RoomNotFoundException;
import org.reminstant.exception.UnavailableReservationException;
import org.reminstant.model.Reservation;
import org.reminstant.model.Room;
import org.reminstant.model.RoomDayRangeAvailability;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
public class RoomService {

  private static final String ROOMS_COLLECTION = "rooms";
  private static final String RESERVATIONS_COLLECTION = "reservations";

  private final MongoTemplate mongoTemplate;

  private final DateTimeFormatter isoDateFormatter;

  public RoomService(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
    this.isoDateFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd");
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



  public RoomDayRangeAvailability getRoomAvailabilityPerDay(String roomTitle, String startStringDate,
                                                            int dayCount) {
    if (startStringDate == null) {
      startStringDate = OffsetDateTime.now().format(isoDateFormatter);
    }

    OffsetDateTime startDate = OffsetDateTime.parse(startStringDate + "T00:00:00Z");
    OffsetDateTime endDate = startDate.plus(Duration.ofDays(dayCount));
    Room room = getRoom(roomTitle);
    var roomAvailability = new RoomDayRangeAvailability(room.getId(), roomTitle);

    Aggregation agg = Aggregation.newAggregation(
        Aggregation.match(Criteria
            .where("roomId").is(room.getId())
            .and("date").gte(startDate).lt(endDate)),
        Aggregation.group("date")
            .push("reservationMask").as("masks"),
        Aggregation.project()
            .and("_id").as("date")
            .and("masks").as("masks"));
    AggregationResults<DayHourMasksDto> aggResult = mongoTemplate
        .aggregate(agg, RESERVATIONS_COLLECTION, DayHourMasksDto.class);

    Map<OffsetDateTime, Integer> reservationMasks = HashMap.newHashMap(dayCount);
    for (DayHourMasksDto dto : aggResult.getMappedResults()) {
      if (dto.getDate() == null || dto.getMasks() == null) {
        log.warn("Null date or masks in aggregation result (roomTitle={}, startDate={}, endDate={}",
            roomTitle, startDate.format(isoDateFormatter), endDate.format(isoDateFormatter));
        continue;
      }
      int reservationMask = dto.getMasks().stream().reduce(0, (a, b) -> a | b);
      reservationMasks.put(dto.getDate(), reservationMask);
    }

    for (OffsetDateTime date = startDate; date.isBefore(endDate); date = date.plus(Duration.ofDays(1))) {
      int roomMask = getRoomMask(room, date);
      int reservationMask = reservationMasks.getOrDefault(date, 0);
      int availableMask = ~(roomMask | reservationMask);
      var avail = new RoomDayRangeAvailability.Availability(date, availableMask);
      roomAvailability.getAvailability().add(avail);
    }

    return roomAvailability;
  }

  public String reserveRoom(String roomTitle, String dateString, int startHour, int endHour)
      throws DateTimeParseException, RoomNotFoundException, UnavailableReservationException {
    OffsetDateTime date = OffsetDateTime.parse(dateString + "T00:00:00Z");
    int reservationMask = convertHourRangeToMask(startHour, endHour);
    Room room = getRoom(roomTitle);
    int roomMask = getRoomMask(room, date);

    if ((roomMask & reservationMask) != 0) {
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

  public RoomDayRangeAvailabilityDto convertAvailabilityToDto(RoomDayRangeAvailability roomAvailability) {
    String roomTitle = roomAvailability.getRoomTitle();
    var dto = new RoomDayRangeAvailabilityDto(roomTitle);

    for (RoomDayRangeAvailability.Availability availability : roomAvailability.getAvailability()) {
      String date = availability.getDate().format(isoDateFormatter);
      List<Integer> hours = convertHourMaskToList(availability.getMask());
      dto.availability().add(new RoomDayRangeAvailabilityDto.Availability(date, hours));
    }

    return dto;
  }



  private int getRoomMask(Room room, OffsetDateTime date) {
    Integer roomMask = switch (date.getDayOfWeek()) {
      case MONDAY -> room.getUnavailabilityMasks().getMonday();
      case TUESDAY -> room.getUnavailabilityMasks().getTuesday();
      case WEDNESDAY -> room.getUnavailabilityMasks().getWednesday();
      case THURSDAY -> room.getUnavailabilityMasks().getThursday();
      case FRIDAY -> room.getUnavailabilityMasks().getFriday();
      case SATURDAY -> room.getUnavailabilityMasks().getSaturday();
      case SUNDAY -> room.getUnavailabilityMasks().getSunday();
    };

    return roomMask != null ? roomMask : 0;
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
