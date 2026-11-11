package com.shinchoku.shinchokuapp.entity.converter;

import com.shinchoku.shinchokuapp.entity.enums.IssueState;
import jakarta.persistence.*;

/** Preserves the existing lowercase text columns without PostgreSQL-specific enum types. */
@Converter(autoApply = true)
public class IssueStateConverter implements AttributeConverter<IssueState, String> {

  public String convertToDatabaseColumn(IssueState value) {
    return value == null ? null : value.value();
  }

  public IssueState convertToEntityAttribute(String value) {
    return value == null ? null : IssueState.fromValue(value);
  }
}
