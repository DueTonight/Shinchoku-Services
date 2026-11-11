package com.shinchoku.shinchokuapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Persistent model for comments. Flyway owns its constraints; JPA only validates the schema. */
@Entity
@Table(name = "comments")
public class IssueComment extends UuidEntity {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "issue_id", nullable = false)
  private UUID issueId;

  @Column(name = "author_id", nullable = false)
  private UUID authorId;

  @Column(name = "body", nullable = false, columnDefinition = "text")
  private String body;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @Column(name = "deleted_at", nullable = true)
  private Instant deletedAt;

  public IssueComment() {}

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

  public UUID getAuthorId() {
    return authorId;
  }

  public void setAuthorId(UUID authorId) {
    this.authorId = authorId;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
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

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(Instant deletedAt) {
    this.deletedAt = deletedAt;
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

  /** Navigation only; the explicit ID fields and database composite keys control ownership. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id", insertable = false, updatable = false)
  private User author;

  @JsonIgnore
  public User getAuthor() {
    return author;
  }
}
