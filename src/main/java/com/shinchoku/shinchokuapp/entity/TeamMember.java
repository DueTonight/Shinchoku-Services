package com.shinchoku.shinchokuapp.entity;

import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/**
 * Persistent model for team_members. Flyway owns its constraints; JPA only validates the schema.
 */
@Entity
@Table(name = "team_members")
public class TeamMember {

  @EmbeddedId private TeamMemberId id;

  @Column(name = "role", nullable = false, columnDefinition = "text")
  private TeamRole role = TeamRole.MEMBER;

  @Column(name = "removed_at", nullable = true)
  private Instant removedAt;

  public TeamMember() {}

  public TeamMemberId getId() {
    return id;
  }

  public void setId(TeamMemberId id) {
    this.id = id;
  }

  public TeamRole getRole() {
    return role;
  }

  public void setRole(TeamRole role) {
    this.role = role;
  }

  public Instant getRemovedAt() {
    return removedAt;
  }

  public void setRemovedAt(Instant removedAt) {
    this.removedAt = removedAt;
  }
}
