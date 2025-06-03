package org.reminstant.dto.http;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("DeconstructionCanBeUsed")
public record RoomConfigurationDto(
    @NotBlank @Length(min = 1, max = 32)
    String roomTitle,
    List<@Min(0) @Max(23) Integer> mondayUnavailable,
    List<@Min(0) @Max(23) Integer> tuesdayUnavailable,
    List<@Min(0) @Max(23) Integer> wednesdayUnavailable,
    List<@Min(0) @Max(23) Integer> thursdayUnavailable,
    List<@Min(0) @Max(23) Integer> fridayUnavailable,
    List<@Min(0) @Max(23) Integer> sundayUnavailable,
    List<@Min(0) @Max(23) Integer> saturdayUnavailable) {

  @Override
  public boolean equals(Object o) {
    return o instanceof RoomConfigurationDto that && // NOSONAR
        roomTitle.equals(that.roomTitle) &&
        mondayUnavailable.equals(that.mondayUnavailable) &&
        tuesdayUnavailable.equals(that.tuesdayUnavailable) &&
        wednesdayUnavailable.equals(that.wednesdayUnavailable) &&
        thursdayUnavailable.equals(that.thursdayUnavailable) &&
        fridayUnavailable.equals(that.fridayUnavailable) &&
        sundayUnavailable.equals(that.sundayUnavailable) &&
        saturdayUnavailable.equals(that.saturdayUnavailable);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(roomTitle);
    result = 31 * result + Objects.hashCode(mondayUnavailable);
    result = 31 * result + Objects.hashCode(tuesdayUnavailable);
    result = 31 * result + Objects.hashCode(wednesdayUnavailable);
    result = 31 * result + Objects.hashCode(thursdayUnavailable);
    result = 31 * result + Objects.hashCode(fridayUnavailable);
    result = 31 * result + Objects.hashCode(sundayUnavailable);
    result = 31 * result + Objects.hashCode(saturdayUnavailable);
    return result;
  }

  @Override
  public String toString() {
    return "RoomConfigurationDto{" +
        "roomTitle='" + roomTitle + '\'' +
        ", mondayUnavailable=" + mondayUnavailable +
        ", tuesdayUnavailable=" + tuesdayUnavailable +
        ", wednesdayUnavailable=" + wednesdayUnavailable +
        ", thursdayUnavailable=" + thursdayUnavailable +
        ", fridayUnavailable=" + fridayUnavailable +
        ", sundayUnavailable=" + sundayUnavailable +
        ", saturdayUnavailable=" + saturdayUnavailable +
        '}';
  }
}
