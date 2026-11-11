package com.shinchoku.shinchokuapp.entity.id;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.*;

/** Composite identity matches the database's tenant-scoped primary key. */
@Embeddable
public class IssueLabelId implements Serializable {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "issue_id", nullable = false)
  private UUID issueId;

  @Column(name = "label_id", nullable = false)
  private UUID labelId;

  public IssueLabelId() {}

  public IssueLabelId(UUID workspaceId, UUID issueId, UUID labelId) {
    this.workspaceId = workspaceId;
    this.issueId = issueId;
    this.labelId = labelId;
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

  public UUID getLabelId() {
    return labelId;
  }

  public void setLabelId(UUID labelId) {
    this.labelId = labelId;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof IssueLabelId that)) {
      return false;
    }
    return Objects.equals(workspaceId, that.workspaceId)
        && Objects.equals(issueId, that.issueId)
        && Objects.equals(labelId, that.labelId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workspaceId, issueId, labelId);
  }
}
