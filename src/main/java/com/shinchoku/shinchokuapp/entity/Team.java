package com.shinchoku.shinchokuapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Persistent model for teams. Flyway owns its constraints; JPA only validates the schema. */
@Entity
@Table(name = "teams")
public class Team extends UuidEntity {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "name", nullable = false, columnDefinition = "text")
  private String name;

  @Column(name = "key", nullable = false, columnDefinition = "text")
  private String key;

  @Column(name = "description", nullable = false, columnDefinition = "text")
  private String description = "";

  @Column(name = "next_issue_number", nullable = false)
  private Long nextIssueNumber = 1L;

  @Column(name = "archived_at", nullable = true)
  private Instant archivedAt;

  public Team() {}

  public UUID getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(UUID workspaceId) {
    this.workspaceId = workspaceId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getNextIssueNumber() {
    return nextIssueNumber;
  }

  public void setNextIssueNumber(Long nextIssueNumber) {
    this.nextIssueNumber = nextIssueNumber;
  }

  public Instant getArchivedAt() {
    return archivedAt;
  }

  public void setArchivedAt(Instant archivedAt) {
    this.archivedAt = archivedAt;
  }

  /** Navigation only; the explicit ID fields and database composite keys control ownership. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workspace_id", insertable = false, updatable = false)
  private Workspace workspace;

  @JsonIgnore
  public Workspace getWorkspace() {
    return workspace;
  }
}
