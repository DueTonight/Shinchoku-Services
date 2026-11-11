package com.shinchoku.shinchokuapp.support;

import com.shinchoku.shinchokuapp.api.ApiException;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.SqlArrayValue;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

/**
 * Executes parameterized PostgreSQL queries and converts database values into API-friendly values.
 *
 * <p>Services choose the SQL and enforce permissions. This class handles JDBC plumbing; it does not
 * decide which workspace a caller is allowed to read or modify.
 */
@Repository
public class TestDatabase {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper;

  public TestDatabase(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  /**
   * Builds named bindings, for example parameters("workspaceId", workspaceId, "issueId", issueId).
   */
  public static Map<String, Object> parameters(Object... nameValuePairs) {
    Map<String, Object> sqlParameters = new LinkedHashMap<>();
    for (int pairIndex = 0; pairIndex < nameValuePairs.length; pairIndex += 2) {
      String parameterName = nameValuePairs[pairIndex].toString();
      sqlParameters.put(parameterName, nameValuePairs[pairIndex + 1]);
    }
    return sqlParameters;
  }

  public static UUID toUuid(Object identifier) {
    return identifier == null ? null : UUID.fromString(identifier.toString());
  }

  /** API fields use camelCase, while PostgreSQL columns use snake_case. */
  public static String toSnakeCase(String fieldName) {
    return fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
  }

  private static String toCamelCase(String columnName) {
    StringBuilder fieldName = new StringBuilder();
    boolean capitalizeNextCharacter = false;
    for (char character : columnName.toCharArray()) {
      if (character == '_') {
        capitalizeNextCharacter = true;
      } else {
        fieldName.append(capitalizeNextCharacter ? Character.toUpperCase(character) : character);
        capitalizeNextCharacter = false;
      }
    }
    return fieldName.toString();
  }

  /** Named values are bound by JDBC rather than concatenated into SQL text. */
  public List<Map<String, Object>> findAll(String sqlQuery, Map<String, ?> sqlParameters) {
    return jdbcTemplate.query(
        sqlQuery,
        sqlParameters,
        (resultSet, rowNumber) -> {
          Map<String, Object> record = new LinkedHashMap<>();
          for (int columnIndex = 1;
              columnIndex <= resultSet.getMetaData().getColumnCount();
              columnIndex++) {
            Object columnValue = resultSet.getObject(columnIndex);
            // PostgreSQL arrays/JSONB and JDBC dates need conversion before JSON serialization.
            if (columnValue instanceof java.sql.Array sqlArray) {
              columnValue = Arrays.asList((Object[]) sqlArray.getArray());
              sqlArray.free();
            } else if (columnValue instanceof Timestamp timestamp) {
              columnValue = timestamp.toInstant().toString();
            } else if (columnValue instanceof java.sql.Date date) {
              columnValue = date.toLocalDate().toString();
            } else if (columnValue instanceof UUID) {
              columnValue = columnValue.toString();
            } else if (columnValue != null
                && "jsonb".equals(resultSet.getMetaData().getColumnTypeName(columnIndex))) {
              columnValue = objectMapper.readValue(columnValue.toString(), Map.class);
            }
            String fieldName = toCamelCase(resultSet.getMetaData().getColumnLabel(columnIndex));
            record.put(fieldName, columnValue);
          }
          return record;
        });
  }

  /**
   * Reads the first matching record; missing or inaccessible resources share the same 404 response.
   */
  public Map<String, Object> findOne(String sqlQuery, Map<String, ?> sqlParameters) {
    List<Map<String, Object>> records = findAll(sqlQuery, sqlParameters);
    if (records.isEmpty()) {
      throw ApiException.missing();
    }
    return records.getFirst();
  }

  public long count(String sqlQuery, Map<String, ?> sqlParameters) {
    return jdbcTemplate.queryForObject(sqlQuery, sqlParameters, Long.class);
  }

  public int executeUpdate(String sqlQuery, Map<String, ?> sqlParameters) {
    return jdbcTemplate.update(sqlQuery, sqlParameters);
  }

  /**
   * Table/column names must be chosen by service code; only their values can be JDBC parameters.
   */
  public Map<String, Object> insertReturningRow(
      String tableName, Map<String, Object> columnValues) {
    String columnNames = String.join(",", columnValues.keySet());
    String namedPlaceholders =
        columnValues.keySet().stream()
            .map(columnName -> ":" + columnName)
            .collect(Collectors.joining(","));
    return findOne(
        "INSERT INTO "
            + tableName
            + " ("
            + columnNames
            + ") VALUES ("
            + namedPlaceholders
            + ") RETURNING *",
        columnValues);
  }

  /**
   * Updates only the supplied columns and returns the updated row. The caller supplies the
   * workspace constraint and any optimistic-version check in whereClause.
   */
  public Map<String, Object> updateReturningRow(
      String tableName,
      Map<String, Object> columnValues,
      String whereClause,
      Map<String, Object> whereParameters) {
    Map<String, Object> sqlParameters = new LinkedHashMap<>(whereParameters);
    String assignments =
        columnValues.keySet().stream()
            .map(
                columnName -> {
                  String parameterName = "v_" + columnName;
                  sqlParameters.put(parameterName, columnValues.get(columnName));
                  return columnName + "=:" + parameterName;
                })
            .collect(Collectors.joining(","));
    return findOne(
        "UPDATE " + tableName + " SET " + assignments + " WHERE " + whereClause + " RETURNING *",
        sqlParameters);
  }

  /** Converts validated API values to the JDBC types expected by PostgreSQL. */
  public static Object toSqlValue(String fieldName, Object fieldValue) {
    if (fieldValue == null) {
      return null;
    }
    if (fieldName.endsWith("Id") || fieldName.equals("id")) {
      return toUuid(fieldValue);
    }
    if (Set.of("plannedStart", "targetDate", "startsOn", "endsOn", "dueOn").contains(fieldName)) {
      return LocalDate.parse(fieldValue.toString());
    }
    if (fieldName.endsWith("At")) {
      return Timestamp.from(Instant.parse(fieldValue.toString()));
    }
    if (fieldValue instanceof List<?> values) {
      return new SqlArrayValue("text", values.toArray());
    }
    return fieldValue;
  }

  /**
   * Builds database columns from a validated body. Relationship arrays and control fields, such as
   * expectedVersion, are excluded by the service and handled explicitly in the same transaction.
   */
  public static Map<String, Object> toDatabaseColumns(
      Map<String, Object> requestBody, String... excludedFieldNames) {
    Set<String> excludedFields = Set.of(excludedFieldNames);
    Map<String, Object> columnValues = new LinkedHashMap<>();
    requestBody.forEach(
        (fieldName, fieldValue) -> {
          if (!excludedFields.contains(fieldName)) {
            columnValues.put(toSnakeCase(fieldName), toSqlValue(fieldName, fieldValue));
          }
        });
    return columnValues;
  }
}
