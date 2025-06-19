package org.reminstant.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "config")
public class CommonUnavailableDays {
  public static final String CONFIG_NAME = "commonUnavailableDays";

  @Id
  private String id;

  private String configName;

  private Integer monday;
  private Integer tuesday;
  private Integer wednesday;
  private Integer thursday;
  private Integer friday;
  private Integer saturday;
  private Integer sunday;

  public CommonUnavailableDays() {
    this.id = "config";
    this.configName = CONFIG_NAME;
  }
}
