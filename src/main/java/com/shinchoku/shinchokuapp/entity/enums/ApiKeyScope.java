package com.shinchoku.shinchokuapp.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ApiKeyScope implements WireValue {
  READ("read"),
  WRITE("write"),
  ADMIN("admin");

  private final String value;

  ApiKeyScope(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static ApiKeyScope fromValue(String value) {
    for (ApiKeyScope candidate : values()) {
      if (candidate.value.equals(value)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Unsupported ApiKeyScope: " + value);
  }
}
