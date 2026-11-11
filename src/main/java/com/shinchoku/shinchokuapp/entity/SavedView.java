package com.shinchoku.shinchokuapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Persistent model for saved_views. Flyway owns its constraints; JPA only validates the schema. */
@Entity
@Table(name = "saved_views")
public class SavedView extends UuidEntity {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "name", nullable = false, columnDefinition = "text")
  private String name;

  @Column(name = "visibility", nullable = false, columnDefinition = "text")
  private ViewVisibility visibility = ViewVisibility.PERSONAL;

  @Column(name = "label_match", nullable = false, columnDefinition = "text")
  private LabelMatch labelMatch = LabelMatch.ANY;

  @Column(name = "search", nullable = false, columnDefinition = "text")
  private String search = "";

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "states", nullable = false, columnDefinition = "text[]")
  private String[] states = new String[0];

  @Column(name = "assignee_id", nullable = true)
  private UUID assigneeId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public SavedView() {}

  public UUID getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(UUID workspaceId) {
    this.workspaceId = workspaceId;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ViewVisibility getVisibility() {
    return visibility;
  }

  public void setVisibility(ViewVisibility visibility) {
    this.visibility = visibility;
  }

  public LabelMatch getLabelMatch() {
    return labelMatch;
  }

  public void setLabelMatch(LabelMatch labelMatch) {
    this.labelMatch = labelMatch;
  }

  public String getSearch() {
    return search;
  }

  public void setSearch(String search) {
    this.search = search;
  }

  public String[] getStates() {
    return states;
  }

  public void setStates(String[] states) {
    this.states = states;
  }

  public UUID getAssigneeId() {
    return assigneeId;
  }

  public void setAssigneeId(UUID assigneeId) {
    this.assigneeId = assigneeId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  /** Navigation only; the explicit ID fields and database composite keys control ownership. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workspace_id", insertable = false, updatable = false)
  private Workspace workspace;

  @JsonIgnore
  public Workspace getWorkspace() {
    return workspace;
  }

  /** Navigation only; the explicit ID fields and database composite keys control ownership. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", insertable = false, updatable = false)
  private User owner;

  @JsonIgnore
  public User getOwner() {
    return owner;
  }

  /** Navigation only; the explicit ID fields and database composite keys control ownership. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id", insertable = false, updatable = false)
  private User assignee;

  @JsonIgnore
  public User getAssignee() {
    return assignee;
  }
}
