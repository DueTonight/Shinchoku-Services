package com.shinchoku.shinchokuapp.entity;

import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/**
 * Persistent model for saved_view_teams. Flyway owns its constraints; JPA only validates the
 * schema.
 */
@Entity
@Table(name = "saved_view_teams")
public class SavedViewTeam {

  @EmbeddedId private SavedViewTeamId id;

  public SavedViewTeam() {}

  public SavedViewTeamId getId() {
    return id;
  }

  public void setId(SavedViewTeamId id) {
    this.id = id;
  }
}
