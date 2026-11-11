package com.shinchoku.shinchokuapp.dto;

import com.fasterxml.jackson.annotation.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/**
 * Typed PATCH payload. Typed presence checks such as hasAssigneeId() distinguish omission from
 * explicit null.
 */
public class CyclePatchRequest extends PatchRequest {

  private Integer number;

  private LocalDate startsOn;

  private LocalDate endsOn;

  public Integer getNumber() {
    return number;
  }

  @JsonSetter("number")
  public void setNumber(Integer number) {
    this.number = number;
    supplied("number");
  }

  public LocalDate getStartsOn() {
    return startsOn;
  }

  @JsonSetter("startsOn")
  public void setStartsOn(LocalDate startsOn) {
    this.startsOn = startsOn;
    supplied("startsOn");
  }

  public LocalDate getEndsOn() {
    return endsOn;
  }

  @JsonSetter("endsOn")
  public void setEndsOn(LocalDate endsOn) {
    this.endsOn = endsOn;
    supplied("endsOn");
  }

  @JsonIgnore
  public boolean hasNumber() {
    return has("number");
  }

  @JsonIgnore
  public boolean hasStartsOn() {
    return has("startsOn");
  }

  @JsonIgnore
  public boolean hasEndsOn() {
    return has("endsOn");
  }
}
