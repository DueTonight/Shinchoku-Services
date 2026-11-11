package com.shinchoku.shinchokuapp.entity;

import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Persistent model for users. Flyway owns its constraints; JPA only validates the schema. */
@Entity
@Table(name = "users")
public class User extends UuidEntity {

  @Column(name = "email", nullable = false, columnDefinition = "text")
  private String email;

  @Column(name = "display_name", nullable = false, columnDefinition = "text")
  private String displayName;

  @Column(name = "avatar_url", nullable = true, columnDefinition = "text")
  private String avatarUrl;

  @Column(name = "timezone", nullable = false, columnDefinition = "text")
  private String timezone = "UTC";

  @Column(name = "theme", nullable = false, columnDefinition = "text")
  private Theme theme = Theme.SYSTEM;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public User() {}

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public Theme getTheme() {
    return theme;
  }

  public void setTheme(Theme theme) {
    this.theme = theme;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
