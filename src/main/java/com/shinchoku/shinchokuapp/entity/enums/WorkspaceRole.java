package com.shinchoku.shinchokuapp.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WorkspaceRole implements WireValue {
  OWNER("owner"),
  ADMIN("admin"),
  MEMBER("member"),
  VIEWER("viewer"),
  GUEST("guest");

  private final String value;

  WorkspaceRole(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static WorkspaceRole fromValue(String value) {
    for (WorkspaceRole candidate : values()) {
      if (candidate.value.equals(value)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Unsupported WorkspaceRole: " + value);
  }
}
