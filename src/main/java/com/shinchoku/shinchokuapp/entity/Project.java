package com.shinchoku.shinchokuapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Persistent model for projects. Flyway owns its constraints; JPA only validates the schema. */
@Entity
@Table(name = "projects")
public class Project extends UuidEntity {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "team_id", nullable = false)
  private UUID teamId;

  @Column(name = "name", nullable = false, columnDefinition = "text")
  private String name;

  @Column(name = "description", nullable = false, columnDefinition = "text")
  private String description = "";

  @Column(name = "state", nullable = false, columnDefinition = "text")
  private ProjectState state = ProjectState.PLANNED;

  @Column(name = "lead_id", nullable = true)
  private UUID leadId;

  @Column(name = "planned_start", nullable = true)
  private LocalDate plannedStart;

  @Column(name = "target_date", nullable = true)
  private LocalDate targetDate;

  @Column(name = "planning_notes", nullable = false, columnDefinition = "text")
  private String planningNotes = "";

  @Column(name = "archived_at", nullable = true)
  private Instant archivedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public Project() {}

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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ProjectState getState() {
    return state;
  }

  public void setState(ProjectState state) {
    this.state = state;
  }

  public UUID getLeadId() {
    return leadId;
  }

  public void setLeadId(UUID leadId) {
    this.leadId = leadId;
  }

  public LocalDate getPlannedStart() {
    return plannedStart;
  }

  public void setPlannedStart(LocalDate plannedStart) {
    this.plannedStart = plannedStart;
  }

  public LocalDate getTargetDate() {
    return targetDate;
  }

  public void setTargetDate(LocalDate targetDate) {
    this.targetDate = targetDate;
  }

  public String getPlanningNotes() {
    return planningNotes;
  }

  public void setPlanningNotes(String planningNotes) {
    this.planningNotes = planningNotes;
  }

  public Instant getArchivedAt() {
    return archivedAt;
  }

  public void setArchivedAt(Instant archivedAt) {
    this.archivedAt = archivedAt;
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
}
