package com.shinchoku.shinchokuapp.entity;

import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Persistent model for workspace_security. Flyway owns its constraints; JPA only validates the
 * schema.
 */
@Entity
@Table(name = "workspace_security")
public class WorkspaceSecurityPolicy {

  @Id
  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "require_mfa", nullable = false)
  private Boolean requireMfa = false;

  @Column(name = "session_timeout_minutes", nullable = false)
  private Integer sessionTimeoutMinutes = 1440;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "allowed_email_domains", nullable = false, columnDefinition = "text[]")
  private String[] allowedEmailDomains = new String[0];

  public WorkspaceSecurityPolicy() {}

  public UUID getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(UUID workspaceId) {
    this.workspaceId = workspaceId;
  }

  public Boolean getRequireMfa() {
    return requireMfa;
  }

  public void setRequireMfa(Boolean requireMfa) {
    this.requireMfa = requireMfa;
  }

  public Integer getSessionTimeoutMinutes() {
    return sessionTimeoutMinutes;
  }

  public void setSessionTimeoutMinutes(Integer sessionTimeoutMinutes) {
    this.sessionTimeoutMinutes = sessionTimeoutMinutes;
  }

  public String[] getAllowedEmailDomains() {
    return allowedEmailDomains;
  }

  public void setAllowedEmailDomains(String[] allowedEmailDomains) {
    this.allowedEmailDomains = allowedEmailDomains;
  }
}
