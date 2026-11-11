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
public class MilestonePatchRequest extends PatchRequest {

  private String name;

  private LocalDate targetDate;

  public String getName() {
    return name;
  }

  @JsonSetter("name")
  public void setName(String name) {
    this.name = name;
    supplied("name");
  }

  public LocalDate getTargetDate() {
    return targetDate;
  }

  @JsonSetter("targetDate")
  public void setTargetDate(LocalDate targetDate) {
    this.targetDate = targetDate;
    supplied("targetDate");
  }

  @JsonIgnore
  public boolean hasName() {
    return has("name");
  }

  @JsonIgnore
  public boolean hasTargetDate() {
    return has("targetDate");
  }
}
