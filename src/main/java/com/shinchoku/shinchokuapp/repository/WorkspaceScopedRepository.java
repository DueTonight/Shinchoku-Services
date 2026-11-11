package com.shinchoku.shinchokuapp.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.NoRepositoryBean;

/** Every UUID resource lookup includes its workspace instead of trusting a globally unique ID. */
@NoRepositoryBean
public interface WorkspaceScopedRepository<T>
    extends JpaRepository<T, UUID>, JpaSpecificationExecutor<T> {

  Optional<T> findByWorkspaceIdAndId(UUID workspaceId, UUID id);

  boolean existsByWorkspaceIdAndId(UUID workspaceId, UUID id);

  List<T> findAllByWorkspaceId(UUID workspaceId);
}
