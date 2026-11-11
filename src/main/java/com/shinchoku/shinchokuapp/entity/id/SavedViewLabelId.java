package com.shinchoku.shinchokuapp.entity.id;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.*;

/** Composite identity matches the database's tenant-scoped primary key. */
@Embeddable
public class SavedViewLabelId implements Serializable {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "view_id", nullable = false)
  private UUID viewId;

  @Column(name = "label_id", nullable = false)
  private UUID labelId;

  public SavedViewLabelId() {}

  public SavedViewLabelId(UUID workspaceId, UUID viewId, UUID labelId) {
    this.workspaceId = workspaceId;
    this.viewId = viewId;
    this.labelId = labelId;
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
    if (!(other instanceof SavedViewLabelId that)) {
      return false;
    }
    return Objects.equals(workspaceId, that.workspaceId)
        && Objects.equals(viewId, that.viewId)
        && Objects.equals(labelId, that.labelId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workspaceId, viewId, labelId);
  }
}
