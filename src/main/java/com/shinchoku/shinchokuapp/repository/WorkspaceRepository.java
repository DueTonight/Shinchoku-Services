package com.shinchoku.shinchokuapp.repository;

import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface WorkspaceRepository
    extends JpaRepository<Workspace, UUID>, JpaSpecificationExecutor<Workspace> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select entity from Workspace entity where entity.id=:id")
  Optional<Workspace> findLocked(UUID id);
}
