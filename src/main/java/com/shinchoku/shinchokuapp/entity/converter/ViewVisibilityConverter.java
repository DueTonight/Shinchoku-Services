package com.shinchoku.shinchokuapp.entity.converter;

import com.shinchoku.shinchokuapp.entity.enums.ViewVisibility;
import jakarta.persistence.*;

/** Preserves the existing lowercase text columns without PostgreSQL-specific enum types. */
@Converter(autoApply = true)
public class ViewVisibilityConverter implements AttributeConverter<ViewVisibility, String> {

  public String convertToDatabaseColumn(ViewVisibility value) {
    return value == null ? null : value.value();
  }

  public ViewVisibility convertToEntityAttribute(String value) {
    return value == null ? null : ViewVisibility.fromValue(value);
  }
}
