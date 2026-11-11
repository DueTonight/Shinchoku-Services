package com.shinchoku.shinchokuapp.entity.converter;

import com.shinchoku.shinchokuapp.entity.enums.IssuePriority;
import jakarta.persistence.*;

/** Preserves the existing lowercase text columns without PostgreSQL-specific enum types. */
@Converter(autoApply = true)
public class IssuePriorityConverter implements AttributeConverter<IssuePriority, String> {

  public String convertToDatabaseColumn(IssuePriority value) {
    return value == null ? null : value.value();
  }

  public IssuePriority convertToEntityAttribute(String value) {
    return value == null ? null : IssuePriority.fromValue(value);
  }
}
