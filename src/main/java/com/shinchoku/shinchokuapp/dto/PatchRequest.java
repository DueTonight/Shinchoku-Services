package com.shinchoku.shinchokuapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*;

/**
 * Base for typed PATCH DTOs. Field setters record presence so a typed hasAssigneeId() check can
 * distinguish omission from an explicit null.
 */
public abstract class PatchRequest {

  @JsonIgnore private final Set<String> suppliedFields = new HashSet<>();

  protected void supplied(String name) {
    suppliedFields.add(name);
  }

  @JsonIgnore
  public boolean has(String name) {
    return suppliedFields.contains(name);
  }
}
