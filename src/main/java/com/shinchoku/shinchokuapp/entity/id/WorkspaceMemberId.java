package com.shinchoku.shinchokuapp.entity.id;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.*;

/** Composite identity matches the database's tenant-scoped primary key. */
@Embeddable
public class WorkspaceMemberId implements Serializable {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  public WorkspaceMemberId() {}

  public WorkspaceMemberId(UUID workspaceId, UUID userId) {
    this.workspaceId = workspaceId;
    this.userId = userId;
  }

  public UUID getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(UUID workspaceId) {
    this.workspaceId = workspaceId;
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
    if (!(other instanceof WorkspaceMemberId that)) {
      return false;
    }
    return Objects.equals(workspaceId, that.workspaceId) && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workspaceId, userId);
  }
}
