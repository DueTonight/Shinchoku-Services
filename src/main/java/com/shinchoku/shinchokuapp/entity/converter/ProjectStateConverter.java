package com.shinchoku.shinchokuapp.entity.converter;

import com.shinchoku.shinchokuapp.entity.enums.ProjectState;
import jakarta.persistence.*;

/** Preserves the existing lowercase text columns without PostgreSQL-specific enum types. */
@Converter(autoApply = true)
public class ProjectStateConverter implements AttributeConverter<ProjectState, String> {

  public String convertToDatabaseColumn(ProjectState value) {
    return value == null ? null : value.value();
  }

  public ProjectState convertToEntityAttribute(String value) {
    return value == null ? null : ProjectState.fromValue(value);
  }
}
