package com.shinchoku.shinchokuapp.api;

import java.net.URI;
import java.time.*;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads OpenAPI validation metadata. Raw JSON is inspected only at the HTTP boundary; controllers
 * and business services receive typed DTOs, not schema maps.
 */
@Component
@SuppressWarnings("unchecked")
public class Contract {

  private final Map<String, Object> openApiDocument;

  private final Map<String, Map<String, Object>> schemasByName;

  public Contract(ObjectMapper objectMapper) throws java.io.IOException {
    try (var contractStream = new ClassPathResource("contracts/openapi.json").getInputStream()) {
      openApiDocument = objectMapper.readValue(contractStream, Map.class);
    }
    Map<String, Object> components = (Map<String, Object>) openApiDocument.get("components");
    schemasByName = (Map<String, Map<String, Object>>) components.get("schemas");
  }

  public Map<String, Object> document() {
    return openApiDocument;
  }

  public void validate(String schemaName, Object value) {
    validateSchema(schemasByName.get(schemaName), value, schemaName);
  }

  /** Recursively checks the schema features used by this API, including nullable anyOf fields. */
  public void validateSchema(Map<String, Object> schema, Object value, String fieldPath) {
    if (schema.containsKey("$ref")) {
      validate(schemaNameFromReference(schema), value);
      return;
    }
    if (schema.containsKey("anyOf")) {
      for (Map<String, Object> alternativeSchema :
          (List<Map<String, Object>>) schema.get("anyOf")) {
        try {
          validateSchema(alternativeSchema, value, fieldPath);
          return;
        } catch (ApiException rejectedAlternative) {
          // An anyOf field is valid if at least one alternative accepts it, for example UUID OR
          // null.
        }
      }
      throw ApiException.invalid(fieldPath + " has an invalid value");
    }
    String expectedType = (String) schema.get("type");
    if ("null".equals(expectedType)) {
      if (value != null) {
        throw ApiException.invalid(fieldPath + " must be null");
      }
      return;
    }
    if (value == null) {
      throw ApiException.invalid(fieldPath + " cannot be null");
    }
    if ("object".equals(expectedType)) {
      validateObject(schema, value, fieldPath);
    } else if ("array".equals(expectedType)) {
      validateArray(schema, value, fieldPath);
    } else if ("string".equals(expectedType)) {
      validateString(schema, value, fieldPath);
    } else if ("integer".equals(expectedType) || "number".equals(expectedType)) {
      validateNumber(schema, value, fieldPath, expectedType);
    } else if ("boolean".equals(expectedType) && !(value instanceof Boolean)) {
      throw ApiException.invalid(fieldPath + " must be boolean");
    }
    if (schema.containsKey("enum") && !((List<?>) schema.get("enum")).contains(value)) {
      throw ApiException.invalid(fieldPath + " has an unsupported value");
    }
  }

  private void validateObject(Map<String, Object> schema, Object value, String fieldPath) {
    if (!(value instanceof Map<?, ?>)) {
      throw ApiException.invalid(fieldPath + " must be an object");
    }
    Map<String, Object> objectFields = (Map<String, Object>) value;
    Map<String, Map<String, Object>> propertySchemas =
        (Map<String, Map<String, Object>>) schema.getOrDefault("properties", Map.of());
    for (String requiredField : (List<String>) schema.getOrDefault("required", List.of())) {
      if (!objectFields.containsKey(requiredField)) {
        throw ApiException.invalid(fieldPath + "." + requiredField + " is required");
      }
    }
    // expectedVersion alone is not a change; IssuePatch requires at least two properties.
    int minimumPropertyCount = ((Number) schema.getOrDefault("minProperties", 0)).intValue();
    if (objectFields.size() < minimumPropertyCount) {
      throw ApiException.invalid("PATCH must contain a change");
    }
    objectFields.forEach(
        (propertyName, propertyValue) -> {
          if (propertySchemas.containsKey(propertyName)) {
            validateSchema(
                propertySchemas.get(propertyName), propertyValue, fieldPath + "." + propertyName);
          } else if (Boolean.FALSE.equals(schema.get("additionalProperties"))) {
            throw ApiException.invalid("Unknown field: " + propertyName);
          }
        });
  }

  private void validateArray(Map<String, Object> schema, Object value, String fieldPath) {
    if (!(value instanceof List<?> values)) {
      throw ApiException.invalid(fieldPath + " must be an array");
    }
    int minimumItemCount = ((Number) schema.getOrDefault("minItems", 0)).intValue();
    if (values.size() < minimumItemCount || values.size() > 500) {
      throw ApiException.invalid(fieldPath + " has an invalid length");
    }
    if (Boolean.TRUE.equals(schema.get("uniqueItems"))
        && new HashSet<>(values).size() != values.size()) {
      throw ApiException.invalid(fieldPath + " contains duplicates");
    }
    Map<String, Object> itemSchema = (Map<String, Object>) schema.get("items");
    for (Object item : values) {
      validateSchema(itemSchema, item, fieldPath + "[]");
    }
  }

  private void validateString(Map<String, Object> schema, Object value, String fieldPath) {
    if (!(value instanceof String text)) {
      throw ApiException.invalid(fieldPath + " must be a string");
    }
    int characterCount = text.codePointCount(0, text.length());
    int minimumLength = ((Number) schema.getOrDefault("minLength", 0)).intValue();
    int maximumLength = ((Number) schema.getOrDefault("maxLength", 100000)).intValue();
    if (characterCount < minimumLength
        || characterCount > maximumLength
        || (schema.containsKey("minLength") && text.isBlank())) {
      throw ApiException.invalid(fieldPath + " has an invalid length");
    }
    if (schema.containsKey("pattern") && !text.matches(schema.get("pattern").toString())) {
      throw ApiException.invalid(fieldPath + " has an invalid format");
    }
    try {
      switch (schema.getOrDefault("format", "").toString()) {
        case "uuid" -> {
          if (!UUID.fromString(text).toString().equalsIgnoreCase(text)) {
            throw new IllegalArgumentException();
          }
        }
        case "date" -> LocalDate.parse(text);
        case "date-time" -> Instant.parse(text);
        case "uri" -> {
          URI parsedUri = URI.create(text);
          if (!parsedUri.isAbsolute()) {
            throw new IllegalArgumentException();
          }
        }
        case "email" -> {
          if (!text.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException();
          }
        }
        default -> {
          /* No additional format constraint was specified. */
        }
      }
    } catch (Exception invalidFormat) {
      throw ApiException.invalid(fieldPath + " has an invalid format");
    }
  }

  private void validateNumber(
      Map<String, Object> schema, Object value, String fieldPath, String expectedType) {
    if (!(value instanceof Number number)) {
      throw ApiException.invalid(fieldPath + " must be numeric");
    }
    double numericValue = number.doubleValue();
    double minimumValue =
        ((Number) schema.getOrDefault("minimum", -Double.MAX_VALUE)).doubleValue();
    double maximumValue = ((Number) schema.getOrDefault("maximum", Double.MAX_VALUE)).doubleValue();
    if (("integer".equals(expectedType) && numericValue != Math.rint(numericValue))
        || numericValue < minimumValue
        || numericValue > maximumValue) {
      throw ApiException.invalid(fieldPath + " is outside the allowed range");
    }
  }

  private String schemaNameFromReference(Map<String, Object> schema) {
    return schema.get("$ref").toString().replace("#/components/schemas/", "");
  }
}
