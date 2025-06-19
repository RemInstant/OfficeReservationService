package org.reminstant.service;

import com.mongodb.DuplicateKeyException;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.reminstant.dto.http.common.CommonUnavailableDaysDto;
import org.reminstant.dto.http.common.RoomDto;
import org.reminstant.dto.http.response.ReservationParamsDto;
import org.reminstant.dto.http.response.RoomDayRangeAvailabilityDto;
import org.reminstant.dto.http.response.RoomsDayAvailabilityDto;
import org.reminstant.dto.mongo.DayHourMasksDto;
import org.reminstant.dto.mongo.RoomHourMasksDto;
import org.reminstant.exception.ReservationNotFoundException;
import org.reminstant.exception.RoomNotFoundException;
import org.reminstant.exception.UnavailableReservationException;
import org.reminstant.model.*;
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
  private static final String COMMMON_CONFIG_COLLECTION = "config";
  private static final String RESERVATIONS_COLLECTION = "reservations";

  private final MongoTemplate mongoTemplate;
  private final AppUserService userService;

  private final DateTimeFormatter isoDateFormatter;

  public RoomService(MongoTemplate mongoTemplate, AppUserService userService) {
    this.mongoTemplate = mongoTemplate;
    this.userService = userService;
    this.isoDateFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd");
  }


  public List<Room> getRooms() {
    return mongoTemplate.find(new Query(), Room.class, ROOMS_COLLECTION);
  }

  public List<String> getRoomTitles() {
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
    Room room = getRoom(roomTitle);

    Query reservationQuery = new Query(Criteria.where("roomId").is(room.getId()));
    mongoTemplate.remove(reservationQuery, Reservation.class, RESERVATIONS_COLLECTION);

    Query roomQuery = new Query(Criteria.where("roomTitle").is(roomTitle));
    DeleteResult result = mongoTemplate.remove(roomQuery, ROOMS_COLLECTION);
    if (result.getDeletedCount() == 0) {
      throw new RoomNotFoundException("title", roomTitle);
    }
  }



  public CommonUnavailableDays getCommonUnavailableDays() {
    Query query = new Query(Criteria.where("configName").is(CommonUnavailableDays.CONFIG_NAME));
    var configs = mongoTemplate.find(query, CommonUnavailableDays.class, COMMMON_CONFIG_COLLECTION);
    Optional<CommonUnavailableDays> config = configs.stream().findFirst();
    return config.orElse(new CommonUnavailableDays());
  }

  public void setCommonUnavailableDays(CommonUnavailableDays unavailable) {
    mongoTemplate.save(unavailable, COMMMON_CONFIG_COLLECTION);
  }



  public RoomDayRangeAvailability getRoomAvailabilityPerDay(String roomTitle, String startStringDate,
                                                            int dayCount)
      throws DateTimeParseException, RoomNotFoundException {
    OffsetDateTime now = OffsetDateTime.now();
    if (startStringDate == null) {
      startStringDate = now.format(isoDateFormatter);
    }

    OffsetDateTime startDate = OffsetDateTime.parse(startStringDate + "T00:00:00Z");
    OffsetDateTime endDate = startDate.plus(Duration.ofDays(dayCount));
    Room room = getRoom(roomTitle);
    CommonUnavailableDays commonUnavailability = getCommonUnavailableDays();
    var roomAvailability = new RoomDayRangeAvailability(room.getId(), roomTitle);

    if (startDate.plus(Duration.ofDays(1)).isBefore(now)) {
      String newStringStartDate = now.format(isoDateFormatter);
      startDate = OffsetDateTime.parse(newStringStartDate + "T00:00:00Z");
    }
    if (endDate.isAfter(now.plus(Duration.ofDays(31)))) {
      String newStringEndDate = now.plus(Duration.ofDays(31)).format(isoDateFormatter);
      endDate = OffsetDateTime.parse(newStringEndDate + "T00:00:00Z");
    }
    if (endDate.isBefore(startDate)) {
      return roomAvailability;
    }

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
        log.warn("Null date or masks in aggregation result (roomTitle={}, startDate={}, endDate={})",
            roomTitle, startDate.format(isoDateFormatter), endDate.format(isoDateFormatter));
        continue;
      }
      int reservationMask = dto.getMasks().stream().reduce(0, (a, b) -> a | b);
      reservationMasks.put(dto.getDate(), reservationMask);
    }

    for (OffsetDateTime date = startDate; date.isBefore(endDate); date = date.plus(Duration.ofDays(1))) {
      int passedHoursMask = 0;
      if (date.isBefore(OffsetDateTime.now())) {
        for (int hour = 0; hour <= OffsetDateTime.now().getHour(); ++hour) {
          passedHoursMask |= 1 << hour;
        }
      }
      int roomMask = getRoomMask(room, date);
      int commonMask = getCommonMask(commonUnavailability, date);
      int reservationMask = reservationMasks.getOrDefault(date, 0);
      int availableMask = ~(roomMask | commonMask | reservationMask | passedHoursMask);
      var avail = new RoomDayRangeAvailability.Availability(date, availableMask);
      roomAvailability.getAvailability().add(avail);
    }

    return roomAvailability;
  }

  public RoomsDayAvailability getRoomsAvailabilityByDay(String stringDate) throws DateTimeParseException {
    Objects.requireNonNull(stringDate, "stringDate cannot be null");

    OffsetDateTime date = OffsetDateTime.parse(stringDate + "T00:00:00Z");
    List<Room> rooms = getRooms();
    CommonUnavailableDays commonUnavailability = getCommonUnavailableDays();
    int passedHoursMask = 0;
    var roomAvailability = new RoomsDayAvailability(date);

    if (date.plus(Duration.ofDays(1)).isBefore(OffsetDateTime.now())) {
      return roomAvailability;
    }
    if (date.isBefore(OffsetDateTime.now())) {
      for (int hour = 0; hour <= OffsetDateTime.now().getHour(); ++hour) {
        passedHoursMask |= 1 << hour;
      }
    }
    if (date.isAfter(OffsetDateTime.now().plus(Duration.ofDays(30)))) {
      return roomAvailability;
    }

    Aggregation agg = Aggregation.newAggregation(
        Aggregation.match(Criteria
            .where("date").is(date)),
        Aggregation.group("roomId")
            .push("reservationMask").as("masks"),
        Aggregation.project()
            .and("_id").as("roomId")
            .and("masks").as("masks"));
    AggregationResults<RoomHourMasksDto> aggResult = mongoTemplate
        .aggregate(agg, RESERVATIONS_COLLECTION, RoomHourMasksDto.class);

    Map<String, Integer> reservationMasks = HashMap.newHashMap(rooms.size());
    for (RoomHourMasksDto dto : aggResult.getMappedResults()) {
      if (dto.getRoomId() == null || dto.getMasks() == null) {
        log.warn("Null roomId or masks in aggregation result (date={}", date);
        continue;
      }
      int reservationMask = dto.getMasks().stream().reduce(0, (a, b) -> a | b);
      reservationMasks.put(dto.getRoomId(), reservationMask);
    }

    int commonMask = getCommonMask(commonUnavailability, date);
    for (Room room : rooms) {
      int roomMask = getRoomMask(room, date);
      int reservationMask = reservationMasks.getOrDefault(room.getId(), 0);
      int availableMask = ~(roomMask | commonMask | reservationMask | passedHoursMask);
      var avail = new RoomsDayAvailability.Availability(room.getId(), room.getRoomTitle(), availableMask);
      roomAvailability.getAvailability().add(avail);
    }

    return roomAvailability;
  }

  // TODO: user not found exception?
  public List<String> getActualReservationIds(String username) {
    AppUser user = userService.getUser(username);
    Long userId = user == null ? null : user.getId();
    OffsetDateTime today = OffsetDateTime.parse(
        OffsetDateTime.now().format(isoDateFormatter)  + "T00:00:00Z");

    Query query = new Query(Criteria
        .where("userId").is(userId)
        .and("date").gte(today));
    query.fields().include("_id");
    List<Reservation> reservations = mongoTemplate.find(query, Reservation.class, RESERVATIONS_COLLECTION);

    return reservations.stream().map(Reservation::getId).toList();
  }

  public Reservation getReservationById(String username, String id) throws ReservationNotFoundException {
    AppUser user = userService.getUser(username);
    Long userId = user == null ? null : user.getId();

    Query query = new Query(Criteria
        .where("userId").is(userId)
        .and("_id").is(id));
    List<Reservation> reservations = mongoTemplate.find(query, Reservation.class, RESERVATIONS_COLLECTION);
    Optional<Reservation> reservation = reservations.stream().findFirst();

    if (reservation.isEmpty()) {
      throw new ReservationNotFoundException(id);
    }

    return reservation.get();
  }

  public String reserveRoom(String username, String roomTitle, String dateString,
                            int startHour, int endHour)
      throws DateTimeParseException, RoomNotFoundException, UnavailableReservationException {
    OffsetDateTime date = OffsetDateTime.parse(dateString + "T00:00:00Z");
    Room room = getRoom(roomTitle);
    CommonUnavailableDays commonUnavailability = getCommonUnavailableDays();
    int roomMask = getRoomMask(room, date);
    int commonMask = getCommonMask(commonUnavailability, date);
    int reservationMask = convertHourRangeToMask(startHour, endHour);
    AppUser user = userService.getUser(username);
    Long userId = user == null ? null : user.getId();

    if (date.isAfter(OffsetDateTime.now().plus(Duration.ofDays(30)))) {
      throw new UnavailableReservationException("Exceeded max reservation delay of 30 days");
    }

    if (((roomMask | commonMask) & reservationMask) != 0) {
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

    Reservation reservation = new Reservation(room.getId(), userId, date, reservationMask);
    reservation = mongoTemplate.save(reservation, RESERVATIONS_COLLECTION);

    return reservation.getId();
  }

  public void cancelReservation(String username, String id) throws ReservationNotFoundException {
    AppUser user = userService.getUser(username);
    Long userId = user == null ? null : user.getId();

    Query query = new Query(Criteria
        .where("userId").is(userId)
        .and("_id").is(id));
    DeleteResult result = mongoTemplate.remove(query, Reservation.class, RESERVATIONS_COLLECTION);
    if (result.getDeletedCount() == 0) {
      throw new ReservationNotFoundException(id);
    }
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

  public CommonUnavailableDays getCommonUnavailableDaysFromDto(CommonUnavailableDaysDto dto) {
    CommonUnavailableDays unavailable = new CommonUnavailableDays();
    unavailable.setMonday(convertHourListToMask(dto.mondayUnavailable()));
    unavailable.setTuesday(convertHourListToMask(dto.tuesdayUnavailable()));
    unavailable.setThursday(convertHourListToMask(dto.thursdayUnavailable()));
    unavailable.setWednesday(convertHourListToMask(dto.wednesdayUnavailable()));
    unavailable.setFriday(convertHourListToMask(dto.fridayUnavailable()));
    unavailable.setSaturday(convertHourListToMask(dto.saturdayUnavailable()));
    unavailable.setSunday(convertHourListToMask(dto.sundayUnavailable()));

    return unavailable;
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

  public CommonUnavailableDaysDto convertCommonUnavailableDaysToDto(CommonUnavailableDays unavailable) {
    return new CommonUnavailableDaysDto(
        convertHourMaskToList(unavailable.getMonday()),
        convertHourMaskToList(unavailable.getTuesday()),
        convertHourMaskToList(unavailable.getWednesday()),
        convertHourMaskToList(unavailable.getThursday()),
        convertHourMaskToList(unavailable.getFriday()),
        convertHourMaskToList(unavailable.getSaturday()),
        convertHourMaskToList(unavailable.getSunday()));
  }

  public ReservationParamsDto convertReservationToDto(Reservation reservation) {
    Room room = getRoomById(reservation.getRoomId());
    List<Integer> hours = convertHourMaskToList(reservation.getReservationMask());

    String roomTitle = room.getRoomTitle();
    String date = reservation.getDate().format(isoDateFormatter);
    int startHour = hours.getFirst();
    int endHour = hours.getLast();

    return new ReservationParamsDto(roomTitle, date, startHour, endHour);
  }

  public RoomDayRangeAvailabilityDto convertAvailabilityToDto(RoomDayRangeAvailability roomAvailability) {
    String roomTitle = roomAvailability.getRoomTitle();
    var dto = new RoomDayRangeAvailabilityDto(roomTitle);

    for (RoomDayRangeAvailability.Availability availability : roomAvailability.getAvailability()) {
      String date = availability.getDate().format(isoDateFormatter);
      List<Integer> hours = convertHourMaskToList(availability.getMask());
      if (hours == null || hours.isEmpty()) {
        continue;
      }
      dto.dateAvailability().add(new RoomDayRangeAvailabilityDto.DateAvailability(date, hours));
    }

    return dto;
  }

  public RoomsDayAvailabilityDto convertAvailabilityToDto(RoomsDayAvailability roomAvailability) {
    String stringDate = roomAvailability.getDate().format(isoDateFormatter);
    var dto = new RoomsDayAvailabilityDto(stringDate);

    for (RoomsDayAvailability.Availability availability : roomAvailability.getAvailability()) {
      String roomTitle = availability.getRoomTitle();
      List<Integer> hours = convertHourMaskToList(availability.getMask());
      if (hours == null || hours.isEmpty()) {
        continue;
      }
      dto.roomAvailability().add(new RoomsDayAvailabilityDto.RoomAvailability(roomTitle, hours));
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

  private int getCommonMask(CommonUnavailableDays unavailable, OffsetDateTime date) {
    Integer commonMask = switch (date.getDayOfWeek()) {
      case MONDAY -> unavailable.getMonday();
      case TUESDAY -> unavailable.getTuesday();
      case WEDNESDAY -> unavailable.getWednesday();
      case THURSDAY -> unavailable.getThursday();
      case FRIDAY -> unavailable.getFriday();
      case SATURDAY -> unavailable.getSaturday();
      case SUNDAY -> unavailable.getSunday();
    };

    return commonMask != null ? commonMask : 0;
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
