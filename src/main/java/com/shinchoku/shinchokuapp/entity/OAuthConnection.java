package com.shinchoku.shinchokuapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Persistent model for oauth_connections. Flyway owns its constraints; JPA only validates the
 * schema.
 */
@Entity
@Table(name = "oauth_connections")
public class OAuthConnection extends UuidEntity {

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "provider", nullable = false, columnDefinition = "text")
  private String provider;

  @Column(name = "provider_subject", nullable = false, columnDefinition = "text")
  private String providerSubject;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "scopes", nullable = false, columnDefinition = "text[]")
  private String[] scopes = new String[0];

  @Column(name = "secret_ref", nullable = true, columnDefinition = "text")
  private String secretRef;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public OAuthConnection() {}

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getProviderSubject() {
    return providerSubject;
  }

  public void setProviderSubject(String providerSubject) {
    this.providerSubject = providerSubject;
  }

  public String[] getScopes() {
    return scopes;
  }

  public void setScopes(String[] scopes) {
    this.scopes = scopes;
  }

  @JsonIgnore
  public String getSecretRef() {
    return secretRef;
  }

  public void setSecretRef(String secretRef) {
    this.secretRef = secretRef;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  /** Navigation only; the explicit ID fields and database composite keys control ownership. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private User user;

  @JsonIgnore
  public User getUser() {
    return user;
  }
}
