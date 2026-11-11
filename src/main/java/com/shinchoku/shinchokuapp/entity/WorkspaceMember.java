package com.shinchoku.shinchokuapp.entity;

import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/**
 * Persistent model for workspace_members. Flyway owns its constraints; JPA only validates the
 * schema.
 */
@Entity
@Table(name = "workspace_members")
public class WorkspaceMember {

  @EmbeddedId private WorkspaceMemberId id;

  @Column(name = "role", nullable = false, columnDefinition = "text")
  private WorkspaceRole role;

  @Column(name = "joined_at", nullable = false)
  private Instant joinedAt = Instant.now();

  @Column(name = "removed_at", nullable = true)
  private Instant removedAt;

  public WorkspaceMember() {}

  public WorkspaceMemberId getId() {
    return id;
  }

  public void setId(WorkspaceMemberId id) {
    this.id = id;
  }

  public WorkspaceRole getRole() {
    return role;
  }

  public void setRole(WorkspaceRole role) {
    this.role = role;
  }

  public Instant getJoinedAt() {
    return joinedAt;
  }

  public void setJoinedAt(Instant joinedAt) {
    this.joinedAt = joinedAt;
  }

  public Instant getRemovedAt() {
    return removedAt;
  }

  public void setRemovedAt(Instant removedAt) {
    this.removedAt = removedAt;
  }
}
