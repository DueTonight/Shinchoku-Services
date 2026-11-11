package com.shinchoku.shinchokuapp.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TeamRole implements WireValue {
  LEAD("lead"),
  MEMBER("member"),
  VIEWER("viewer");

  private final String value;

  TeamRole(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static TeamRole fromValue(String value) {
    for (TeamRole candidate : values()) {
      if (candidate.value.equals(value)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Unsupported TeamRole: " + value);
  }
}
