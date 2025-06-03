package org.reminstant.repository;

import org.springframework.data.mongodb.core.MongoTemplate;

public class MongoRepository {

  private final MongoTemplate mongoTemplate;

  public MongoRepository(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }


}
