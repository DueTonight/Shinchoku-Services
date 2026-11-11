package com.shinchoku.shinchokuapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/**
 * Persistent model for triage_requests. Flyway owns its constraints; JPA only validates the schema.
 */
@Entity
@Table(name = "triage_requests")
public class TriageRequest extends UuidEntity {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "source_team_id", nullable = false)
  private UUID sourceTeamId;

  @Column(name = "target_team_id", nullable = false)
  private UUID targetTeamId;

  @Column(name = "requester_id", nullable = false)
  private UUID requesterId;

  @Column(name = "title", nullable = false, columnDefinition = "text")
  private String title;

  @Column(name = "description", nullable = false, columnDefinition = "text")
  private String description = "";

  @Column(name = "state", nullable = false, columnDefinition = "text")
  private TriageState state = TriageState.PENDING;

  @Column(name = "decided_by", nullable = true)
  private UUID decidedBy;

  @Column(name = "decided_at", nullable = true)
  private Instant decidedAt;

  @Column(name = "decision_reason", nullable = true, columnDefinition = "text")
  private String decisionReason;

  @Column(name = "accepted_issue_id", nullable = true)
  private UUID acceptedIssueId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public TriageRequest() {}

  public UUID getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(UUID workspaceId) {
    this.workspaceId = workspaceId;
  }

  public UUID getSourceTeamId() {
    return sourceTeamId;
  }

  public void setSourceTeamId(UUID sourceTeamId) {
    this.sourceTeamId = sourceTeamId;
  }

  public UUID getTargetTeamId() {
    return targetTeamId;
  }

  public void setTargetTeamId(UUID targetTeamId) {
    this.targetTeamId = targetTeamId;
  }

  public UUID getRequesterId() {
    return requesterId;
  }

  public void setRequesterId(UUID requesterId) {
    this.requesterId = requesterId;
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

  public TriageState getState() {
    return state;
  }

  public void setState(TriageState state) {
    this.state = state;
  }

  public UUID getDecidedBy() {
    return decidedBy;
  }

  public void setDecidedBy(UUID decidedBy) {
    this.decidedBy = decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public void setDecidedAt(Instant decidedAt) {
    this.decidedAt = decidedAt;
  }

  public String getDecisionReason() {
    return decisionReason;
  }

  public void setDecisionReason(String decisionReason) {
    this.decisionReason = decisionReason;
  }

  public UUID getAcceptedIssueId() {
    return acceptedIssueId;
  }

  public void setAcceptedIssueId(UUID acceptedIssueId) {
    this.acceptedIssueId = acceptedIssueId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
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
