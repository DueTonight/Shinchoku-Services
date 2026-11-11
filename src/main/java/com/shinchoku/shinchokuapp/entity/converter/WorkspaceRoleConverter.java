package com.shinchoku.shinchokuapp.entity.converter;

import com.shinchoku.shinchokuapp.entity.enums.WorkspaceRole;
import jakarta.persistence.*;

/** Preserves the existing lowercase text columns without PostgreSQL-specific enum types. */
@Converter(autoApply = true)
public class WorkspaceRoleConverter implements AttributeConverter<WorkspaceRole, String> {

  public String convertToDatabaseColumn(WorkspaceRole value) {
    return value == null ? null : value.value();
  }

  public WorkspaceRole convertToEntityAttribute(String value) {
    return value == null ? null : WorkspaceRole.fromValue(value);
  }
}
