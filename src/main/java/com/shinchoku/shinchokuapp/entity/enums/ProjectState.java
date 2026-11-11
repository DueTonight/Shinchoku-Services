package com.shinchoku.shinchokuapp.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProjectState implements WireValue {
  PLANNED("planned"),
  ACTIVE("active"),
  PAUSED("paused"),
  COMPLETED("completed"),
  CANCELLED("cancelled");

  private final String value;

  ProjectState(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static ProjectState fromValue(String value) {
    for (ProjectState candidate : values()) {
      if (candidate.value.equals(value)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Unsupported ProjectState: " + value);
  }
}
