package com.shinchoku.shinchokuapp.entity.converter;

import com.shinchoku.shinchokuapp.entity.enums.Theme;
import jakarta.persistence.*;

/** Preserves the existing lowercase text columns without PostgreSQL-specific enum types. */
@Converter(autoApply = true)
public class ThemeConverter implements AttributeConverter<Theme, String> {

  public String convertToDatabaseColumn(Theme value) {
    return value == null ? null : value.value();
  }

  public Theme convertToEntityAttribute(String value) {
    return value == null ? null : Theme.fromValue(value);
  }
}
