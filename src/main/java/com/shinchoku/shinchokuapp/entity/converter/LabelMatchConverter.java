package com.shinchoku.shinchokuapp.entity.converter;

import com.shinchoku.shinchokuapp.entity.enums.LabelMatch;
import jakarta.persistence.*;

/** Preserves the existing lowercase text columns without PostgreSQL-specific enum types. */
@Converter(autoApply = true)
public class LabelMatchConverter implements AttributeConverter<LabelMatch, String> {

  public String convertToDatabaseColumn(LabelMatch value) {
    return value == null ? null : value.value();
  }

  public LabelMatch convertToEntityAttribute(String value) {
    return value == null ? null : LabelMatch.fromValue(value);
  }
}
