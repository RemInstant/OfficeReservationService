package org.reminstant;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.reminstant.dto.http.RoomConfigurationDto;
import org.reminstant.dto.http.RoomTitleDto;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TTT {

  public static void main(String[] args) {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    List<Integer> list = List.of(0, 1, 33);
    DTO dto = new DTO("rrr", new int[]{33});

    var violations = validator.validate(dto);
    violations.forEach(v -> System.out.println(v.getMessage()));
  }

  public record DTO(
      @NotBlank String roomTitle,
      int @Max(23) [] mondayUnavailable) {
  }
}
