package com.shinchoku.shinchokuapp.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Theme implements WireValue {
  LIGHT("light"),
  DARK("dark"),
  SYSTEM("system");

  private final String value;

  Theme(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static Theme fromValue(String value) {
    for (Theme candidate : values()) {
      if (candidate.value.equals(value)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Unsupported Theme: " + value);
  }
}
