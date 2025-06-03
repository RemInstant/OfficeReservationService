package org.reminstant.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Slf4j
@RestController
public class TestController {

  private final StringRedisTemplate redisTemplate;
  private final MongoTemplate mongoTemplate;

  public TestController(StringRedisTemplate redisTemplate, MongoTemplate mongoTemplate) {
    this.redisTemplate = redisTemplate;
    this.mongoTemplate = mongoTemplate;
  }

  @GetMapping("api/test")
  String test() {
    return "ok";
  }


  @PostMapping("api/test-redis")
  String testRedis() {
    redisTemplate.opsForValue().set("test", "test", Duration.ofMinutes(1));

    return redisTemplate.opsForValue().get("test");
  }

  @PostMapping("api/test-mongo")
  String testMongo() {
    record StringWrapper(String str) {}

    mongoTemplate.save(new StringWrapper("testString"), "testCollection");

    StringWrapper wrapper = mongoTemplate
        .findAll(StringWrapper.class, "testCollection")
        .stream()
        .findFirst()
        .orElse(null);

    if (wrapper == null) {
      return "fail";
    }

    Query removeQuery = Query.query(Criteria.where("str").is(wrapper.str));
    mongoTemplate.remove(removeQuery, "testCollection");

    long cnt = mongoTemplate.getCollection("testCollection").countDocuments();
    log.info("document cnt = {}", cnt);

    return wrapper.str;
  }
}
