package com.shinchoku.shinchokuapp.persistence;

import static com.shinchoku.shinchokuapp.persistence.JpaPredicates.*;

import com.shinchoku.shinchokuapp.api.ApiException;
import com.shinchoku.shinchokuapp.dto.*;
import com.shinchoku.shinchokuapp.entity.UuidEntity;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/**
 * Keyset pagination over typed JPA Criteria queries. Services supply the authorization predicates
 * before sorting and fetching limit plus one entities.
 */
@Repository
@SuppressWarnings({"unchecked", "rawtypes"})
public class JpaPagination {

  private final EntityManager entityManager;

  public JpaPagination(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public <E extends UuidEntity, R> PageResponse<R> byId(
      Class<E> entityType, Specification<E> filters, CursorRequest cursor, Function<E, R> mapper) {
    return page(
        entityType,
        filters,
        cursor,
        "",
        "id",
        "id",
        false,
        entity -> new CursorKey(null, entity.getId()),
        mapper);
  }

  public <E extends UuidEntity, R> PageResponse<R> byTime(
      Class<E> entityType,
      Specification<E> filters,
      CursorRequest cursor,
      String timestampProperty,
      Function<E, ?> timestamp,
      boolean descending,
      Function<E, R> mapper) {
    return page(
        entityType,
        filters,
        cursor,
        timestampProperty,
        "id",
        "id",
        descending,
        entity -> new CursorKey(timestamp.apply(entity), entity.getId()),
        mapper);
  }

  public record CursorKey(Object timestamp, UUID id) {}

  public <E, R> PageResponse<R> page(
      Class<E> entityType,
      Specification<E> filters,
      CursorRequest cursor,
      String timestampProperty,
      String identifierProperty,
      String publicIdentifierName,
      boolean descending,
      Function<E, CursorKey> cursorKey,
      Function<E, R> responseMapper) {
    var builder = entityManager.getCriteriaBuilder();
    var query = builder.createQuery(entityType);
    var root = query.from(entityType);
    List<Predicate> predicates = new ArrayList<>();
    if (filters != null) {
      predicates.add(filters.toPredicate(root, query, builder));
    }
    if (cursor.cursor() != null) {
      try {
        String decoded =
            new String(Base64.getUrlDecoder().decode(cursor.cursor()), StandardCharsets.UTF_8);
        String[] parts = decoded.split("\\|", -1);
        if (parts.length != 4
            || !parts[0].equals(timestampProperty)
            || !parts[1].equals(publicIdentifierName)) {
          throw new IllegalArgumentException();
        }
        UUID lastId = UUID.fromString(parts[3]);
        Path<UUID> idPath = path(root, identifierProperty);
        Predicate idAfter =
            descending ? builder.lessThan(idPath, lastId) : builder.greaterThan(idPath, lastId);
        if (timestampProperty.isEmpty()) {
          predicates.add(idAfter);
        } else {
          Comparable lastTime =
              timestampProperty.equals("startsOn")
                  ? LocalDate.parse(parts[2])
                  : Instant.parse(parts[2]);
          Path timePath = path(root, timestampProperty);
          Predicate timeAfter =
              descending
                  ? builder.lessThan(timePath, lastTime)
                  : builder.greaterThan(timePath, lastTime);
          predicates.add(
              builder.or(timeAfter, builder.and(builder.equal(timePath, lastTime), idAfter)));
        }
      } catch (Exception exception) {
        throw new ApiException(400, "INVALID_CURSOR", "Invalid pagination cursor");
      }
    }
    query.where(builder.and(predicates.toArray(Predicate[]::new)));
    List<Order> order = new ArrayList<>();
    if (!timestampProperty.isEmpty()) {
      order.add(
          descending
              ? builder.desc(path(root, timestampProperty))
              : builder.asc(path(root, timestampProperty)));
    }
    order.add(
        descending
            ? builder.desc(path(root, identifierProperty))
            : builder.asc(path(root, identifierProperty)));
    query.orderBy(order);
    List<E> entities =
        entityManager.createQuery(query).setMaxResults(cursor.limit() + 1).getResultList();
    String nextCursor = null;
    if (entities.size() > cursor.limit()) {
      entities = new ArrayList<>(entities.subList(0, cursor.limit()));
      CursorKey last = cursorKey.apply(entities.getLast());
      String value =
          timestampProperty
              + "|"
              + publicIdentifierName
              + "|"
              + (timestampProperty.isEmpty() ? "" : last.timestamp())
              + "|"
              + last.id();
      nextCursor =
          Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
    return new PageResponse<>(entities.stream().map(responseMapper).toList(), nextCursor);
  }
}
