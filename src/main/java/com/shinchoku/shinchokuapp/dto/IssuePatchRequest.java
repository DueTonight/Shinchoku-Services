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
public class IssuePatchRequest extends PatchRequest {

  private String title;

  private String description;

  private IssueState state;

  private IssuePriority priority;

  private UUID assigneeId;

  private UUID projectId;

  private UUID cycleId;

  private LocalDate dueOn;

  private List<UUID> labelIds;

  private Integer expectedVersion;

  private Boolean archived;

  public String getTitle() {
    return title;
  }

  @JsonSetter("title")
  public void setTitle(String title) {
    this.title = title;
    supplied("title");
  }

  public String getDescription() {
    return description;
  }

  @JsonSetter("description")
  public void setDescription(String description) {
    this.description = description;
    supplied("description");
  }

  public IssueState getState() {
    return state;
  }

  @JsonSetter("state")
  public void setState(IssueState state) {
    this.state = state;
    supplied("state");
  }

  public IssuePriority getPriority() {
    return priority;
  }

  @JsonSetter("priority")
  public void setPriority(IssuePriority priority) {
    this.priority = priority;
    supplied("priority");
  }

  public UUID getAssigneeId() {
    return assigneeId;
  }

  @JsonSetter("assigneeId")
  public void setAssigneeId(UUID assigneeId) {
    this.assigneeId = assigneeId;
    supplied("assigneeId");
  }

  public UUID getProjectId() {
    return projectId;
  }

  @JsonSetter("projectId")
  public void setProjectId(UUID projectId) {
    this.projectId = projectId;
    supplied("projectId");
  }

  public UUID getCycleId() {
    return cycleId;
  }

  @JsonSetter("cycleId")
  public void setCycleId(UUID cycleId) {
    this.cycleId = cycleId;
    supplied("cycleId");
  }

  public LocalDate getDueOn() {
    return dueOn;
  }

  @JsonSetter("dueOn")
  public void setDueOn(LocalDate dueOn) {
    this.dueOn = dueOn;
    supplied("dueOn");
  }

  public List<UUID> getLabelIds() {
    return labelIds;
  }

  @JsonSetter("labelIds")
  public void setLabelIds(List<UUID> labelIds) {
    this.labelIds = labelIds;
    supplied("labelIds");
  }

  public Integer getExpectedVersion() {
    return expectedVersion;
  }

  @JsonSetter("expectedVersion")
  public void setExpectedVersion(Integer expectedVersion) {
    this.expectedVersion = expectedVersion;
    supplied("expectedVersion");
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
  public boolean hasTitle() {
    return has("title");
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
  public boolean hasPriority() {
    return has("priority");
  }

  @JsonIgnore
  public boolean hasAssigneeId() {
    return has("assigneeId");
  }

  @JsonIgnore
  public boolean hasProjectId() {
    return has("projectId");
  }

  @JsonIgnore
  public boolean hasCycleId() {
    return has("cycleId");
  }

  @JsonIgnore
  public boolean hasDueOn() {
    return has("dueOn");
  }

  @JsonIgnore
  public boolean hasLabelIds() {
    return has("labelIds");
  }

  @JsonIgnore
  public boolean hasExpectedVersion() {
    return has("expectedVersion");
  }

  @JsonIgnore
  public boolean hasArchived() {
    return has("archived");
  }
}
