package com.shinchoku.shinchokuapp.entity.converter;

import com.shinchoku.shinchokuapp.entity.enums.ActivityKind;
import jakarta.persistence.*;

/** Preserves the existing lowercase text columns without PostgreSQL-specific enum types. */
@Converter(autoApply = true)
public class ActivityKindConverter implements AttributeConverter<ActivityKind, String> {

  public String convertToDatabaseColumn(ActivityKind value) {
    return value == null ? null : value.value();
  }

  public ActivityKind convertToEntityAttribute(String value) {
    return value == null ? null : ActivityKind.fromValue(value);
  }
}
