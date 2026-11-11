package com.shinchoku.shinchokuapp.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ActivityKind implements WireValue {
  CREATED("created"),
  UPDATED("updated"),
  LABEL_ADDED("label_added"),
  LABEL_REMOVED("label_removed"),
  COMMENTED("commented"),
  SYNCED("synced");

  private final String value;

  ActivityKind(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static ActivityKind fromValue(String value) {
    for (ActivityKind candidate : values()) {
      if (candidate.value.equals(value)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Unsupported ActivityKind: " + value);
  }
}
