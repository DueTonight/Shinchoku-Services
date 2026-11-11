package com.shinchoku.shinchokuapp.entity;

import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/**
 * Persistent model for issue_labels. Flyway owns its constraints; JPA only validates the schema.
 */
@Entity
@Table(name = "issue_labels")
public class IssueLabel {

  @EmbeddedId private IssueLabelId id;

  public IssueLabel() {}

  public IssueLabelId getId() {
    return id;
  }

  public void setId(IssueLabelId id) {
    this.id = id;
  }
}
