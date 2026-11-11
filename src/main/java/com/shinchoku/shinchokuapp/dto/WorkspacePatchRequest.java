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
public class WorkspacePatchRequest extends PatchRequest {

  private String name;

  private String slug;

  public String getName() {
    return name;
  }

  @JsonSetter("name")
  public void setName(String name) {
    this.name = name;
    supplied("name");
  }

  public String getSlug() {
    return slug;
  }

  @JsonSetter("slug")
  public void setSlug(String slug) {
    this.slug = slug;
    supplied("slug");
  }

  @JsonIgnore
  public boolean hasName() {
    return has("name");
  }

  @JsonIgnore
  public boolean hasSlug() {
    return has("slug");
  }
}
