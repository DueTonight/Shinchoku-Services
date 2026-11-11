package com.shinchoku.shinchokuapp.persistence;

import jakarta.persistence.criteria.*;
import java.util.*;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable Criteria predicates selected by trusted Java field names. Input values are bound
 * separately; these helpers do not construct arbitrary SQL.
 */
public final class JpaPredicates {

  private JpaPredicates() {}

  public static <T> Path<T> path(Path<?> root, String property) {
    Path<?> result = root;
    for (String part : property.split("\\.")) {
      result = result.get(part);
    }
    return (Path<T>) result;
  }

  public static <T> Specification<T> equalTo(String property, Object value) {
    return (root, query, builder) ->
        value == null
            ? builder.isNull(path(root, property))
            : builder.equal(path(root, property), value);
  }

  public static <T> Specification<T> notNull(String property) {
    return (root, query, builder) -> builder.isNotNull(path(root, property));
  }

  public static <T> Specification<T> and(Specification<T>... filters) {
    return (root, query, builder) ->
        builder.and(
            Arrays.stream(filters)
                .filter(Objects::nonNull)
                .map(filter -> filter.toPredicate(root, query, builder))
                .toArray(Predicate[]::new));
  }

  public static <T> Specification<T> any(Specification<T>... filters) {
    return (root, query, builder) ->
        builder.or(
            Arrays.stream(filters)
                .map(filter -> filter.toPredicate(root, query, builder))
                .toArray(Predicate[]::new));
  }

  public static <T> Specification<T> workspace(UUID workspaceId) {
    return equalTo("workspaceId", workspaceId);
  }

  public static <T> Specification<T> active() {
    return equalTo("removedAt", null);
  }
}
