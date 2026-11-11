package com.shinchoku.shinchokuapp.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IssueState implements WireValue {
  UPCOMING("upcoming"),
  TODO("todo"),
  IN_PROGRESS("in_progress"),
  IN_REVIEW("in_review"),
  BLOCKED("blocked"),
  DONE("done"),
  CANCELLED("cancelled");

  private final String value;

  IssueState(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static IssueState fromValue(String value) {
    for (IssueState candidate : values()) {
      if (candidate.value.equals(value)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Unsupported IssueState: " + value);
  }
}
