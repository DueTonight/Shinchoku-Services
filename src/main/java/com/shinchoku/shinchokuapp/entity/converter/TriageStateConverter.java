package com.shinchoku.shinchokuapp.entity.converter;

import com.shinchoku.shinchokuapp.entity.enums.TriageState;
import jakarta.persistence.*;

/** Preserves the existing lowercase text columns without PostgreSQL-specific enum types. */
@Converter(autoApply = true)
public class TriageStateConverter implements AttributeConverter<TriageState, String> {

  public String convertToDatabaseColumn(TriageState value) {
    return value == null ? null : value.value();
  }

  public TriageState convertToEntityAttribute(String value) {
    return value == null ? null : TriageState.fromValue(value);
  }
}
