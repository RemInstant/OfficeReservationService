package org.reminstant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public class TTT {

  public static void main(String[] args) {

    LocalDate date = LocalDate.parse("2004-12-10");
    System.out.println(date);

//    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
//
//    List<Integer> list = List.of(0, 1, 33);
//    DTO dto = new DTO("rrr", new int[]{33});
//
//    var violations = validator.validate(dto);
//    violations.forEach(v -> System.out.println(v.getMessage()));
  }

  public record DTO(
      @NotBlank String roomTitle,
      int @Max(23) [] mondayUnavailable) {
  }
}
