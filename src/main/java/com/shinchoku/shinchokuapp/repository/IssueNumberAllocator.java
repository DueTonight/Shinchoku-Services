package com.shinchoku.shinchokuapp.repository;

import com.shinchoku.shinchokuapp.api.ApiException;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Targeted native SQL is retained for PostgreSQL's atomic UPDATE ... RETURNING operation. */
@Repository
public class IssueNumberAllocator {

  private final EntityManager entityManager;

  public IssueNumberAllocator(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public long reserve(UUID workspaceId, UUID teamId) {
    var values =
        entityManager
            .createNativeQuery(
                """
                UPDATE teams
                SET next_issue_number=next_issue_number+1
                WHERE workspace_id=:workspaceId
                AND id=:teamId
                RETURNING next_issue_number-1
                """,
                Long.class)
            .setParameter("workspaceId", workspaceId)
            .setParameter("teamId", teamId)
            .getResultList();
    if (values.isEmpty()) {
      throw ApiException.missing();
    }
    return ((Number) values.getFirst()).longValue();
  }
}
