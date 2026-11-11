package com.shinchoku.shinchokuapp.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TriageState implements WireValue {
  PENDING("pending"),
  ACCEPTED("accepted"),
  DECLINED("declined");

  private final String value;

  TriageState(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static TriageState fromValue(String value) {
    for (TriageState candidate : values()) {
      if (candidate.value.equals(value)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Unsupported TriageState: " + value);
  }
}
