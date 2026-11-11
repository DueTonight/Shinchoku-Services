package com.shinchoku.shinchokuapp.dto;

import com.fasterxml.jackson.annotation.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/**
 * Typed PATCH payload. Typed presence checks such as hasAssigneeId() distinguish omission from
 * explicit null.
 */
public class CommentPatchRequest extends PatchRequest {

  private String body;

  public String getBody() {
    return body;
  }

  @JsonSetter("body")
  public void setBody(String body) {
    this.body = body;
    supplied("body");
  }

  @JsonIgnore
  public boolean hasBody() {
    return has("body");
  }
}
