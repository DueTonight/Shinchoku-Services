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
public class TeamPatchRequest extends PatchRequest {

  private String name;

  private String description;

  private Boolean archived;

  public String getName() {
    return name;
  }

  @JsonSetter("name")
  public void setName(String name) {
    this.name = name;
    supplied("name");
  }

  public String getDescription() {
    return description;
  }

  @JsonSetter("description")
  public void setDescription(String description) {
    this.description = description;
    supplied("description");
  }

  public Boolean getArchived() {
    return archived;
  }

  @JsonSetter("archived")
  public void setArchived(Boolean archived) {
    this.archived = archived;
    supplied("archived");
  }

  @JsonIgnore
  public boolean hasName() {
    return has("name");
  }

  @JsonIgnore
  public boolean hasDescription() {
    return has("description");
  }

  @JsonIgnore
  public boolean hasArchived() {
    return has("archived");
  }
}
