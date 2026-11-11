package com.shinchoku.shinchokuapp.repository;

import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface TriageRequestRepository extends WorkspaceScopedRepository<TriageRequest> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select entity from TriageRequest entity where entity.workspaceId=:workspaceId and"
          + " entity.id=:id")
  Optional<TriageRequest> findLocked(UUID workspaceId, UUID id);
}
