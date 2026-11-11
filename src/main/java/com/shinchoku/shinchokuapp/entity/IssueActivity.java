package com.shinchoku.shinchokuapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Persistent model for issue_activity. Flyway owns its constraints; JPA only validates the schema.
 */
@Entity
@Table(name = "issue_activity")
public class IssueActivity extends UuidEntity {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "issue_id", nullable = false)
  private UUID issueId;

  @Column(name = "actor_id", nullable = true)
  private UUID actorId;

  @Column(name = "kind", nullable = false, columnDefinition = "text")
  private ActivityKind kind;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "changes", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> changes = new LinkedHashMap<>();

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt = Instant.now();

  public IssueActivity() {}

  public UUID getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(UUID workspaceId) {
    this.workspaceId = workspaceId;
  }

  public UUID getIssueId() {
    return issueId;
  }

  public void setIssueId(UUID issueId) {
    this.issueId = issueId;
  }

  public UUID getActorId() {
    return actorId;
  }

  public void setActorId(UUID actorId) {
    this.actorId = actorId;
  }

  public ActivityKind getKind() {
    return kind;
  }

  public void setKind(ActivityKind kind) {
    this.kind = kind;
  }

  public Map<String, Object> getChanges() {
    return changes;
  }

  public void setChanges(Map<String, Object> changes) {
    this.changes = changes;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
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
  @JoinColumn(name = "issue_id", insertable = false, updatable = false)
  private Issue issue;

  @JsonIgnore
  public Issue getIssue() {
    return issue;
  }
}
