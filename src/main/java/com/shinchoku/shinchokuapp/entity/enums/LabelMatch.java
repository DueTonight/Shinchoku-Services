package com.shinchoku.shinchokuapp.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LabelMatch implements WireValue {
  ANY("any"),
  ALL("all");

  private final String value;

  LabelMatch(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static LabelMatch fromValue(String value) {
    for (LabelMatch candidate : values()) {
      if (candidate.value.equals(value)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Unsupported LabelMatch: " + value);
  }
}
