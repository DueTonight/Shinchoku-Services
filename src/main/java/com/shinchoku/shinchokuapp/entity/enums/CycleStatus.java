package com.shinchoku.shinchokuapp.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CycleStatus implements WireValue {
  UPCOMING("upcoming"),
  ACTIVE("active"),
  COMPLETED("completed");

  private final String value;

  CycleStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static CycleStatus fromValue(String value) {
    for (CycleStatus candidate : values()) {
      if (candidate.value.equals(value)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Unsupported CycleStatus: " + value);
  }
}
