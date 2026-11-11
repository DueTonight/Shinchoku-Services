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
public class SavedViewPatchRequest extends PatchRequest {

  private String name;

  private ViewVisibility visibility;

  private LabelMatch labelMatch;

  private List<UUID> labelIds;

  private List<UUID> teamIds;

  private List<IssueState> states;

  private UUID assigneeId;

  private String search;

  public String getName() {
    return name;
  }

  @JsonSetter("name")
  public void setName(String name) {
    this.name = name;
    supplied("name");
  }

  public ViewVisibility getVisibility() {
    return visibility;
  }

  @JsonSetter("visibility")
  public void setVisibility(ViewVisibility visibility) {
    this.visibility = visibility;
    supplied("visibility");
  }

  public LabelMatch getLabelMatch() {
    return labelMatch;
  }

  @JsonSetter("labelMatch")
  public void setLabelMatch(LabelMatch labelMatch) {
    this.labelMatch = labelMatch;
    supplied("labelMatch");
  }

  public List<UUID> getLabelIds() {
    return labelIds;
  }

  @JsonSetter("labelIds")
  public void setLabelIds(List<UUID> labelIds) {
    this.labelIds = labelIds;
    supplied("labelIds");
  }

  public List<UUID> getTeamIds() {
    return teamIds;
  }

  @JsonSetter("teamIds")
  public void setTeamIds(List<UUID> teamIds) {
    this.teamIds = teamIds;
    supplied("teamIds");
  }

  public List<IssueState> getStates() {
    return states;
  }

  @JsonSetter("states")
  public void setStates(List<IssueState> states) {
    this.states = states;
    supplied("states");
  }

  public UUID getAssigneeId() {
    return assigneeId;
  }

  @JsonSetter("assigneeId")
  public void setAssigneeId(UUID assigneeId) {
    this.assigneeId = assigneeId;
    supplied("assigneeId");
  }

  public String getSearch() {
    return search;
  }

  @JsonSetter("search")
  public void setSearch(String search) {
    this.search = search;
    supplied("search");
  }

  @JsonIgnore
  public boolean hasName() {
    return has("name");
  }

  @JsonIgnore
  public boolean hasVisibility() {
    return has("visibility");
  }

  @JsonIgnore
  public boolean hasLabelMatch() {
    return has("labelMatch");
  }

  @JsonIgnore
  public boolean hasLabelIds() {
    return has("labelIds");
  }

  @JsonIgnore
  public boolean hasTeamIds() {
    return has("teamIds");
  }

  @JsonIgnore
  public boolean hasStates() {
    return has("states");
  }

  @JsonIgnore
  public boolean hasAssigneeId() {
    return has("assigneeId");
  }

  @JsonIgnore
  public boolean hasSearch() {
    return has("search");
  }
}
