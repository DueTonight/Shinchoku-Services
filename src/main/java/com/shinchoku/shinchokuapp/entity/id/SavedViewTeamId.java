package com.shinchoku.shinchokuapp.entity.id;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.*;

/** Composite identity matches the database's tenant-scoped primary key. */
@Embeddable
public class SavedViewTeamId implements Serializable {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "view_id", nullable = false)
  private UUID viewId;

  @Column(name = "team_id", nullable = false)
  private UUID teamId;

  public SavedViewTeamId() {}

  public SavedViewTeamId(UUID workspaceId, UUID viewId, UUID teamId) {
    this.workspaceId = workspaceId;
    this.viewId = viewId;
    this.teamId = teamId;
  }

  public UUID getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(UUID workspaceId) {
    this.workspaceId = workspaceId;
  }

  public UUID getViewId() {
    return viewId;
  }

  public void setViewId(UUID viewId) {
    this.viewId = viewId;
  }

  public UUID getTeamId() {
    return teamId;
  }

  public void setTeamId(UUID teamId) {
    this.teamId = teamId;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SavedViewTeamId that)) {
      return false;
    }
    return Objects.equals(workspaceId, that.workspaceId)
        && Objects.equals(viewId, that.viewId)
        && Objects.equals(teamId, that.teamId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workspaceId, viewId, teamId);
  }
}
