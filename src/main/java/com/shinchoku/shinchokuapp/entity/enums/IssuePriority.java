package com.shinchoku.shinchokuapp.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IssuePriority implements WireValue {
  URGENT("urgent"),
  HIGH("high"),
  MEDIUM("medium"),
  LOW("low"),
  NONE("none");

  private final String value;

  IssuePriority(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static IssuePriority fromValue(String value) {
    for (IssuePriority candidate : values()) {
      if (candidate.value.equals(value)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Unsupported IssuePriority: " + value);
  }
}
