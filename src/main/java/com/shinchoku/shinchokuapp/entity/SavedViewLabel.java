package com.shinchoku.shinchokuapp.entity;

import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/**
 * Persistent model for saved_view_labels. Flyway owns its constraints; JPA only validates the
 * schema.
 */
@Entity
@Table(name = "saved_view_labels")
public class SavedViewLabel {

  @EmbeddedId private SavedViewLabelId id;

  public SavedViewLabel() {}

  public SavedViewLabelId getId() {
    return id;
  }

  public void setId(SavedViewLabelId id) {
    this.id = id;
  }
}
