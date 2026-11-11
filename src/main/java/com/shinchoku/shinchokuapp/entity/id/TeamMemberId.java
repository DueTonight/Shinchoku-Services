package com.shinchoku.shinchokuapp.entity.id;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.*;

/** Composite identity matches the database's tenant-scoped primary key. */
@Embeddable
public class TeamMemberId implements Serializable {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "team_id", nullable = false)
  private UUID teamId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  public TeamMemberId() {}

  public TeamMemberId(UUID workspaceId, UUID teamId, UUID userId) {
    this.workspaceId = workspaceId;
    this.teamId = teamId;
    this.userId = userId;
  }

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

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof TeamMemberId that)) {
      return false;
    }
    return Objects.equals(workspaceId, that.workspaceId)
        && Objects.equals(teamId, that.teamId)
        && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workspaceId, teamId, userId);
  }
}
