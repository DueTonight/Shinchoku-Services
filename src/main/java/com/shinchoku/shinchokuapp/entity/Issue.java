package com.shinchoku.shinchokuapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Persistent model for issues. Flyway owns its constraints; JPA only validates the schema. */
@Entity
@Table(name = "issues")
public class Issue extends UuidEntity {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "team_id", nullable = false)
  private UUID teamId;

  @Column(name = "number", nullable = false)
  private Long number;

  @Column(name = "title", nullable = false, columnDefinition = "text")
  private String title;

  @Column(name = "description", nullable = false, columnDefinition = "text")
  private String description = "";

  @Column(name = "state", nullable = false, columnDefinition = "text")
  private IssueState state = IssueState.TODO;

  @Column(name = "priority", nullable = false, columnDefinition = "text")
  private IssuePriority priority = IssuePriority.MEDIUM;

  @Column(name = "assignee_id", nullable = true)
  private UUID assigneeId;

  @Column(name = "creator_id", nullable = false)
  private UUID creatorId;

  @Column(name = "project_id", nullable = true)
  private UUID projectId;

  @Column(name = "cycle_id", nullable = true)
  private UUID cycleId;

  @Column(name = "due_on", nullable = true)
  private LocalDate dueOn;

  @Column(name = "completed_at", nullable = true)
  private Instant completedAt;

  @Column(name = "archived_at", nullable = true)
  private Instant archivedAt;

  /** Hibernate includes this version in UPDATE conditions and increments it on successful edits. */
  @Version
  @Column(name = "version", nullable = false)
  private int version = 1;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public Issue() {}

  public UUID getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(UUID workspaceId) {
    this.workspaceId = workspaceId;
  }

  public UUID getTeamId() {
    return teamId;
  }

  public void setTeamId(UUID teamId) {
    this.teamId = teamId;
  }

  public Long getNumber() {
    return number;
  }

  public void setNumber(Long number) {
    this.number = number;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public IssueState getState() {
    return state;
  }

  public void setState(IssueState state) {
    this.state = state;
  }

  public IssuePriority getPriority() {
    return priority;
  }

  public void setPriority(IssuePriority priority) {
    this.priority = priority;
  }

  public UUID getAssigneeId() {
    return assigneeId;
  }

  public void setAssigneeId(UUID assigneeId) {
    this.assigneeId = assigneeId;
  }

  public UUID getCreatorId() {
    return creatorId;
  }

  public void setCreatorId(UUID creatorId) {
    this.creatorId = creatorId;
  }

  public UUID getProjectId() {
    return projectId;
  }

  public void setProjectId(UUID projectId) {
    this.projectId = projectId;
  }

  public UUID getCycleId() {
    return cycleId;
  }

  public void setCycleId(UUID cycleId) {
    this.cycleId = cycleId;
  }

  public LocalDate getDueOn() {
    return dueOn;
  }

  public void setDueOn(LocalDate dueOn) {
    this.dueOn = dueOn;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }

  public Instant getArchivedAt() {
    return archivedAt;
  }

  public void setArchivedAt(Instant archivedAt) {
    this.archivedAt = archivedAt;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
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
  @JoinColumn(name = "team_id", insertable = false, updatable = false)
  private Team team;

  @JsonIgnore
  public Team getTeam() {
    return team;
  }

  /** Navigation only; the explicit ID fields and database composite keys control ownership. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id", insertable = false, updatable = false)
  private User assignee;

  @JsonIgnore
  public User getAssignee() {
    return assignee;
  }

  /** Navigation only; the explicit ID fields and database composite keys control ownership. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "creator_id", insertable = false, updatable = false)
  private User creator;

  @JsonIgnore
  public User getCreator() {
    return creator;
  }

  /** Navigation only; the explicit ID fields and database composite keys control ownership. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", insertable = false, updatable = false)
  private Project project;

  @JsonIgnore
  public Project getProject() {
    return project;
  }

  /** Navigation only; the explicit ID fields and database composite keys control ownership. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id", insertable = false, updatable = false)
  private Cycle cycle;

  @JsonIgnore
  public Cycle getCycle() {
    return cycle;
  }
}
