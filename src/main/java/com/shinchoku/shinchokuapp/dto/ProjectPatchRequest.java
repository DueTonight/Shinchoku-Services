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
public class ProjectPatchRequest extends PatchRequest {

  private String name;

  private String description;

  private ProjectState state;

  private UUID leadId;

  private LocalDate plannedStart;

  private LocalDate targetDate;

  private String planningNotes;

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

  public ProjectState getState() {
    return state;
  }

  @JsonSetter("state")
  public void setState(ProjectState state) {
    this.state = state;
    supplied("state");
  }

  public UUID getLeadId() {
    return leadId;
  }

  @JsonSetter("leadId")
  public void setLeadId(UUID leadId) {
    this.leadId = leadId;
    supplied("leadId");
  }

  public LocalDate getPlannedStart() {
    return plannedStart;
  }

  @JsonSetter("plannedStart")
  public void setPlannedStart(LocalDate plannedStart) {
    this.plannedStart = plannedStart;
    supplied("plannedStart");
  }

  public LocalDate getTargetDate() {
    return targetDate;
  }

  @JsonSetter("targetDate")
  public void setTargetDate(LocalDate targetDate) {
    this.targetDate = targetDate;
    supplied("targetDate");
  }

  public String getPlanningNotes() {
    return planningNotes;
  }

  @JsonSetter("planningNotes")
  public void setPlanningNotes(String planningNotes) {
    this.planningNotes = planningNotes;
    supplied("planningNotes");
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
  public boolean hasState() {
    return has("state");
  }

  @JsonIgnore
  public boolean hasLeadId() {
    return has("leadId");
  }

  @JsonIgnore
  public boolean hasPlannedStart() {
    return has("plannedStart");
  }

  @JsonIgnore
  public boolean hasTargetDate() {
    return has("targetDate");
  }

  @JsonIgnore
  public boolean hasPlanningNotes() {
    return has("planningNotes");
  }

  @JsonIgnore
  public boolean hasArchived() {
    return has("archived");
  }
}
