package com.shinchoku.shinchokuapp.entity.id;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.*;

/** Composite identity matches the database's tenant-scoped primary key. */
@Embeddable
public class GithubIssueLinkId implements Serializable {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "issue_id", nullable = false)
  private UUID issueId;

  public GithubIssueLinkId() {}

  public GithubIssueLinkId(UUID workspaceId, UUID issueId) {
    this.workspaceId = workspaceId;
    this.issueId = issueId;
  }

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

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof GithubIssueLinkId that)) {
      return false;
    }
    return Objects.equals(workspaceId, that.workspaceId) && Objects.equals(issueId, that.issueId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workspaceId, issueId);
  }
}
