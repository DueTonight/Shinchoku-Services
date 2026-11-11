package com.shinchoku.shinchokuapp.entity;

import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Persistent model for workspaces. Flyway owns its constraints; JPA only validates the schema. */
@Entity
@Table(name = "workspaces")
public class Workspace extends UuidEntity {

  @Column(name = "name", nullable = false, columnDefinition = "text")
  private String name;

  @Column(name = "slug", nullable = false, columnDefinition = "text")
  private String slug;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Workspace() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
