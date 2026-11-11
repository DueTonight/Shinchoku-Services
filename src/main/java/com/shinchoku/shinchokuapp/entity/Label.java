package com.shinchoku.shinchokuapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Persistent model for labels. Flyway owns its constraints; JPA only validates the schema. */
@Entity
@Table(name = "labels")
public class Label extends UuidEntity {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "name", nullable = false, columnDefinition = "text")
  private String name;

  public Label() {}

  public UUID getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(UUID workspaceId) {
    this.workspaceId = workspaceId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /** Navigation only; the explicit ID fields and database composite keys control ownership. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workspace_id", insertable = false, updatable = false)
  private Workspace workspace;

  @JsonIgnore
  public Workspace getWorkspace() {
    return workspace;
  }
}
