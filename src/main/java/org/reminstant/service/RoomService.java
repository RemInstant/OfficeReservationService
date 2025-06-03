package org.reminstant.service;

import com.mongodb.DuplicateKeyException;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.reminstant.dto.http.RoomConfigurationDto;
import org.reminstant.model.Room;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RoomService {

  private static final String ROOMS_COLLECTIONS = "rooms";

  private final MongoTemplate mongoTemplate;

  public RoomService(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }


  public List<String> getRooms() {
    Query query = new Query();
    query.fields().include("roomTitle").exclude("_id");
    List<Room> rooms = mongoTemplate.find(query, Room.class, ROOMS_COLLECTIONS);

    return rooms.stream().map(Room::getRoomTitle).toList();
  }

  public Optional<Room> getRoom(String roomTitle) {
    Query query = new Query(Criteria.where("roomTitle").is(roomTitle));
    List<Room> rooms = mongoTemplate.find(query, Room.class, ROOMS_COLLECTIONS);

    return rooms.stream().findFirst();
  }

  public void addRoom(Room room) throws DuplicateKeyException {
    mongoTemplate.save(room, ROOMS_COLLECTIONS);
  }

  public boolean configureRoom(Room room) {
    Query query = new Query(Criteria.where("roomTitle").is(room.getRoomTitle()));
    UpdateDefinition update = new Update().set("availabilityMask", room.getAvailabilityMask());

    UpdateResult result = mongoTemplate.updateFirst(query, update, ROOMS_COLLECTIONS);
    return result.getMatchedCount() > 0;
  }

  public boolean deleteRoom(String roomTitle) {
    Query query = new Query(Criteria.where("roomTitle").is(roomTitle));

    DeleteResult result = mongoTemplate.remove(query, ROOMS_COLLECTIONS);
    return result.getDeletedCount() > 0;
  }
}
