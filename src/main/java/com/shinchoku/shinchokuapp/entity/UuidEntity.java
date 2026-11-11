package com.shinchoku.shinchokuapp.entity;

import jakarta.persistence.*;
import java.util.UUID;

/** Shared surrogate identity. Business display identifiers are separate from this UUID. */
@MappedSuperclass
public abstract class UuidEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }
}
