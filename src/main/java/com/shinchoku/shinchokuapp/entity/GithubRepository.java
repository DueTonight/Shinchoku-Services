package com.shinchoku.shinchokuapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/**
 * Persistent model for github_repositories. Flyway owns its constraints; JPA only validates the
 * schema.
 */
@Entity
@Table(name = "github_repositories")
public class GithubRepository extends UuidEntity {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "team_id", nullable = false)
  private UUID teamId;

  @Column(name = "github_id", nullable = false)
  private Long githubId;

  @Column(name = "full_name", nullable = false, columnDefinition = "text")
  private String fullName;

  @Column(name = "installation_secret_ref", nullable = false, columnDefinition = "text")
  private String installationSecretRef;

  @Column(name = "synced_at", nullable = true)
  private Instant syncedAt;

  public GithubRepository() {}

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

  public Long getGithubId() {
    return githubId;
  }

  public void setGithubId(Long githubId) {
    this.githubId = githubId;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  @JsonIgnore
  public String getInstallationSecretRef() {
    return installationSecretRef;
  }

  public void setInstallationSecretRef(String installationSecretRef) {
    this.installationSecretRef = installationSecretRef;
  }

  public Instant getSyncedAt() {
    return syncedAt;
  }

  public void setSyncedAt(Instant syncedAt) {
    this.syncedAt = syncedAt;
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
  @JoinColumn(name = "team_id", insertable = false, updatable = false)
  private Team team;

  @JsonIgnore
  public Team getTeam() {
    return team;
  }
}
