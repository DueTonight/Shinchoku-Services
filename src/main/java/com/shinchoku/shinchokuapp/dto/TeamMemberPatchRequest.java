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
public class TeamMemberPatchRequest extends PatchRequest {

  private TeamRole role;

  public TeamRole getRole() {
    return role;
  }

  @JsonSetter("role")
  public void setRole(TeamRole role) {
    this.role = role;
    supplied("role");
  }

  @JsonIgnore
  public boolean hasRole() {
    return has("role");
  }
}
