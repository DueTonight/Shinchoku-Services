package com.shinchoku.shinchokuapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Persistent model for cycles. Flyway owns its constraints; JPA only validates the schema. */
@Entity
@Table(name = "cycles")
public class Cycle extends UuidEntity {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "team_id", nullable = false)
  private UUID teamId;

  @Column(name = "number", nullable = false)
  private Integer number;

  @Column(name = "starts_on", nullable = false)
  private LocalDate startsOn;

  @Column(name = "ends_on", nullable = false)
  private LocalDate endsOn;

  public Cycle() {}

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

  public Integer getNumber() {
    return number;
  }

  public void setNumber(Integer number) {
    this.number = number;
  }

  public LocalDate getStartsOn() {
    return startsOn;
  }

  public void setStartsOn(LocalDate startsOn) {
    this.startsOn = startsOn;
  }

  public LocalDate getEndsOn() {
    return endsOn;
  }

  public void setEndsOn(LocalDate endsOn) {
    this.endsOn = endsOn;
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
