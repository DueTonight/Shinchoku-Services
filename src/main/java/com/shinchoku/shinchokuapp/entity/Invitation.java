package com.shinchoku.shinchokuapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Persistent model for invitations. Flyway owns its constraints; JPA only validates the schema. */
@Entity
@Table(name = "invitations")
public class Invitation extends UuidEntity {

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "invited_by", nullable = false)
  private UUID invitedBy;

  @Column(name = "email", nullable = false, columnDefinition = "text")
  private String email;

  @Column(name = "role", nullable = false, columnDefinition = "text")
  private WorkspaceRole role;

  @Column(name = "token_hash", nullable = false, columnDefinition = "text")
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "accepted_at", nullable = true)
  private Instant acceptedAt;

  @Column(name = "revoked_at", nullable = true)
  private Instant revokedAt;

  public Invitation() {}

  public UUID getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(UUID workspaceId) {
    this.workspaceId = workspaceId;
  }

  public UUID getInvitedBy() {
    return invitedBy;
  }

  public void setInvitedBy(UUID invitedBy) {
    this.invitedBy = invitedBy;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public WorkspaceRole getRole() {
    return role;
  }

  public void setRole(WorkspaceRole role) {
    this.role = role;
  }

  @JsonIgnore
  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getAcceptedAt() {
    return acceptedAt;
  }

  public void setAcceptedAt(Instant acceptedAt) {
    this.acceptedAt = acceptedAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(Instant revokedAt) {
    this.revokedAt = revokedAt;
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
