package com.shinchoku.shinchokuapp.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ViewVisibility implements WireValue {
  PERSONAL("personal"),
  WORKSPACE("workspace");

  private final String value;

  ViewVisibility(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static ViewVisibility fromValue(String value) {
    for (ViewVisibility candidate : values()) {
      if (candidate.value.equals(value)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Unsupported ViewVisibility: " + value);
  }
}
