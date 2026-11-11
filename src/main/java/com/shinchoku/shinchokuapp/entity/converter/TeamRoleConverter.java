package com.shinchoku.shinchokuapp.entity.converter;

import com.shinchoku.shinchokuapp.entity.enums.TeamRole;
import jakarta.persistence.*;

/** Preserves the existing lowercase text columns without PostgreSQL-specific enum types. */
@Converter(autoApply = true)
public class TeamRoleConverter implements AttributeConverter<TeamRole, String> {

  public String convertToDatabaseColumn(TeamRole value) {
    return value == null ? null : value.value();
  }

  public TeamRole convertToEntityAttribute(String value) {
    return value == null ? null : TeamRole.fromValue(value);
  }
}
